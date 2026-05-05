-- 一次性修复：演示账号已有行但 community_id 等为 NULL 时执行（请先保证 sys_region 已插入）
USE community_service;

UPDATE sys_user SET community_id = NULL, time_coins = 0, points = 0 WHERE username = 'admin';
UPDATE sys_user SET community_id = 3001, time_coins = 0, points = 0 WHERE username = 'manager';
UPDATE sys_user SET community_id = 3002, time_coins = 0, points = 0 WHERE username = 'manager2';
UPDATE sys_user SET community_id = 3001, time_coins = 5, points = 120, identity_tag = '普通居民' WHERE username = 'resident1';
UPDATE sys_user SET community_id = 3001, time_coins = 0, points = 80, identity_tag = '孤寡老人' WHERE username = 'resident2';
UPDATE sys_user SET community_id = 3002, time_coins = 1, points = 60, identity_tag = '残疾人' WHERE username = 'resident3';
UPDATE sys_user SET community_id = 3001, time_coins = 0, points = 200, identity_tag = '活力老人' WHERE username = 'volunteer1';
UPDATE sys_user SET community_id = 3002, time_coins = 0, points = 150, identity_tag = '普通居民' WHERE username = 'volunteer2';
UPDATE sys_user SET community_id = 3001, time_coins = 0, points = 90, identity_tag = '普通居民' WHERE username = 'volunteer3';
UPDATE sys_user SET community_id = 3001, time_coins = 0, points = 70, identity_tag = '普通居民' WHERE username = 'both1';
