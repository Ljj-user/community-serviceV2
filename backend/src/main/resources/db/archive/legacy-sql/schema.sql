-- 社区公益服务对接管理平台 - 表结构（MySQL 8.0）
-- 只包含 DDL：CREATE TABLE / INDEX 等，不包含任何 INSERT 数据

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS service_evaluation;
DROP TABLE IF EXISTS service_claim;
DROP TABLE IF EXISTS service_request;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_region;

-- ----------------------------
-- 区域/网格表（sys_user.community_id、需求与公告的社区维度均引用本表 id）
-- level：1区 2街道 3社区
-- ----------------------------
CREATE TABLE sys_region (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  name              VARCHAR(100)     NOT NULL COMMENT '区域名称（社区等由管理员录入）',
  level             TINYINT UNSIGNED NOT NULL COMMENT '区域层级：1区 2街道 3社区',
  parent_id         BIGINT UNSIGNED NULL COMMENT '父级区域ID',
  province          VARCHAR(64)      NULL COMMENT '省（展示用，如 浙江省；不参与业务校验）',
  city              VARCHAR(64)      NULL COMMENT '市（展示用，如 杭州市；不参与业务校验）',
  PRIMARY KEY (id),
  KEY idx_region_level (level),
  KEY idx_region_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='区域/网格表';

-- ----------------------------
-- 用户表（含角色/身份）
-- 角色：1超级管理员 2社区管理员 3普通用户
-- 普通用户身份：1居民老人 2志愿者（仅 role=3，互斥）
-- community_id：绑定社区时，与 sys_region.id 对应（应用层关联；也可后续加外键）
-- ----------------------------
CREATE TABLE sys_user (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  username          VARCHAR(50)      NOT NULL COMMENT '用户名（登录名）',
  password_md5      VARCHAR(100)     NOT NULL COMMENT '密码哈希（字段名沿用；兼容历史MD5与当前BCrypt）',
  role              TINYINT UNSIGNED NOT NULL COMMENT '角色：1超级管理员 2社区管理员 3普通用户',
  identity_type     TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '普通用户身份：1居民老人 2志愿者（仅role=3，互斥）',
  community_id      BIGINT UNSIGNED NULL COMMENT '关联区域ID（通常 level=3 的社区，对应 sys_region.id）',
  time_coins        BIGINT           NOT NULL DEFAULT 0 COMMENT '当前可用时间币',
  points            BIGINT           NOT NULL DEFAULT 0 COMMENT '累计积分/经验值',
  identity_tag      VARCHAR(64)      NULL COMMENT '身份标签（如普通居民、孤寡老人、残疾人）',
  real_name         VARCHAR(50)      NULL COMMENT '真实姓名',
  phone             VARCHAR(20)      NULL COMMENT '手机号',
  email             VARCHAR(100)     NULL COMMENT '邮箱',
  avatar_url        VARCHAR(255)     NULL COMMENT '头像URL',
  gender            TINYINT UNSIGNED NULL COMMENT '性别：0未知 1男 2女',
  address           VARCHAR(255)     NULL COMMENT '常住地址/社区地址（用于匹配/服务记录）',
  skill_tags        JSON             NULL COMMENT '志愿者能力/标签（用于智能匹配；如护理、助浴等）',
  status            TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  last_login_at     DATETIME(3)      NULL COMMENT '最近登录时间',
  created_at        DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at        DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_role (role),
  KEY idx_sys_user_identity (identity_type),
  KEY idx_sys_user_phone (phone),
  KEY idx_sys_user_community (community_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表（含角色/身份）';

-- ----------------------------
-- 需求表（居民发布 -> 社区管理员审核 -> 发布/驳回 -> 认领 -> 完成）
-- 状态：0待审核 1已发布 2已认领 3已完成 4已驳回
-- 紧急程度：1低 2中 3高 4紧急
-- ----------------------------
CREATE TABLE service_request (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  requester_user_id   BIGINT UNSIGNED NOT NULL COMMENT '需求发起人（居民）用户ID',
  community_id        BIGINT UNSIGNED NULL COMMENT '所属社区ID（网格化隔离）',
  service_type        VARCHAR(50)      NOT NULL COMMENT '服务类型（如助老、清洁、教育等）',
  description         TEXT             NULL COMMENT '需求描述/补充说明',
  service_address     VARCHAR(255)     NOT NULL COMMENT '服务地址',
  expected_time       DATETIME(3)      NULL COMMENT '期望服务时间',
  urgency_level       TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '紧急程度：1低 2中 3高 4紧急',
  emergency_contact_name     VARCHAR(64)   NULL COMMENT '紧急联系人姓名',
  emergency_contact_phone    VARCHAR(32)   NULL COMMENT '紧急联系人电话',
  emergency_contact_relation VARCHAR(32)   NULL COMMENT '与服务对象关系（子女/邻居等）',
  special_tags        JSON             NULL COMMENT '特殊人群/需求标签（JSON数组，如独居老人、残障等）',
  status              TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已发布 2已认领 3已完成 4已驳回',

  -- 审核信息（社区管理员）
  audit_by_user_id    BIGINT UNSIGNED NULL COMMENT '审核人（社区管理员）用户ID',
  audit_at            DATETIME(3)     NULL COMMENT '审核时间',
  reject_reason       VARCHAR(255)    NULL COMMENT '驳回原因（status=4时必填）',

  -- 流转时间（便于统计/异常监管）
  published_at        DATETIME(3)     NULL COMMENT '发布公开时间（status=1）',
  claimed_at          DATETIME(3)     NULL COMMENT '被认领时间（status=2）',
  completed_at        DATETIME(3)     NULL COMMENT '完成时间（status=3）',

  created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  KEY idx_req_requester (requester_user_id),
  KEY idx_req_community (community_id),
  KEY idx_req_status (status),
  KEY idx_req_type (service_type),
  KEY idx_req_created (created_at),
  CONSTRAINT fk_req_requester FOREIGN KEY (requester_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_req_auditor FOREIGN KEY (audit_by_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公益服务需求表';

-- ----------------------------
-- 服务认领记录表（志愿者认领 -> 完成后提交时长）
-- claim_status：1已认领 2已完成 3已取消
-- ----------------------------
CREATE TABLE service_claim (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  request_id         BIGINT UNSIGNED NOT NULL COMMENT '需求ID',
  volunteer_user_id  BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
  claim_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '认领时间',
  claim_status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1已认领 2已完成 3已取消',

  service_hours     DECIMAL(6,2)    NULL COMMENT '服务时长（小时，完成后提交）',
  hours_submitted_at DATETIME(3)    NULL COMMENT '时长提交时间',
  completion_note   VARCHAR(255)    NULL COMMENT '完成说明/备注（可用于异常记录）',

  created_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  KEY idx_claim_request (request_id),
  KEY idx_claim_volunteer (volunteer_user_id),
  KEY idx_claim_status (claim_status),
  CONSTRAINT fk_claim_request FOREIGN KEY (request_id) REFERENCES service_request(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_claim_volunteer FOREIGN KEY (volunteer_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务认领记录表';

-- ----------------------------
-- 评价表（居民在服务完成后评价）
-- 一个认领记录只能评价一次（uk_eval_claim）
-- ----------------------------
CREATE TABLE service_evaluation (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  claim_id         BIGINT UNSIGNED NOT NULL COMMENT '认领记录ID',
  request_id       BIGINT UNSIGNED NOT NULL COMMENT '需求ID（冗余，便于查询）',
  resident_user_id BIGINT UNSIGNED NOT NULL COMMENT '评价人（居民）用户ID',
  volunteer_user_id BIGINT UNSIGNED NOT NULL COMMENT '被评价人（志愿者）用户ID',
  evaluator_role   TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '评价方角色：1居民 2志愿者',
  rating           TINYINT UNSIGNED NOT NULL COMMENT '星级：1-5',
  content          VARCHAR(500)     NULL COMMENT '评价内容',
  created_at       DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at       DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_eval_claim_role (claim_id, evaluator_role),
  KEY idx_eval_request (request_id),
  KEY idx_eval_volunteer (volunteer_user_id),
  KEY idx_eval_evaluator_role (evaluator_role),
  KEY idx_eval_rating (rating),
  CONSTRAINT fk_eval_claim FOREIGN KEY (claim_id) REFERENCES service_claim(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_eval_request FOREIGN KEY (request_id) REFERENCES service_request(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_eval_resident FOREIGN KEY (resident_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_eval_volunteer FOREIGN KEY (volunteer_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务评价表';

-- ----------------------------
-- 社区公告表
-- ----------------------------
DROP TABLE IF EXISTS announcement;
CREATE TABLE announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(200) NOT NULL COMMENT '公告标题',
  content_html LONGTEXT NOT NULL COMMENT '公告内容（HTML，富文本）',
  content_text TEXT NULL COMMENT '公告纯文本（可选，用于搜索）',
  target_scope TINYINT NOT NULL DEFAULT 0 COMMENT '推送范围：0全体 1社区 2楼栋',
  target_community_id BIGINT NULL COMMENT '推送社区ID（scope=1/2）',
  target_building_id BIGINT NULL COMMENT '推送楼栋ID（scope=2）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0草稿 1已发布',
  is_top TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0否 1是',
  top_at DATETIME NULL COMMENT '置顶时间',
  publisher_user_id BIGINT NOT NULL COMMENT '发布人（社区管理员）用户ID',
  published_at DATETIME NULL COMMENT '发布时间',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_status_published (status, published_at),
  KEY idx_scope (target_scope, target_community_id, target_building_id),
  KEY idx_top (is_top, top_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区公告表';

-- ----------------------------
-- 备份/恢复/导出记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS backup_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  record_type VARCHAR(16) NOT NULL COMMENT '类型 BACKUP/RESTORE/EXPORT',
  module VARCHAR(64) DEFAULT NULL COMMENT '导出模块 service_request/users/audit 等',
  format VARCHAR(16) DEFAULT NULL COMMENT '导出格式 excel/pdf/csv 等',
  filename VARCHAR(255) DEFAULT NULL COMMENT '文件名',
  file_path VARCHAR(512) DEFAULT NULL COMMENT '文件路径（服务器本地）',
  file_size_mb DECIMAL(10,2) DEFAULT NULL COMMENT '文件大小 MB',
  status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态 SUCCESS/FAILED/RUNNING',
  note VARCHAR(255) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  INDEX idx_type (record_type),
  INDEX idx_module (module),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备份/恢复/导出记录表';

-- ----------------------------
-- 全局点赞计数表
-- ----------------------------
CREATE TABLE IF NOT EXISTS support_like (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    total_count  BIGINT      NOT NULL DEFAULT 0 COMMENT '点赞总数',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局点赞计数表';

-- ----------------------------
-- 系统配置表（key-value）
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(64) NOT NULL UNIQUE COMMENT '配置键: basic/notice/alert',
  config_value JSON NOT NULL COMMENT '配置值 JSON',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ----------------------------
-- 待办事项表
-- ----------------------------
CREATE TABLE IF NOT EXISTS todo_group (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id      BIGINT      NOT NULL COMMENT '所属用户ID',
    title        VARCHAR(100) NOT NULL COMMENT '分组标题',
    icon         VARCHAR(16)  NOT NULL COMMENT '分组图标（emoji 或图标编码）',
    bg_color     VARCHAR(32)  NULL COMMENT '背景颜色',
    is_today     TINYINT(1)   DEFAULT 0 COMMENT '是否今日任务分组',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办事项分组表';

-- 防止重复初始化时报 Duplicate key name
SET @__idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'todo_group'
    AND index_name = 'idx_todo_group_user'
);
SET @__sql := IF(@__idx_exists > 0,
  'SELECT 1',
  'CREATE INDEX idx_todo_group_user ON todo_group (user_id)'
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

CREATE TABLE IF NOT EXISTS todo_task (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id      BIGINT      NOT NULL COMMENT '所属用户ID（冗余，便于按用户查询）',
    group_id     BIGINT      NOT NULL COMMENT '所属分组ID',
    title        VARCHAR(255) NOT NULL COMMENT '任务标题',
    is_done      TINYINT(1)   DEFAULT 0 COMMENT '是否完成',
    is_favorite  TINYINT(1)   DEFAULT 0 COMMENT '是否收藏/置顶',
    is_today     TINYINT(1)   DEFAULT 0 COMMENT '是否今日任务',
    sort_order   INT          DEFAULT 0 COMMENT '排序序号',
    done_date    DATETIME     NULL COMMENT '完成时间',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办事项任务表';

-- 防止重复初始化时报 Duplicate key name
SET @__idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'todo_task'
    AND index_name = 'idx_todo_task_group'
);
SET @__sql := IF(@__idx_exists > 0,
  'SELECT 1',
  'CREATE INDEX idx_todo_task_group ON todo_task (group_id)'
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

SET @__idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'todo_task'
    AND index_name = 'idx_todo_task_user'
);
SET @__sql := IF(@__idx_exists > 0,
  'SELECT 1',
  'CREATE INDEX idx_todo_task_user ON todo_task (user_id)'
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

-- ----------------------------
-- 审计日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  username VARCHAR(64) DEFAULT NULL COMMENT '操作人用户名',
  role TINYINT DEFAULT NULL COMMENT '角色 1超级管理员 2社区管理员 3普通用户',
  module VARCHAR(64) NOT NULL COMMENT '模块 USER_MANAGE/SYSTEM_CONFIG/REQUEST_AUDIT等',
  action VARCHAR(128) NOT NULL COMMENT '操作动作',
  request_path VARCHAR(256) DEFAULT NULL COMMENT '请求路径',
  http_method VARCHAR(16) DEFAULT NULL COMMENT 'GET/POST/PUT/DELETE',
  success TINYINT NOT NULL DEFAULT 1 COMMENT '0失败 1成功',
  result_msg VARCHAR(512) DEFAULT NULL COMMENT '结果摘要',
  risk_level VARCHAR(16) DEFAULT 'NORMAL' COMMENT 'NORMAL/WARN/HIGH',
  ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
  elapsed_ms INT DEFAULT NULL COMMENT '耗时毫秒',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  INDEX idx_created_at (created_at),
  INDEX idx_username (username),
  INDEX idx_module (module),
  INDEX idx_success (success),
  INDEX idx_risk_level (risk_level),
  INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

SET FOREIGN_KEY_CHECKS = 1;
