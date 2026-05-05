-- 兼容 BCrypt：将 sys_user.password_md5 从 CHAR(32) 扩展为 VARCHAR(100)
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'password_md5'
);

SET @need_alter := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'password_md5'
    AND (
      data_type <> 'varchar'
      OR character_maximum_length < 60
    )
);

SET @sql_stmt := IF(
  @col_exists = 0,
  'SELECT 1',
  IF(@need_alter > 0,
     'ALTER TABLE sys_user MODIFY COLUMN password_md5 VARCHAR(100) NOT NULL COMMENT ''密码哈希（字段名沿用；兼容历史MD5与当前BCrypt）''',
     'SELECT 1')
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
