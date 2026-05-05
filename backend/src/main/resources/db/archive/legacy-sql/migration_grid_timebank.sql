-- 网格化管理 + 时间银行（DDL迁移）
-- 使用方式：SOURCE 本文件（应在 schema.sql 之后执行）
--
-- 若已使用最新 schema.sql（已含 sys_region 与 sys_user.community_id/time_coins/points），
-- 可跳过本文件中「创建 sys_region / ALTER sys_user 增加 community_id…」等已存在的步骤，避免重复列报错。

-- 注意：本迁移尽量采用“可空/带默认值”策略，避免 temp_data.sql 旧插入语句因为字段缺失而失败

USE community_service;

-- ============================================
-- 1) 区域表：sys_region
-- level: 1区 / 2街道 / 3社区
-- ============================================
CREATE TABLE IF NOT EXISTS sys_region (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  name       VARCHAR(100) NOT NULL COMMENT '区域名称',
  level      TINYINT UNSIGNED NOT NULL COMMENT '区域层级：1区 2街道 3社区',
  parent_id  BIGINT UNSIGNED NULL COMMENT '父级区域ID',
  province   VARCHAR(64) NULL COMMENT '省（展示用）',
  city       VARCHAR(64) NULL COMMENT '市（展示用）',
  PRIMARY KEY (id),
  KEY idx_region_level (level),
  KEY idx_region_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='区域/网格表';

-- service_request 增加 community_id（网格化隔离核心字段）
-- 由于不同 MySQL 小版本语法兼容差异，这里用 information_schema + 动态 SQL 保证可重复执行
SET @__col_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'service_request'
    AND column_name = 'community_id'
);
SET @__sql := IF(@__col_exists > 0,
  'SELECT 1',
  'ALTER TABLE service_request ADD COLUMN community_id BIGINT UNSIGNED NULL COMMENT ''所属社区ID（网格化隔离）'''
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

SET @__idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'service_request'
    AND index_name = 'idx_req_community'
);
SET @__sql := IF(@__idx_exists > 0,
  'SELECT 1',
  'CREATE INDEX idx_req_community ON service_request (community_id)'
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

-- ============================================
-- 2) 修改用户表：sys_user 增加网格与时间银行字段
-- community_id: 关联区域表
-- time_coins: 当前可用币
-- points: 累计积分/经验
-- identity_tag: 身份标签（如普通居民、孤寡老人、残疾人）
-- ============================================
-- 可重复执行：按列检查是否存在再添加（避免与最新 schema.sql 重复）
SET @__col_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'community_id'
);
SET @__sql := IF(@__col_exists > 0,
  'SELECT 1',
  'ALTER TABLE sys_user ADD COLUMN community_id BIGINT UNSIGNED NULL COMMENT ''关联区域ID'''
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

SET @__col_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'time_coins'
);
SET @__sql := IF(@__col_exists > 0,
  'SELECT 1',
  'ALTER TABLE sys_user ADD COLUMN time_coins BIGINT NOT NULL DEFAULT 0 COMMENT ''当前可用时间币'''
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

SET @__col_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'points'
);
SET @__sql := IF(@__col_exists > 0,
  'SELECT 1',
  'ALTER TABLE sys_user ADD COLUMN points BIGINT NOT NULL DEFAULT 0 COMMENT ''累计积分/经验值'''
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

SET @__col_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'identity_tag'
);
SET @__sql := IF(@__col_exists > 0,
  'SELECT 1',
  'ALTER TABLE sys_user ADD COLUMN identity_tag VARCHAR(64) NULL COMMENT ''身份标签（如普通居民、孤寡老人、残疾人）'''
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

-- 仅加索引，不强制外键约束，降低历史数据/初始化风险
SET @__idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND index_name = 'idx_sys_user_community'
);
SET @__sql := IF(@__idx_exists > 0,
  'SELECT 1',
  'CREATE INDEX idx_sys_user_community ON sys_user (community_id)'
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

-- ============================================
-- 3) 创建服务订单表：service_order
-- 说明：当前项目的“需求/服务流转”核心数据在 service_request/service_claim，
-- 这里为了支持时间银行闭环，新增 service_order 表并加入 community_id
-- ============================================
CREATE TABLE IF NOT EXISTS service_order (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  request_id          BIGINT UNSIGNED NOT NULL COMMENT '需求ID（service_request）',
  volunteer_user_id  BIGINT UNSIGNED NULL COMMENT '服务执行志愿者（可空）',
  community_id        BIGINT UNSIGNED NULL COMMENT '需求所属社区（关联 sys_region）',
  status              TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单状态（留作扩展）',
  created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  KEY idx_order_request (request_id),
  KEY idx_order_community (community_id),
  CONSTRAINT fk_order_request FOREIGN KEY (request_id) REFERENCES service_request(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_order_volunteer FOREIGN KEY (volunteer_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务订单表（时间银行扩展）';

-- ============================================
-- 4) 创建时间币流水表：time_transaction
-- user_id / amount / type / order_id / create_time
-- type: 1服务所得 2兑换消耗 3系统补贴
-- ============================================
CREATE TABLE IF NOT EXISTS time_transaction (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id      BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  amount       BIGINT NOT NULL COMMENT '变动额度（可正可负）',
  type         TINYINT UNSIGNED NOT NULL COMMENT '1-服务所得 2-兑换消耗 3-系统补贴',
  order_id     BIGINT UNSIGNED NULL COMMENT '关联订单ID（service_order）',
  create_time  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_tt_user_time (user_id, create_time),
  KEY idx_tt_order (order_id),
  CONSTRAINT fk_tt_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_tt_order FOREIGN KEY (order_id) REFERENCES service_order(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='时间币流水表';

