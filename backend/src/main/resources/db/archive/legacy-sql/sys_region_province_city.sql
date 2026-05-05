-- 为 sys_region 增加省、市展示字段（已有库执行一次即可）
USE community_service;

ALTER TABLE sys_region
  ADD COLUMN province VARCHAR(64) NULL COMMENT '省（展示用）' AFTER parent_id,
  ADD COLUMN city VARCHAR(64) NULL COMMENT '市（展示用）' AFTER province;
