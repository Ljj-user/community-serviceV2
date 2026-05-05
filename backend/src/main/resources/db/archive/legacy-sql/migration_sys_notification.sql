-- 用户消息通知（业务待办 / 系统公告）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_notification (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  recipient_user_id   BIGINT UNSIGNED NOT NULL COMMENT '接收人用户ID',
  title               VARCHAR(200)     NOT NULL COMMENT '标题',
  summary             VARCHAR(500)     NULL COMMENT '摘要',
  msg_category        TINYINT UNSIGNED NOT NULL COMMENT '1业务待办 2系统公告',
  read_status         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  ref_type            VARCHAR(32)      NULL COMMENT '关联业务类型',
  ref_id              BIGINT UNSIGNED  NULL COMMENT '关联业务主键',
  created_at          DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_notif_user_read (recipient_user_id, read_status),
  KEY idx_notif_user_cat_time (recipient_user_id, msg_category, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内消息通知';
