-- 注册验证码 + 引导问卷（增量）
USE community_service;

CREATE TABLE IF NOT EXISTS verify_code_ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene VARCHAR(32) NOT NULL COMMENT '场景 REGISTER/RESET_PASSWORD 等',
  target VARCHAR(128) NOT NULL COMMENT '目标邮箱/手机号',
  verify_code VARCHAR(16) NOT NULL COMMENT '验证码',
  expires_at DATETIME NOT NULL COMMENT '过期时间',
  is_used TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用',
  used_at DATETIME NULL COMMENT '使用时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_verify_target_scene (target, scene),
  INDEX idx_verify_expire (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证码票据表';

CREATE TABLE IF NOT EXISTS user_onboarding_profile (
  user_id BIGINT PRIMARY KEY COMMENT '用户ID',
  skill_tags_json JSON NULL COMMENT '技能标签',
  preferred_features_json JSON NULL COMMENT '偏好功能',
  intent_note VARCHAR(500) NULL COMMENT '补充说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_onboarding_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户引导问卷主表';

CREATE TABLE IF NOT EXISTS user_onboarding_answer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  question_key VARCHAR(64) NOT NULL COMMENT '问题键 skill_tags/preferred_features/intent_note',
  answer_value VARCHAR(255) NOT NULL COMMENT '答案值',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_onboarding_answer_user (user_id),
  INDEX idx_onboarding_answer_key (question_key),
  CONSTRAINT fk_onboarding_answer_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户引导问卷答案表';

