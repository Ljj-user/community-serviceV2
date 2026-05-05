-- 社区公益服务对接管理平台 - PRD v2 数据库结构
-- 用法建议：开发库可 DROP DATABASE 后重新执行本文件。
-- 设计原则：保留现有后端主干表名，校准一人多角色、社区审核、重点关怀、异常预警闭环。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS volunteer_match_snapshot;
DROP TABLE IF EXISTS volunteer_credit_snapshot;
DROP TABLE IF EXISTS volunteer_credit_ledger;
DROP TABLE IF EXISTS service_request_tag;
DROP TABLE IF EXISTS sys_user_skill;
DROP TABLE IF EXISTS sys_notification;
DROP TABLE IF EXISTS abnormal_alert_event;
DROP TABLE IF EXISTS anomaly_alert_event;
DROP TABLE IF EXISTS convenience_info;
DROP TABLE IF EXISTS service_evaluation;
DROP TABLE IF EXISTS service_claim;
DROP TABLE IF EXISTS service_order;
DROP TABLE IF EXISTS service_request;
DROP TABLE IF EXISTS care_subject_profile;
DROP TABLE IF EXISTS volunteer_profile;
DROP TABLE IF EXISTS community_join_application;
DROP TABLE IF EXISTS community_invite_code;
DROP TABLE IF EXISTS community_banner;
DROP TABLE IF EXISTS announcement;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS backup_record;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS verify_code_ticket;
DROP TABLE IF EXISTS user_onboarding_answer;
DROP TABLE IF EXISTS user_onboarding_profile;
DROP TABLE IF EXISTS time_transaction;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_region;

-- 区域/社区表
CREATE TABLE sys_region (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(100) NOT NULL COMMENT '区域名称',
  level TINYINT UNSIGNED NOT NULL COMMENT '区域层级：1区 2街道 3社区',
  parent_id BIGINT UNSIGNED NULL COMMENT '父级区域ID',
  province VARCHAR(64) NULL COMMENT '省',
  city VARCHAR(64) NULL COMMENT '市',
  address VARCHAR(255) NULL COMMENT '社区地址',
  contact_phone VARCHAR(32) NULL COMMENT '社区联系电话',
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_region_level (level),
  KEY idx_region_parent (parent_id),
  KEY idx_region_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='区域/社区表';

-- 用户表：基础账号与权限，不再用 identity_type 表示是否为志愿者
CREATE TABLE sys_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(50) NOT NULL COMMENT '用户名',
  password_md5 VARCHAR(100) NOT NULL COMMENT '密码哈希，字段名兼容历史',
  role TINYINT UNSIGNED NOT NULL COMMENT '角色：1超级管理员 2社区管理员 3普通用户',
  identity_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '兼容旧字段：默认普通居民，不作为志愿者认证依据',
  community_id BIGINT UNSIGNED NULL COMMENT '所属社区ID',
  community_join_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '社区加入状态：0未加入 1待审核 2已加入 3已拒绝',
  real_name VARCHAR(50) NULL COMMENT '真实姓名',
  phone VARCHAR(20) NULL COMMENT '手机号',
  email VARCHAR(100) NULL COMMENT '邮箱',
  avatar_url VARCHAR(255) NULL COMMENT '头像URL',
  gender TINYINT UNSIGNED NULL COMMENT '性别：0未知 1男 2女',
  address VARCHAR(255) NULL COMMENT '常住地址',
  time_coins BIGINT NOT NULL DEFAULT 0 COMMENT '当前可用时间币，兼容旧功能',
  points BIGINT NOT NULL DEFAULT 0 COMMENT '累计积分',
  identity_tag VARCHAR(64) NULL COMMENT '兼容旧字段：身份标签展示，不作为重点关怀主依据',
  skill_tags JSON NULL COMMENT '兼容旧字段：技能标签缓存',
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  last_login_at DATETIME(3) NULL COMMENT '最近登录时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_role (role),
  KEY idx_sys_user_phone (phone),
  KEY idx_sys_user_community (community_id),
  KEY idx_sys_user_join_status (community_join_status),
  CONSTRAINT fk_user_community FOREIGN KEY (community_id) REFERENCES sys_region(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- 社区邀请码
CREATE TABLE community_invite_code (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  community_id BIGINT UNSIGNED NOT NULL COMMENT '社区ID',
  code VARCHAR(32) NOT NULL COMMENT '邀请码',
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
  expires_at DATETIME(3) NULL COMMENT '过期时间',
  max_uses INT UNSIGNED NOT NULL DEFAULT 100 COMMENT '最大可用次数',
  used_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已使用次数',
  created_by BIGINT UNSIGNED NULL COMMENT '创建人用户ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_invite_code (code),
  KEY idx_invite_comm_status (community_id, status),
  KEY idx_invite_expire (expires_at),
  CONSTRAINT fk_invite_community FOREIGN KEY (community_id) REFERENCES sys_region(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区邀请码';

-- 社区加入申请：邀请码不直接绑定，先进入审核流程
CREATE TABLE community_join_application (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '申请用户ID',
  community_id BIGINT UNSIGNED NOT NULL COMMENT '申请加入社区ID',
  invite_code VARCHAR(32) NULL COMMENT '使用的邀请码',
  real_name VARCHAR(50) NULL COMMENT '申请人真实姓名',
  phone VARCHAR(20) NULL COMMENT '联系电话',
  address VARCHAR(255) NULL COMMENT '居住地址',
  status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1通过 2拒绝 3撤回',
  reviewer_user_id BIGINT UNSIGNED NULL COMMENT '审核人ID',
  reviewed_at DATETIME(3) NULL COMMENT '审核时间',
  reject_reason VARCHAR(255) NULL COMMENT '拒绝原因',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_join_user_status (user_id, status),
  KEY idx_join_comm_status (community_id, status),
  KEY idx_join_reviewer (reviewer_user_id),
  CONSTRAINT fk_join_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_join_community FOREIGN KEY (community_id) REFERENCES sys_region(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_join_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区加入申请表';

-- 志愿者认证与服务能力表
CREATE TABLE volunteer_profile (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  community_id BIGINT UNSIGNED NULL COMMENT '所属社区ID',
  cert_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '认证状态：0未提交 1待审核 2已认证 3已拒绝 4已停用',
  real_name VARCHAR(50) NULL COMMENT '认证姓名',
  id_card_no VARCHAR(32) NULL COMMENT '证件号，可脱敏存储',
  skill_tags JSON NULL COMMENT '技能标签',
  service_radius_km DECIMAL(6,2) NULL COMMENT '可服务半径',
  available_time VARCHAR(255) NULL COMMENT '可服务时间说明',
  service_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计服务次数',
  completed_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '完成次数',
  cancelled_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '取消次数',
  avg_rating DECIMAL(4,2) NULL COMMENT '平均评分',
  reviewer_user_id BIGINT UNSIGNED NULL COMMENT '审核人ID',
  certified_at DATETIME(3) NULL COMMENT '认证通过时间',
  reject_reason VARCHAR(255) NULL COMMENT '拒绝原因',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_volunteer_user (user_id),
  KEY idx_volunteer_comm_status (community_id, cert_status),
  KEY idx_volunteer_rating (avg_rating),
  CONSTRAINT fk_volunteer_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_volunteer_community FOREIGN KEY (community_id) REFERENCES sys_region(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_volunteer_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='志愿者认证信息表';

-- 重点关怀对象表
CREATE TABLE care_subject_profile (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
  care_type VARCHAR(64) NOT NULL COMMENT '关怀类型：独居老人/残疾居民/困难家庭/长期病患等',
  care_level TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '关注等级：1低 2中 3高',
  living_status VARCHAR(64) NULL COMMENT '居住情况',
  health_note VARCHAR(500) NULL COMMENT '健康情况备注',
  emergency_contact_name VARCHAR(64) NULL COMMENT '紧急联系人姓名',
  emergency_contact_phone VARCHAR(32) NULL COMMENT '紧急联系人电话',
  emergency_contact_relation VARCHAR(32) NULL COMMENT '紧急联系人关系',
  monitor_enabled TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用异常监测：0否 1是',
  last_visit_at DATETIME(3) NULL COMMENT '最近走访时间',
  created_by BIGINT UNSIGNED NULL COMMENT '创建管理员ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_care_user (user_id),
  KEY idx_care_comm_level (community_id, care_level),
  KEY idx_care_type (care_type),
  KEY idx_care_monitor (monitor_enabled),
  CONSTRAINT fk_care_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_care_community FOREIGN KEY (community_id) REFERENCES sys_region(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_care_creator FOREIGN KEY (created_by) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='重点关怀对象表';

-- 公益服务需求表
CREATE TABLE service_request (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  requester_user_id BIGINT UNSIGNED NOT NULL COMMENT '需求发起人用户ID',
  target_user_id BIGINT UNSIGNED NULL COMMENT '实际服务对象用户ID，可为空表示本人',
  community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
  title VARCHAR(120) NULL COMMENT '需求标题',
  service_type VARCHAR(50) NOT NULL COMMENT '服务类型',
  description TEXT NULL COMMENT '需求描述',
  service_address VARCHAR(255) NOT NULL COMMENT '服务地址',
  contact_phone VARCHAR(32) NULL COMMENT '联系电话',
  expected_time DATETIME(3) NULL COMMENT '期望服务时间',
  urgency_level TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '紧急程度：1低 2中 3高 4紧急',
  priority_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否优先处理：0否 1是',
  emergency_contact_name VARCHAR(64) NULL COMMENT '紧急联系人姓名',
  emergency_contact_phone VARCHAR(32) NULL COMMENT '紧急联系人电话',
  emergency_contact_relation VARCHAR(32) NULL COMMENT '紧急联系人关系',
  special_tags JSON NULL COMMENT '特殊标签',
  status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1待接单 2已接单 3服务中 4待确认 5已完成 6已取消 7审核未通过',
  audit_by_user_id BIGINT UNSIGNED NULL COMMENT '审核人ID',
  audit_at DATETIME(3) NULL COMMENT '审核时间',
  reject_reason VARCHAR(255) NULL COMMENT '驳回原因',
  published_at DATETIME(3) NULL COMMENT '发布时间',
  claimed_at DATETIME(3) NULL COMMENT '接单时间',
  completed_at DATETIME(3) NULL COMMENT '完成时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  KEY idx_req_requester (requester_user_id),
  KEY idx_req_target (target_user_id),
  KEY idx_req_comm_status (community_id, status),
  KEY idx_req_type (service_type),
  KEY idx_req_created (created_at),
  CONSTRAINT fk_req_requester FOREIGN KEY (requester_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_req_target FOREIGN KEY (target_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_req_community FOREIGN KEY (community_id) REFERENCES sys_region(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_req_auditor FOREIGN KEY (audit_by_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公益服务需求表';

-- 服务认领/履约表：现有后端主流程表
CREATE TABLE service_claim (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  request_id BIGINT UNSIGNED NOT NULL COMMENT '需求ID',
  volunteer_user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
  claim_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '接单时间',
  claim_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1已接单 2服务中 3待确认 4已完成 5已取消 6争议中',
  service_hours DECIMAL(6,2) NULL COMMENT '服务时长',
  hours_submitted_at DATETIME(3) NULL COMMENT '时长提交时间',
  completion_note VARCHAR(255) NULL COMMENT '完成说明',
  cancel_reason VARCHAR(255) NULL COMMENT '取消原因',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  KEY idx_claim_request (request_id),
  KEY idx_claim_volunteer (volunteer_user_id),
  KEY idx_claim_status (claim_status),
  CONSTRAINT fk_claim_request FOREIGN KEY (request_id) REFERENCES service_request(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_claim_volunteer FOREIGN KEY (volunteer_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务认领履约表';

-- 服务订单表：作为论文中的订单/工单概念，可与 service_claim 一对一关联
CREATE TABLE service_order (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  request_id BIGINT UNSIGNED NOT NULL COMMENT '需求ID',
  claim_id BIGINT UNSIGNED NULL COMMENT '认领记录ID',
  volunteer_user_id BIGINT UNSIGNED NULL COMMENT '服务执行志愿者',
  community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
  status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单状态',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  KEY idx_order_request (request_id),
  KEY idx_order_claim (claim_id),
  KEY idx_order_community (community_id),
  CONSTRAINT fk_order_request FOREIGN KEY (request_id) REFERENCES service_request(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_order_claim FOREIGN KEY (claim_id) REFERENCES service_claim(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_order_volunteer FOREIGN KEY (volunteer_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_order_community FOREIGN KEY (community_id) REFERENCES sys_region(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务订单表';

-- 服务评价表
CREATE TABLE service_evaluation (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  claim_id BIGINT UNSIGNED NOT NULL COMMENT '认领记录ID',
  request_id BIGINT UNSIGNED NOT NULL COMMENT '需求ID',
  resident_user_id BIGINT UNSIGNED NOT NULL COMMENT '居民用户ID',
  volunteer_user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
  evaluator_role TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '评价方角色：1居民 2志愿者',
  rating TINYINT UNSIGNED NOT NULL COMMENT '星级：1-5',
  content VARCHAR(500) NULL COMMENT '评价内容',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_eval_claim_role (claim_id, evaluator_role),
  KEY idx_eval_request (request_id),
  KEY idx_eval_volunteer (volunteer_user_id),
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

-- 社区公告表
CREATE TABLE announcement (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(200) NOT NULL COMMENT '公告标题',
  content_html LONGTEXT NOT NULL COMMENT '公告内容HTML',
  content_text TEXT NULL COMMENT '公告纯文本',
  target_scope TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '推送范围：0全体 1社区',
  target_community_id BIGINT UNSIGNED NULL COMMENT '推送社区ID',
  target_building_id BIGINT UNSIGNED NULL COMMENT '推送楼栋ID，兼容旧实体字段',
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0草稿 1已发布',
  is_top TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否置顶',
  top_at DATETIME(3) NULL COMMENT '置顶时间',
  publisher_user_id BIGINT UNSIGNED NOT NULL COMMENT '发布人ID',
  published_at DATETIME(3) NULL COMMENT '发布时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  KEY idx_announcement_status_published (status, published_at),
  KEY idx_announcement_scope (target_scope, target_community_id),
  KEY idx_announcement_top (is_top, top_at),
  CONSTRAINT fk_announcement_publisher FOREIGN KEY (publisher_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_announcement_community FOREIGN KEY (target_community_id) REFERENCES sys_region(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区公告表';

-- 便民信息表
CREATE TABLE convenience_info (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  community_id BIGINT UNSIGNED NOT NULL COMMENT '所属社区ID',
  category VARCHAR(64) NOT NULL COMMENT '分类：社区电话/医院药店/维修服务/政务窗口等',
  title VARCHAR(120) NOT NULL COMMENT '标题',
  content VARCHAR(1000) NULL COMMENT '说明内容',
  contact_phone VARCHAR(32) NULL COMMENT '联系电话',
  address VARCHAR(255) NULL COMMENT '地址',
  sort_no INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序',
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  created_by BIGINT UNSIGNED NULL COMMENT '创建人ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  KEY idx_conv_comm_category (community_id, category),
  KEY idx_conv_status_sort (status, sort_no),
  CONSTRAINT fk_conv_community FOREIGN KEY (community_id) REFERENCES sys_region(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_conv_creator FOREIGN KEY (created_by) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='便民信息表';

-- 异常预警事件表：补齐处理闭环
CREATE TABLE anomaly_alert_event (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  alert_code VARCHAR(64) NOT NULL COMMENT '告警编码',
  community_id BIGINT UNSIGNED NULL COMMENT '社区ID',
  request_id BIGINT UNSIGNED NULL COMMENT '关联需求ID',
  target_user_id BIGINT UNSIGNED NULL COMMENT '关联用户ID',
  severity TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '严重程度：1低 2中 3高',
  trigger_rule VARCHAR(255) NOT NULL COMMENT '触发规则摘要',
  suggestion_action VARCHAR(255) NULL COMMENT '建议动作',
  rule_snapshot TEXT NULL COMMENT '规则快照JSON',
  dedup_key VARCHAR(128) NOT NULL COMMENT '去重键',
  status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理 1处理中 2已处理 3忽略',
  handler_user_id BIGINT UNSIGNED NULL COMMENT '处理人ID',
  handled_at DATETIME(3) NULL COMMENT '处理时间',
  handle_result VARCHAR(500) NULL COMMENT '处理结果',
  occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '触发时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_alert_code_time (alert_code, occurred_at),
  KEY idx_alert_comm_status (community_id, status),
  KEY idx_alert_target (target_user_id),
  KEY idx_alert_request (request_id),
  UNIQUE KEY uk_alert_dedup (dedup_key, occurred_at),
  CONSTRAINT fk_alert_community FOREIGN KEY (community_id) REFERENCES sys_region(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_alert_request FOREIGN KEY (request_id) REFERENCES service_request(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_alert_target FOREIGN KEY (target_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_alert_handler FOREIGN KEY (handler_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异常预警事件表';

-- 技能与需求标签
CREATE TABLE sys_user_skill (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  skill_tag VARCHAR(64) NOT NULL COMMENT '技能标签',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_skill (user_id, skill_tag),
  KEY idx_user_skill_user (user_id),
  CONSTRAINT fk_skill_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户技能表';

CREATE TABLE service_request_tag (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  request_id BIGINT UNSIGNED NOT NULL COMMENT '需求ID',
  tag_name VARCHAR(64) NOT NULL COMMENT '需求标签',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_request_tag (request_id, tag_name),
  KEY idx_request_tag_req (request_id),
  CONSTRAINT fk_request_tag_request FOREIGN KEY (request_id) REFERENCES service_request(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='需求标签表';

CREATE TABLE volunteer_match_snapshot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  request_id BIGINT UNSIGNED NOT NULL COMMENT '需求ID',
  volunteer_user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
  total_score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '综合评分',
  skill_score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '技能匹配分',
  area_score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '距离/社区匹配分',
  priority_score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '紧急程度分',
  rating_score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '历史评价分',
  reason_tags_json JSON NULL COMMENT '推荐理由标签',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_match_req_vol (request_id, volunteer_user_id),
  KEY idx_match_total (total_score),
  CONSTRAINT fk_match_request FOREIGN KEY (request_id) REFERENCES service_request(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_match_volunteer FOREIGN KEY (volunteer_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='志愿者匹配解释快照';

-- 消息通知
CREATE TABLE sys_notification (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  recipient_user_id BIGINT UNSIGNED NOT NULL COMMENT '接收人用户ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  summary VARCHAR(500) NULL COMMENT '摘要',
  msg_category TINYINT UNSIGNED NOT NULL COMMENT '1业务待办 2系统公告',
  read_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  ref_type VARCHAR(32) NULL COMMENT '关联业务类型',
  ref_id BIGINT UNSIGNED NULL COMMENT '关联业务主键',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_notif_user_read (recipient_user_id, read_status),
  KEY idx_notif_user_cat_time (recipient_user_id, msg_category, created_at),
  CONSTRAINT fk_notif_user FOREIGN KEY (recipient_user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内消息通知';

-- 志愿信用
CREATE TABLE volunteer_credit_ledger (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
  request_id BIGINT UNSIGNED NOT NULL COMMENT '需求ID',
  claim_id BIGINT UNSIGNED NOT NULL COMMENT '认领ID',
  hours DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '核销时长',
  rating TINYINT UNSIGNED NULL COMMENT '评价星级',
  credit_delta DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '信用增量',
  calc_version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '计算版本',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_credit_claim (claim_id),
  KEY idx_credit_user_time (user_id, created_at),
  CONSTRAINT fk_credit_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_credit_request FOREIGN KEY (request_id) REFERENCES service_request(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_credit_claim FOREIGN KEY (claim_id) REFERENCES service_claim(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='志愿者信用明细账本';

CREATE TABLE volunteer_credit_snapshot (
  user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
  total_hours DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '累计服务时长',
  avg_rating_30d DECIMAL(4,2) NULL COMMENT '近30天平均评分',
  completion_rate_30d DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '近30天履约率',
  credit_score DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '综合信用分',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (user_id),
  CONSTRAINT fk_credit_snapshot_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='志愿者信用快照';

-- 其他支撑表
CREATE TABLE sys_config (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  config_key VARCHAR(64) NOT NULL COMMENT '配置键',
  config_value JSON NOT NULL COMMENT '配置值',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

CREATE TABLE audit_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NULL COMMENT '操作人ID',
  username VARCHAR(64) NULL COMMENT '操作人用户名',
  role TINYINT UNSIGNED NULL COMMENT '角色',
  module VARCHAR(64) NOT NULL COMMENT '模块',
  action VARCHAR(128) NOT NULL COMMENT '操作动作',
  request_path VARCHAR(256) NULL COMMENT '请求路径',
  http_method VARCHAR(16) NULL COMMENT '请求方法',
  success TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0失败 1成功',
  result_msg VARCHAR(512) NULL COMMENT '结果摘要',
  risk_level VARCHAR(16) NULL DEFAULT 'NORMAL' COMMENT '风险级别',
  ip VARCHAR(64) NULL COMMENT '客户端IP',
  user_agent VARCHAR(512) NULL COMMENT 'User-Agent',
  elapsed_ms INT NULL COMMENT '耗时毫秒',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_audit_created (created_at),
  KEY idx_audit_module (module),
  KEY idx_audit_user (user_id),
  KEY idx_audit_risk (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审计日志表';

CREATE TABLE backup_record (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  record_type VARCHAR(16) NOT NULL COMMENT '类型 BACKUP/RESTORE/EXPORT',
  module VARCHAR(64) NULL COMMENT '模块',
  format VARCHAR(16) NULL COMMENT '格式',
  filename VARCHAR(255) NULL COMMENT '文件名',
  file_path VARCHAR(512) NULL COMMENT '文件路径',
  file_size_mb DECIMAL(10,2) NULL COMMENT '文件大小MB',
  status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态',
  note VARCHAR(255) NULL COMMENT '备注',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_backup_type (record_type),
  KEY idx_backup_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备份恢复导出记录表';

CREATE TABLE verify_code_ticket (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  scene VARCHAR(32) NOT NULL COMMENT '场景',
  target VARCHAR(128) NOT NULL COMMENT '目标邮箱/手机号',
  verify_code VARCHAR(16) NOT NULL COMMENT '验证码',
  expires_at DATETIME(3) NOT NULL COMMENT '过期时间',
  is_used TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否已使用',
  used_at DATETIME(3) NULL COMMENT '使用时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_verify_target_scene (target, scene),
  KEY idx_verify_expire (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='验证码票据表';

CREATE TABLE user_onboarding_profile (
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  skill_tags_json JSON NULL COMMENT '技能标签',
  preferred_features_json JSON NULL COMMENT '偏好功能',
  intent_note VARCHAR(500) NULL COMMENT '补充说明',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (user_id),
  CONSTRAINT fk_onboarding_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户引导问卷主表';

CREATE TABLE user_onboarding_answer (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  question_key VARCHAR(64) NOT NULL COMMENT '问题键',
  answer_value VARCHAR(255) NOT NULL COMMENT '答案值',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_onboarding_answer_user (user_id),
  KEY idx_onboarding_answer_key (question_key),
  CONSTRAINT fk_onboarding_answer_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户引导问卷答案表';

CREATE TABLE time_transaction (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  amount BIGINT NOT NULL COMMENT '变动额度',
  type TINYINT UNSIGNED NOT NULL COMMENT '1服务所得 2兑换消耗 3系统补贴',
  order_id BIGINT UNSIGNED NULL COMMENT '关联订单ID',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_tt_user_time (user_id, create_time),
  KEY idx_tt_order (order_id),
  CONSTRAINT fk_tt_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_tt_order FOREIGN KEY (order_id) REFERENCES service_order(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='时间币流水表';

CREATE TABLE community_banner (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  community_id BIGINT UNSIGNED NULL COMMENT '社区ID，NULL表示全局',
  title VARCHAR(120) NULL COMMENT '标题',
  image_url VARCHAR(500) NOT NULL COMMENT '图片URL',
  link_url VARCHAR(500) NULL COMMENT '跳转链接',
  sort_no INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序',
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_banner_comm_status (community_id, status),
  CONSTRAINT fk_banner_community FOREIGN KEY (community_id) REFERENCES sys_region(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区轮播图表';

SET FOREIGN_KEY_CHECKS = 1;
