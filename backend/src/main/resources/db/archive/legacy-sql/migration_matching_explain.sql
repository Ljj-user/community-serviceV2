-- 可解释匹配快照（预留扩展）
USE community_service;

CREATE TABLE IF NOT EXISTS volunteer_match_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL COMMENT '需求ID',
  volunteer_user_id BIGINT NOT NULL COMMENT '志愿者用户ID',
  total_score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '综合评分',
  skill_score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '技能匹配分',
  area_score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '地理距离分',
  priority_score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '紧急程度分',
  rating_score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '历史评价分',
  weight_skill DECIMAL(5,2) NOT NULL DEFAULT 0.50 COMMENT '技能权重',
  weight_area DECIMAL(5,2) NOT NULL DEFAULT 0.30 COMMENT '地理权重',
  weight_priority DECIMAL(5,2) NOT NULL DEFAULT 0.10 COMMENT '紧急权重',
  weight_rating DECIMAL(5,2) NOT NULL DEFAULT 0.10 COMMENT '历史权重',
  reason_tags_json JSON NULL COMMENT '推荐理由标签',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_match_req_vol (request_id, volunteer_user_id),
  INDEX idx_match_total (total_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿者匹配解释快照';

