-- 智能匹配：技能与需求标签结构化（图3落库）
USE community_service;

CREATE TABLE IF NOT EXISTS sys_user_skill (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '关联用户',
  skill_tag VARCHAR(64) NOT NULL COMMENT '技能标签',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_skill (user_id, skill_tag),
  INDEX idx_user_skill_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户技能表';

CREATE TABLE IF NOT EXISTS service_request_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL COMMENT '关联需求',
  tag_name VARCHAR(64) NOT NULL COMMENT '需求标签',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_request_tag (request_id, tag_name),
  INDEX idx_request_tag_req (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求标签表';

CREATE TABLE IF NOT EXISTS skill_tag_stat (
  skill_tag VARCHAR(64) PRIMARY KEY COMMENT '技能标签',
  user_count INT NOT NULL DEFAULT 0 COMMENT '具备该技能的用户数',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能热度统计（用于新用户推荐冷启动）';

