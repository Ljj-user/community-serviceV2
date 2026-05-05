package com.community.platform.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 运行期数据库兜底。
 * 当本地数据库还没完整执行 `init_all.sql` 时，尽量补齐关键表和字段，
 * 避免应用启动时因为缺少结构直接报错。
 */
@Component
public class DbBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DbBootstrapRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        runSafely("ensurePasswordHashColumn", this::ensurePasswordHashColumn);
        runSafely("ensureSysNotification", this::ensureSysNotification);
        runSafely("ensureVerificationAndOnboarding", this::ensureVerificationAndOnboarding);
        runSafely("ensureMatchingTags", this::ensureMatchingTags);
        runSafely("ensureCommunityInviteCode", this::ensureCommunityInviteCode);
        runSafely("ensureCommunityBanner", this::ensureCommunityBanner);
        runSafely("ensureAnomalyAlertAndCredit", this::ensureAnomalyAlertAndCredit);
        runSafely("ensureAiAnalysisRecord", this::ensureAiAnalysisRecord);
        runSafely("ensureDualEvaluationSchema", this::ensureDualEvaluationSchema);
        runSafely("ensureRuntimeConfig", this::ensureRuntimeConfig);
    }

    private void runSafely(String stepName, Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            log.warn("Db bootstrap step failed: {}", stepName, ex);
        }
    }

    private void ensurePasswordHashColumn() {
        Integer cnt = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name='sys_user'
                  AND column_name='password_md5'
                  AND (
                    data_type <> 'varchar'
                    OR character_maximum_length < 60
                  )
                """, Integer.class);
        if (cnt != null && cnt > 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE sys_user
                    MODIFY COLUMN password_md5 VARCHAR(100) NOT NULL COMMENT '密码哈希，字段名沿用旧版本，兼容历史 MD5 与当前 BCrypt'
                    """);
        }
    }

    private void ensureSysNotification() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_notification (
                  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                  recipient_user_id BIGINT UNSIGNED NOT NULL COMMENT '接收用户 ID',
                  title             VARCHAR(200)     NOT NULL COMMENT '标题',
                  summary           VARCHAR(500)     NULL COMMENT '摘要',
                  msg_category      TINYINT UNSIGNED NOT NULL COMMENT '1业务待办 2系统公告',
                  read_status       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
                  ref_type          VARCHAR(32)      NULL COMMENT '关联业务类型',
                  ref_id            BIGINT UNSIGNED  NULL COMMENT '关联业务主键',
                  created_at        DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                  PRIMARY KEY (id),
                  KEY idx_notif_user_read (recipient_user_id, read_status),
                  KEY idx_notif_user_cat_time (recipient_user_id, msg_category, created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统通知表';
                """);
    }

    private void ensureVerificationAndOnboarding() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS verify_code_ticket (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  scene VARCHAR(32) NOT NULL,
                  target VARCHAR(128) NOT NULL,
                  verify_code VARCHAR(16) NOT NULL,
                  expires_at DATETIME NOT NULL,
                  is_used TINYINT NOT NULL DEFAULT 0,
                  used_at DATETIME NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX idx_verify_target_scene (target, scene),
                  INDEX idx_verify_expire (expires_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证码票据表';
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_onboarding_profile (
                  user_id BIGINT PRIMARY KEY,
                  skill_tags_json JSON NULL,
                  preferred_features_json JSON NULL,
                  intent_note VARCHAR(500) NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户引导档案表';
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_onboarding_answer (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  question_key VARCHAR(64) NOT NULL,
                  answer_value VARCHAR(255) NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_onboarding_answer_user (user_id),
                  INDEX idx_onboarding_answer_key (question_key)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户引导问卷答案表';
                """);
    }

    private void ensureMatchingTags() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_user_skill (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  skill_tag VARCHAR(64) NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE KEY uk_user_skill (user_id, skill_tag),
                  INDEX idx_user_skill_user (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户技能表';
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS service_request_tag (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  request_id BIGINT NOT NULL,
                  tag_name VARCHAR(64) NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE KEY uk_request_tag (request_id, tag_name),
                  INDEX idx_request_tag_req (request_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求标签表';
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS skill_tag_stat (
                  skill_tag VARCHAR(64) PRIMARY KEY,
                  user_count INT NOT NULL DEFAULT 0,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能标签统计表，用于推荐冷启动';
                """);
    }

    private void ensureCommunityInviteCode() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS community_invite_code (
                  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                  community_id  BIGINT UNSIGNED NOT NULL COMMENT '社区 ID，对应 sys_region.id',
                  code          VARCHAR(32)      NOT NULL COMMENT '邀请码',
                  status        TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
                  expires_at    DATETIME(3)      NULL COMMENT '过期时间，NULL 表示不过期',
                  max_uses      INT UNSIGNED     NOT NULL DEFAULT 100 COMMENT '最大可使用次数',
                  used_count    INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '已使用次数',
                  created_by    BIGINT UNSIGNED  NULL COMMENT '创建人用户 ID',
                  created_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                  updated_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_invite_code (code),
                  KEY idx_invite_comm_status (community_id, status),
                  KEY idx_invite_expire (expires_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区邀请码表';
                """);
    }

    private void ensureCommunityBanner() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS community_banner (
                  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                  community_id BIGINT UNSIGNED NULL COMMENT '社区 ID，NULL 表示全局默认',
                  title        VARCHAR(100)    NOT NULL COMMENT '主标题',
                  subtitle     VARCHAR(200)    NULL COMMENT '副标题',
                  image_url    VARCHAR(500)    NULL COMMENT '图片地址',
                  link_url     VARCHAR(500)    NULL COMMENT '点击跳转地址',
                  sort_no      INT             NOT NULL DEFAULT 0 COMMENT '排序值',
                  status       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
                  created_by   BIGINT UNSIGNED NULL COMMENT '创建人用户 ID',
                  created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                  updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                  PRIMARY KEY (id),
                  KEY idx_banner_comm_status_sort (community_id, status, sort_no),
                  KEY idx_banner_comm_time (community_id, created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区轮播图表';
                """);
    }

    private void ensureAnomalyAlertAndCredit() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS anomaly_alert_event (
                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                  alert_code VARCHAR(64) NOT NULL,
                  community_id BIGINT UNSIGNED NULL,
                  request_id BIGINT UNSIGNED NULL,
                  target_user_id BIGINT UNSIGNED NULL,
                  severity TINYINT UNSIGNED NOT NULL DEFAULT 2,
                  trigger_rule VARCHAR(255) NOT NULL,
                  suggestion_action VARCHAR(255) NULL,
                  rule_snapshot TEXT NULL,
                  dedup_key VARCHAR(128) NOT NULL,
                  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  KEY idx_alert_code_time (alert_code, occurred_at),
                  KEY idx_alert_comm_time (community_id, occurred_at),
                  KEY idx_alert_req_time (request_id, occurred_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常预警事件表';
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS volunteer_credit_ledger (
                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                  user_id BIGINT UNSIGNED NOT NULL,
                  request_id BIGINT UNSIGNED NOT NULL,
                  claim_id BIGINT UNSIGNED NOT NULL,
                  hours DECIMAL(10,2) NOT NULL DEFAULT 0,
                  rating TINYINT NULL,
                  overtime_penalty DECIMAL(5,2) NOT NULL DEFAULT 1.00,
                  credit_delta DECIMAL(10,2) NOT NULL DEFAULT 0,
                  calc_version VARCHAR(32) NOT NULL DEFAULT 'v1',
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_credit_claim (claim_id),
                  KEY idx_credit_user_time (user_id, created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿者信用明细账本';
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS volunteer_credit_snapshot (
                  user_id BIGINT UNSIGNED NOT NULL,
                  total_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
                  avg_rating_30d DECIMAL(4,2) NULL,
                  completion_rate_30d DECIMAL(5,2) NOT NULL DEFAULT 0,
                  credit_score DECIMAL(12,2) NOT NULL DEFAULT 0,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿者信用快照表';
                """);
    }

    private void ensureAiAnalysisRecord() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_analysis_record (
                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                  user_id BIGINT UNSIGNED NOT NULL,
                  community_id BIGINT UNSIGNED NULL,
                  scene VARCHAR(32) NOT NULL DEFAULT 'mobile_assistant',
                  input_text TEXT NOT NULL,
                  result_mode VARCHAR(32) NOT NULL DEFAULT 'FAQ',
                  result_json JSON NULL,
                  applied_to_form TINYINT UNSIGNED NOT NULL DEFAULT 0,
                  submitted_success TINYINT UNSIGNED NOT NULL DEFAULT 0,
                  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                  PRIMARY KEY (id),
                  KEY idx_ai_analysis_user_time (user_id, created_at),
                  KEY idx_ai_analysis_comm_time (community_id, created_at),
                  KEY idx_ai_analysis_mode_time (result_mode, created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 分析记录表';
                """);
    }

    private void ensureRuntimeConfig() {
        Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM sys_config WHERE config_key='runtime'", Integer.class);
        if (cnt == null || cnt == 0) {
            jdbcTemplate.update("""
                    INSERT INTO sys_config(config_key, config_value, created_at, updated_at)
                    VALUES('runtime', CAST(? AS JSON), NOW(3), NOW(3))
                    """, "{\"demoModeEnabled\":true}");
        }
    }

    /**
     * 双向评价结构兜底。
     * 1. 补 evaluator_role 字段。
     * 2. 将唯一索引从 uk_eval_claim 升级为 uk_eval_claim_role(claim_id, evaluator_role)。
     */
    private void ensureDualEvaluationSchema() {
        Integer hasColumn = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name='service_evaluation'
                  AND column_name='evaluator_role'
                """, Integer.class);
        if (hasColumn == null || hasColumn == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE service_evaluation
                    ADD COLUMN evaluator_role TINYINT UNSIGNED NOT NULL DEFAULT 1
                    COMMENT '评价方角色：1居民 2志愿者'
                    AFTER volunteer_user_id
                    """);
        }

        Integer hasOldUk = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name='service_evaluation'
                  AND index_name='uk_eval_claim'
                """, Integer.class);
        if (hasOldUk != null && hasOldUk > 0) {
            try {
                jdbcTemplate.execute("ALTER TABLE service_evaluation DROP INDEX uk_eval_claim");
            } catch (DataAccessException ex) {
                // 历史库可能存在与外键依赖同名索引的场景，此时跳过删除，优先保证应用可启动。
            }
        }

        Integer hasNewUk = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name='service_evaluation'
                  AND index_name='uk_eval_claim_role'
                """, Integer.class);
        if (hasNewUk == null || hasNewUk == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE service_evaluation
                    ADD UNIQUE KEY uk_eval_claim_role (claim_id, evaluator_role)
                    """);
        }

        Integer hasRoleIndex = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name='service_evaluation'
                  AND index_name='idx_eval_evaluator_role'
                """, Integer.class);
        if (hasRoleIndex == null || hasRoleIndex == 0) {
            jdbcTemplate.execute("""
                    CREATE INDEX idx_eval_evaluator_role
                    ON service_evaluation (evaluator_role)
                    """);
        }
    }
}
