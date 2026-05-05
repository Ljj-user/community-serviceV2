-- 异常预警 + 志愿时长信用精细化

CREATE TABLE IF NOT EXISTS anomaly_alert_event (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  alert_code VARCHAR(64) NOT NULL COMMENT '告警编码，如 CARE_INACTIVE / DEMAND_SURGE',
  community_id BIGINT UNSIGNED NULL COMMENT '社区ID',
  request_id BIGINT UNSIGNED NULL COMMENT '关联需求ID（可空）',
  target_user_id BIGINT UNSIGNED NULL COMMENT '关联用户ID（可空）',
  severity TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '1低 2中 3高',
  trigger_rule VARCHAR(255) NOT NULL COMMENT '触发规则摘要',
  suggestion_action VARCHAR(255) NULL COMMENT '建议动作',
  rule_snapshot TEXT NULL COMMENT '规则快照(JSON文本)',
  dedup_key VARCHAR(128) NOT NULL COMMENT '去重键',
  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_alert_code_time (alert_code, occurred_at),
  KEY idx_alert_comm_time (community_id, occurred_at),
  KEY idx_alert_req_time (request_id, occurred_at),
  UNIQUE KEY uk_alert_dedup (dedup_key, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常预警事件';

CREATE TABLE IF NOT EXISTS volunteer_credit_ledger (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
  request_id BIGINT UNSIGNED NOT NULL COMMENT '需求ID',
  claim_id BIGINT UNSIGNED NOT NULL COMMENT '认领ID',
  hours DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '核销时长',
  rating TINYINT NULL COMMENT '评价星级(1-5，可空)',
  overtime_penalty DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '时效扣减系数',
  credit_delta DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '本次信用增量',
  calc_version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '计算版本',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_credit_claim (claim_id),
  KEY idx_credit_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿者信用明细账本';

CREATE TABLE IF NOT EXISTS volunteer_credit_snapshot (
  user_id BIGINT UNSIGNED NOT NULL COMMENT '志愿者用户ID',
  total_hours DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '累计服务时长',
  avg_rating_30d DECIMAL(4,2) NULL COMMENT '近30天平均评分',
  completion_rate_30d DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '近30天履约率',
  credit_score DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '综合信用分',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿者信用快照';
