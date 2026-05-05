-- 管理员分权与数据范围增强（增量）
USE community_service;

-- 用户社区查询索引（社区管理员数据隔离常用）
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name='sys_user' AND index_name='idx_sys_user_role_community'
);
SET @sql_stmt := IF(@idx_exists > 0, 'SELECT 1', 'CREATE INDEX idx_sys_user_role_community ON sys_user(role, community_id)');
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 需求监控与看板口径按社区过滤常用
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name='service_request' AND index_name='idx_req_community_status'
);
SET @sql_stmt := IF(@idx_exists > 0, 'SELECT 1', 'CREATE INDEX idx_req_community_status ON service_request(community_id, status)');
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 公告读写按社区过滤常用
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name='announcement' AND index_name='idx_announcement_target_community'
);
SET @sql_stmt := IF(@idx_exists > 0, 'SELECT 1', 'CREATE INDEX idx_announcement_target_community ON announcement(target_community_id, status, published_at)');
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

