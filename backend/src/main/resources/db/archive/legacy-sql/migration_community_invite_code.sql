-- 社区邀请码（移动端加入/改绑社区，无审核）
-- 依赖：sys_region（community_id 指向 sys_region.id）

CREATE TABLE IF NOT EXISTS community_invite_code (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  community_id  BIGINT UNSIGNED NOT NULL COMMENT '社区ID（sys_region.id，通常 level=3）',
  code          VARCHAR(32)      NOT NULL COMMENT '邀请码（短码）',
  status        TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
  expires_at    DATETIME(3)      NULL COMMENT '过期时间（NULL=不过期）',
  max_uses      INT UNSIGNED     NOT NULL DEFAULT 100 COMMENT '最大可用次数',
  used_count    INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '已使用次数',
  created_by    BIGINT UNSIGNED  NULL COMMENT '创建人用户ID',
  created_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_invite_code (code),
  KEY idx_invite_comm_status (community_id, status),
  KEY idx_invite_expire (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区邀请码';

