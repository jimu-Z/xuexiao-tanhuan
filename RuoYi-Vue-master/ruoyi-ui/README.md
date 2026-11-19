# 管理端开发说明

本目录是学生谈心谈话系统的管理端。项目整体结构、配置和首次使用流程分别见 [项目说明](../README.md)、[源码环境说明](../../SOURCE-SETUP.md) 和 [功能清单](../../docs/项目功能清单与首次使用流程.md)。

在本目录安装依赖并启动开发服务：

```bash
npm install
npm run dev
```

默认访问 `http://localhost:6080`；实际端口可由环境变量 `port` 或 `npm_config_port` 覆盖。开发环境的 `/dev-api` 默认代理到 `http://127.0.0.1:6060`，可通过 `VUE_APP_PROXY_TARGET` 指向目标后端。端口和代理配置以 `vue.config.js` 及 `.env.development` 为准。

构建命令为 `npm run build:stage` 和 `npm run build:prod`，输出到 `dist`。构建会生成部署用文件，执行前应遵守当前任务的交付约束；上述命令不代表本次已执行或通过构建。
