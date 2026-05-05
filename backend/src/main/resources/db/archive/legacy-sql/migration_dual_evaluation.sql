-- 双向评价：同一 claim 支持居民/志愿者各评价一次
-- 1) 增加 evaluator_role（1=居民 2=志愿者）
-- 2) 唯一约束从 claim_id 改为 (claim_id, evaluator_role)

ALTER TABLE service_evaluation
  ADD COLUMN evaluator_role TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '评价方角色：1居民 2志愿者' AFTER volunteer_user_id;

ALTER TABLE service_evaluation
  DROP INDEX uk_eval_claim,
  ADD UNIQUE KEY uk_eval_claim_role (claim_id, evaluator_role);

CREATE INDEX idx_eval_evaluator_role ON service_evaluation (evaluator_role);

