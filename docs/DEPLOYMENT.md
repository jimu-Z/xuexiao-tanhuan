# 安装与部署

## 环境

- JDK 17 或更高版本、Maven、MySQL 8 和 Redis。后端默认使用 6060 端口，Redis 默认使用 6379。
- 管理端使用 Vue CLI 4。使用 OpenSSL 3 的 Node.js 版本时，通过 `NODE_OPTIONS=--openssl-legacy-provider` 兼容构建。

## 启动后端

先按[数据库说明](../database/README.md)建库，使用有权访问该库的应用账号。下例为 PowerShell，从仓库根目录执行。数据库密码和令牌密钥通过部署平台的受保护环境变量配置。

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/xuexiaotanhua?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME = 'xuexiaotanhua'
$env:REDIS_HOST = '127.0.0.1'
Set-Location RuoYi-Vue-master
mvn -B clean package
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

| 变量 | 用途 |
| --- | --- |
| `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | MySQL 连接和应用账号 |
| `REDIS_HOST`、`REDIS_PASSWORD` | Redis 地址及认证配置，密码按服务端配置设置 |
| `TOKEN_SECRET` | 登录令牌签名密钥。使用独立的随机值，多实例使用同一值 |
| `RUOYI_PROFILE` | 附件和上传目录。生产环境设为进程可写的持久化目录 |

启动后访问 `http://127.0.0.1:6060/captchaImage`，确认返回验证码接口响应。

## 启动管理端

在新的终端从仓库根目录执行。开发服务器默认监听 6080，并将 API 请求转发到本机后端。异机联调可设置 `VUE_APP_PROXY_TARGET`。

```powershell
Set-Location RuoYi-Vue-master/ruoyi-ui
$env:NODE_OPTIONS = '--openssl-legacy-provider'
npm ci
npm run dev
```

浏览器打开 `http://localhost:6080`。用初始化的管理员登录。生产构建在同一目录运行 `npm run build:prod`，输出到 `dist/`。学生端源码如需单独构建，在 `Student-UI/` 执行 `npm ci` 和 `npm run build:prod`，并设置相同的 Node 兼容选项。

## Nginx 部署

将管理端 `dist/` 内容部署到示例路径并按环境设置域名、TLS、后端地址及上传限制。`proxy_pass` 的尾部 `/` 会剥离 `/prod-api/` 前缀。

```nginx
server {
    listen 80;
    server_name talk.example.edu;
    root /var/www/xuexiao-tanhuan;
    index index.html;
    client_max_body_size 60m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /prod-api/ {
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_pass http://127.0.0.1:6060/;
    }
}
```

上线前用 `nginx -t` 校验配置，然后验证登录、接口响应、文件上传和文书导出。备份数据库与 `RUOYI_PROFILE` 指向的附件目录，并限制数据库和 Redis 的访问范围。
