-- 可选：已有库从「双重身份」迁移为单一身份（历史 identity_type=3 归为居民老人）
-- 执行前请备份数据库。

USE community_service;

UPDATE sys_user SET identity_type = 1 WHERE role = 3 AND identity_type = 3;
