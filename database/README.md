# 数据库初始化与升级

## 初始化 MySQL 8 空库

从仓库根目录启动 MySQL 客户端。创建一个新空库并切换到该库后，按以下顺序执行。

```sql
CREATE DATABASE xuexiaotanhua CHARACTER SET utf8mb4;
USE xuexiaotanhua;
SOURCE database/schema.sql;
SOURCE database/seed.sql;
SOURCE RuoYi-Vue-master/sql/talk_session_participant_20261105.sql;
```

在当前 MySQL 会话把 `@bootstrap_admin_hash` 设置为为此部署生成的独立 BCrypt 密码哈希，再运行 `SOURCE database/bootstrap-admin.sql;`。使用受保护的终端，避免将真实密码和哈希写入源码或历史记录。脚本仅在用户表为空且哈希格式有效时创建 `admin`。

```sql
SELECT user_id, user_name FROM sys_user WHERE user_id = 1 AND user_name = 'admin';
SELECT user_id, role_id FROM sys_user_role WHERE user_id = 1 AND role_id = 1;
SHOW COLUMNS FROM talk_session WHERE Field IN ('revision', 'talk_person');
SHOW TABLES LIKE 'talk_session_participant';
```

为应用创建仅有目标库所需权限的 MySQL 账号，然后按[部署说明](../docs/DEPLOYMENT.md)配置后端。

## 升级已有环境

先备份数据库和附件，在测试库核对已有字段、索引和迁移历史。参与人迁移会扩展 `talk_person`、增加 `revision` 和 `talk_session_participant`，并保留历史谈话人文本。历史 SQL 可能包含覆盖式建表或角色、菜单更新，按目标库实际版本逐个核对并选用。空库初始化脚本仅用于新库。
