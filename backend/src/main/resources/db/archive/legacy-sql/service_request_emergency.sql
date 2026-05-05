USE community_service;

-- 可重复执行：按列检查是否存在再添加（避免 init_all.sql 重复执行时报错）

SET @__col_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'service_request'
    AND column_name = 'emergency_contact_name'
);
SET @__sql := IF(@__col_exists > 0,
  'SELECT 1',
  'ALTER TABLE service_request ADD COLUMN emergency_contact_name VARCHAR(64) NULL COMMENT ''紧急联系人姓名'' AFTER urgency_level'
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

SET @__col_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'service_request'
    AND column_name = 'emergency_contact_phone'
);
SET @__sql := IF(@__col_exists > 0,
  'SELECT 1',
  'ALTER TABLE service_request ADD COLUMN emergency_contact_phone VARCHAR(32) NULL COMMENT ''紧急联系人电话'' AFTER emergency_contact_name'
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

SET @__col_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'service_request'
    AND column_name = 'emergency_contact_relation'
);
SET @__sql := IF(@__col_exists > 0,
  'SELECT 1',
  'ALTER TABLE service_request ADD COLUMN emergency_contact_relation VARCHAR(32) NULL COMMENT ''与服务对象关系（子女/邻居等）'' AFTER emergency_contact_phone'
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;