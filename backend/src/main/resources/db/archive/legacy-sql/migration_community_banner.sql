-- 社区轮播图（移动端首页 Banner）
-- 说明：按 community_id 隔离；community_id=NULL 表示全局默认（兜底）

CREATE TABLE IF NOT EXISTS community_banner (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  community_id BIGINT UNSIGNED NULL COMMENT '社区ID（NULL=全局默认）',
  title        VARCHAR(100)    NOT NULL COMMENT '主标题',
  subtitle     VARCHAR(200)    NULL COMMENT '副标题',
  image_url    VARCHAR(500)    NULL COMMENT '图片URL（可空：纯文案卡片）',
  link_url     VARCHAR(500)    NULL COMMENT '点击跳转链接（可空）',
  sort_no      INT             NOT NULL DEFAULT 0 COMMENT '排序（小在前）',
  status       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
  created_by   BIGINT UNSIGNED NULL COMMENT '创建人用户ID',
  created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_banner_comm_status_sort (community_id, status, sort_no),
  KEY idx_banner_comm_time (community_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区轮播图';

