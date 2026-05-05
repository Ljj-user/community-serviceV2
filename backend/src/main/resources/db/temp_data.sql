-- 社区公益服务对接管理平台 - PRD v2 最小演示数据
-- 演示账号口令已脱敏，请在本地初始化说明中自行配置。

USE community_service;
SET NAMES utf8mb4;

-- 1) 社区区域
INSERT INTO sys_region (id, name, level, parent_id, province, city, address, contact_phone, status) VALUES
(100, '西湖区', 1, NULL, '浙江省', '杭州市', NULL, NULL, 1),
(110, '文新街道', 2, 100, '浙江省', '杭州市', NULL, NULL, 1),
(3001, '翠苑社区', 3, 110, '浙江省', '杭州市', '杭州市西湖区文新街道翠苑社区', '0571-88880001', 1),
(3002, '文苑社区', 3, 110, '浙江省', '杭州市', '杭州市西湖区文新街道文苑社区', '0571-88880002', 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  level = VALUES(level),
  parent_id = VALUES(parent_id),
  province = VALUES(province),
  city = VALUES(city),
  address = VALUES(address),
  contact_phone = VALUES(contact_phone),
  status = VALUES(status);

-- 2) 系统配置
INSERT INTO sys_config (config_key, config_value) VALUES
('basic', '{"pointsPerHour":10,"feedbackDays":3,"enableLargeText":true}'),
('notice', '{"demandApproved":"您的求助已通过审核。","demandRejected":"您的求助未通过审核。"}'),
('alert', '{"careInactivityDays":3,"surge24hMinRequests":5,"surgeMultiplier":2,"enableCareInactivityAlert":true,"enableDemandSurgeAlert":true,"alertNotifyChannels":["IN_APP"]}'),
('ai', '{"enabled":false,"provider":"deepseek","model":"deepseek-v4-flash"}')
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  updated_at = NOW(3);

-- 3) 用户
INSERT INTO sys_user (
  id, username, password_md5, role, identity_type, community_id, community_join_status,
  real_name, phone, email, avatar_url, gender, address, time_coins, points, identity_tag,
  skill_tags, status, last_login_at, created_at, updated_at, is_deleted
) VALUES
(1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, NULL, 0, '系统管理员', '13800000001', 'admin@demo.com', NULL, 1, '平台管理中心', 0, 0, NULL, NULL, 1, NOW(3), NOW(3), NOW(3), 0),
(2, 'manager', 'e10adc3949ba59abbe56e057f20f883e', 2, 1, 3001, 2, '翠苑社区管理员', '13800000002', 'manager@demo.com', NULL, 2, '翠苑社区服务站', 0, 0, NULL, NULL, 1, NOW(3), NOW(3), NOW(3), 0),
(3, 'resident1', 'e10adc3949ba59abbe56e057f20f883e', 3, 1, 3001, 2, '张大爷', '13800000011', 'resident1@demo.com', NULL, 1, '翠苑社区1栋101', 20, 100, '普通居民', NULL, 1, NOW(3), NOW(3), NOW(3), 0),
(4, 'elder1', 'e10adc3949ba59abbe56e057f20f883e', 3, 1, 3001, 2, '李奶奶', '13800000012', 'elder1@demo.com', NULL, 2, '翠苑社区2栋202', 10, 80, '独居老人', NULL, 1, DATE_SUB(NOW(3), INTERVAL 7 DAY), NOW(3), NOW(3), 0),
(5, 'volunteer1', 'e10adc3949ba59abbe56e057f20f883e', 3, 2, 3001, 2, '志愿者小王', '13800000021', 'vol1@demo.com', NULL, 1, '翠苑社区5栋501', 0, 200, '普通居民', '["护理","助浴","陪伴"]', 1, NOW(3), NOW(3), NOW(3), 0),
(6, 'pending1', 'e10adc3949ba59abbe56e057f20f883e', 3, 1, NULL, 1, '待审核居民', '13800000031', 'pending1@demo.com', NULL, 2, '翠苑社区7栋701', 0, 0, '普通居民', NULL, 1, NOW(3), NOW(3), NOW(3), 0)
ON DUPLICATE KEY UPDATE
  role = VALUES(role),
  identity_type = VALUES(identity_type),
  community_id = VALUES(community_id),
  community_join_status = VALUES(community_join_status),
  real_name = VALUES(real_name),
  phone = VALUES(phone),
  email = VALUES(email),
  gender = VALUES(gender),
  address = VALUES(address),
  time_coins = VALUES(time_coins),
  points = VALUES(points),
  identity_tag = VALUES(identity_tag),
  skill_tags = VALUES(skill_tags),
  status = VALUES(status),
  last_login_at = VALUES(last_login_at),
  updated_at = NOW(3),
  is_deleted = 0;

-- 4) 社区邀请码与加入申请
INSERT INTO community_invite_code
  (id, community_id, code, status, expires_at, max_uses, used_count, created_by)
VALUES
  (9001, 3001, 'DEMO3001', 1, DATE_ADD(NOW(3), INTERVAL 30 DAY), 200, 1, 2),
  (9002, 3002, 'DEMO3002', 1, DATE_ADD(NOW(3), INTERVAL 30 DAY), 200, 0, 1)
ON DUPLICATE KEY UPDATE
  community_id = VALUES(community_id),
  status = VALUES(status),
  expires_at = VALUES(expires_at),
  max_uses = VALUES(max_uses),
  used_count = VALUES(used_count),
  updated_at = NOW(3);

INSERT INTO community_join_application
  (id, user_id, community_id, invite_code, real_name, phone, address, status, reviewer_user_id, reviewed_at, reject_reason)
VALUES
  (9101, 6, 3001, 'DEMO3001', '待审核居民', '13800000031', '翠苑社区7栋701', 0, NULL, NULL, NULL),
  (9102, 3, 3001, 'DEMO3001', '张大爷', '13800000011', '翠苑社区1栋101', 1, 2, NOW(3), NULL)
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  reviewer_user_id = VALUES(reviewer_user_id),
  reviewed_at = VALUES(reviewed_at),
  reject_reason = VALUES(reject_reason),
  updated_at = NOW(3);

-- 5) 志愿者认证与重点关怀
INSERT INTO volunteer_profile
  (id, user_id, community_id, cert_status, real_name, id_card_no, skill_tags, service_radius_km, available_time, service_count, completed_count, cancelled_count, avg_rating, reviewer_user_id, certified_at)
VALUES
  (5001, 5, 3001, 2, '志愿者小王', '330100199001010011', '["护理","助浴","陪伴"]', 3.00, '工作日晚上、周末全天', 12, 10, 1, 4.80, 2, NOW(3))
ON DUPLICATE KEY UPDATE
  community_id = VALUES(community_id),
  cert_status = VALUES(cert_status),
  skill_tags = VALUES(skill_tags),
  service_radius_km = VALUES(service_radius_km),
  available_time = VALUES(available_time),
  service_count = VALUES(service_count),
  completed_count = VALUES(completed_count),
  cancelled_count = VALUES(cancelled_count),
  avg_rating = VALUES(avg_rating),
  updated_at = NOW(3);

INSERT INTO care_subject_profile
  (id, user_id, community_id, care_type, care_level, living_status, health_note, emergency_contact_name, emergency_contact_phone, emergency_contact_relation, monitor_enabled, last_visit_at, created_by)
VALUES
  (6001, 4, 3001, '独居老人', 3, '独居', '行动较慢，需要定期关注', '李女士', '13900000012', '女儿', 1, DATE_SUB(NOW(3), INTERVAL 10 DAY), 2)
ON DUPLICATE KEY UPDATE
  community_id = VALUES(community_id),
  care_type = VALUES(care_type),
  care_level = VALUES(care_level),
  living_status = VALUES(living_status),
  health_note = VALUES(health_note),
  emergency_contact_name = VALUES(emergency_contact_name),
  emergency_contact_phone = VALUES(emergency_contact_phone),
  emergency_contact_relation = VALUES(emergency_contact_relation),
  monitor_enabled = VALUES(monitor_enabled),
  last_visit_at = VALUES(last_visit_at),
  updated_at = NOW(3),
  is_deleted = 0;

-- 6) 公益需求、接单、订单、评价
INSERT INTO service_request
  (id, requester_user_id, target_user_id, community_id, title, service_type, description, service_address, contact_phone, expected_time, urgency_level, priority_flag, emergency_contact_name, emergency_contact_phone, emergency_contact_relation, special_tags, status, audit_by_user_id, audit_at, published_at, claimed_at, completed_at)
VALUES
  (8001, 4, 4, 3001, '需要陪诊去社区医院', '陪诊陪同', '上午去社区医院复诊，希望有人陪同排队取药。', '翠苑社区2栋202', '13800000012', DATE_ADD(NOW(3), INTERVAL 1 DAY), 4, 1, '李女士', '13900000012', '女儿', '["独居老人","陪诊"]', 1, 2, NOW(3), NOW(3), NULL, NULL),
  (8002, 3, 3, 3001, '帮忙买菜取药', '代买取药', '腿脚不方便，需要买青菜和降压药。', '翠苑社区1栋101', '13800000011', DATE_ADD(NOW(3), INTERVAL 2 DAY), 2, 0, NULL, NULL, NULL, '["代买","取药"]', 2, 2, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3), NULL),
  (8003, 4, 4, 3001, '周末家务协助', '家务协助', '帮忙打扫厨房和客厅。', '翠苑社区2栋202', '13800000012', DATE_SUB(NOW(3), INTERVAL 2 DAY), 2, 0, '李女士', '13900000012', '女儿', '["家务","独居老人"]', 3, 2, DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  service_type = VALUES(service_type),
  description = VALUES(description),
  service_address = VALUES(service_address),
  contact_phone = VALUES(contact_phone),
  expected_time = VALUES(expected_time),
  urgency_level = VALUES(urgency_level),
  priority_flag = VALUES(priority_flag),
  special_tags = VALUES(special_tags),
  status = VALUES(status),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO service_claim
  (id, request_id, volunteer_user_id, claim_at, claim_status, service_hours, hours_submitted_at, completion_note)
VALUES
  (8101, 8002, 5, NOW(3), 1, NULL, NULL, NULL),
  (8102, 8003, 5, DATE_SUB(NOW(3), INTERVAL 3 DAY), 2, 2.00, DATE_SUB(NOW(3), INTERVAL 2 DAY), '已完成家务协助')
ON DUPLICATE KEY UPDATE
  request_id = VALUES(request_id),
  volunteer_user_id = VALUES(volunteer_user_id),
  claim_status = VALUES(claim_status),
  service_hours = VALUES(service_hours),
  completion_note = VALUES(completion_note),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO service_order
  (id, request_id, claim_id, volunteer_user_id, community_id, status)
VALUES
  (8201, 8002, 8101, 5, 3001, 2),
  (8202, 8003, 8102, 5, 3001, 5)
ON DUPLICATE KEY UPDATE
  claim_id = VALUES(claim_id),
  volunteer_user_id = VALUES(volunteer_user_id),
  status = VALUES(status),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO service_evaluation
  (id, claim_id, request_id, resident_user_id, volunteer_user_id, evaluator_role, rating, content)
VALUES
  (8301, 8102, 8003, 4, 5, 1, 5, '很认真，也很准时。')
ON DUPLICATE KEY UPDATE
  rating = VALUES(rating),
  content = VALUES(content),
  updated_at = NOW(3),
  is_deleted = 0;

-- 7) 公告与便民信息
INSERT INTO announcement
  (id, title, content_html, content_text, target_scope, target_community_id, status, is_top, top_at, publisher_user_id, published_at)
VALUES
  (7001, '翠苑社区志愿者招募', '<p>本周六上午9点，社区招募陪诊和代买志愿者。</p>', '本周六上午9点，社区招募陪诊和代买志愿者。', 1, 3001, 1, 1, NOW(3), 2, NOW(3)),
  (7002, '社区卫生服务站义诊通知', '<p>本周三下午两点，社区卫生服务站提供义诊。</p>', '本周三下午两点，社区卫生服务站提供义诊。', 1, 3001, 1, 0, NULL, 2, NOW(3))
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  content_html = VALUES(content_html),
  content_text = VALUES(content_text),
  status = VALUES(status),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO convenience_info
  (id, community_id, category, title, content, contact_phone, address, sort_no, status, created_by)
VALUES
  (7201, 3001, '社区电话', '翠苑社区服务站', '工作日 9:00-17:30 可咨询公益服务。', '0571-88880001', '翠苑社区服务站', 1, 1, 2),
  (7202, 3001, '医院药店', '文新街道社区卫生服务中心', '可提供基础诊疗和取药服务。', '0571-88881234', '文新街道卫生服务中心', 2, 1, 2),
  (7203, 3001, '维修服务', '社区便民维修点', '水电小修可先电话咨询。', '0571-88885678', '翠苑二区门口', 3, 1, 2)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  content = VALUES(content),
  contact_phone = VALUES(contact_phone),
  address = VALUES(address),
  sort_no = VALUES(sort_no),
  status = VALUES(status),
  updated_at = NOW(3),
  is_deleted = 0;

-- 8) 推荐、信用、预警
INSERT INTO sys_user_skill (user_id, skill_tag) VALUES
  (5, '护理'),
  (5, '助浴'),
  (5, '陪伴'),
  (5, '代买取药')
ON DUPLICATE KEY UPDATE created_at = NOW(3);

INSERT INTO service_request_tag (request_id, tag_name) VALUES
  (8001, '陪诊'),
  (8001, '独居老人'),
  (8002, '代买取药'),
  (8003, '家务协助')
ON DUPLICATE KEY UPDATE created_at = NOW(3);

INSERT INTO volunteer_match_snapshot
  (id, request_id, volunteer_user_id, total_score, skill_score, area_score, priority_score, rating_score, reason_tags_json)
VALUES
  (8401, 8001, 5, 91.50, 35.00, 25.00, 20.00, 11.50, '["同社区","技能匹配","评分较高"]')
ON DUPLICATE KEY UPDATE
  total_score = VALUES(total_score),
  reason_tags_json = VALUES(reason_tags_json);

INSERT INTO volunteer_credit_snapshot
  (user_id, total_hours, avg_rating_30d, completion_rate_30d, credit_score)
VALUES
  (5, 26.50, 4.80, 0.92, 88.00)
ON DUPLICATE KEY UPDATE
  total_hours = VALUES(total_hours),
  avg_rating_30d = VALUES(avg_rating_30d),
  completion_rate_30d = VALUES(completion_rate_30d),
  credit_score = VALUES(credit_score),
  updated_at = NOW(3);

INSERT INTO anomaly_alert_event
  (id, alert_code, community_id, request_id, target_user_id, severity, trigger_rule, suggestion_action, rule_snapshot, dedup_key, status)
VALUES
  (8501, 'CARE_INACTIVE', 3001, NULL, 4, 2, '重点对象连续3天未登录', '建议社区管理员电话关怀或上门核实', '{"days":3}', CONCAT('CARE_INACTIVE:4:', CURDATE()), 0)
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  trigger_rule = VALUES(trigger_rule),
  suggestion_action = VALUES(suggestion_action);

INSERT INTO sys_notification
  (recipient_user_id, title, summary, msg_category, read_status, ref_type, ref_id)
VALUES
  (2, '关怀预警：重点对象未登录', '李奶奶已连续多日未登录，建议主动跟进。', 1, 0, 'CARE_ALERT', 8501);
-- 9) Extra mock data for Cuiyuan community (3001)
INSERT INTO sys_user (
  id, username, password_md5, role, identity_type, community_id, community_join_status,
  real_name, phone, email, avatar_url, gender, address, time_coins, points, identity_tag,
  skill_tags, status, last_login_at, created_at, updated_at, is_deleted
) VALUES
(7, 'resident2', 'e10adc3949ba59abbe56e057f20f883e', 3, 1, 3001, 2, '王阿姨', '13800000013', 'resident2@demo.com', NULL, 2, '翠苑社区3栋303', 6, 76, '慢病居民', NULL, 1, DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3), NOW(3), 0),
(8, 'resident3', 'e10adc3949ba59abbe56e057f20f883e', 3, 1, 3001, 2, '陈伯伯', '13800000014', 'resident3@demo.com', NULL, 1, '翠苑社区4栋505', 3, 68, '重点关怀对象', NULL, 1, DATE_SUB(NOW(3), INTERVAL 5 DAY), NOW(3), NOW(3), 0),
(9, 'volunteer2', 'e10adc3949ba59abbe56e057f20f883e', 3, 2, 3001, 2, '志愿者小刘', '13800000022', 'vol2@demo.com', NULL, 1, '翠苑社区6栋202', 0, 186, '普通居民', '["助浴","护理","陪伴"]', 1, DATE_SUB(NOW(3), INTERVAL 3 HOUR), NOW(3), NOW(3), 0),
(10, 'volunteer3', 'e10adc3949ba59abbe56e057f20f883e', 3, 2, 3001, 2, '志愿者小吴', '13800000023', 'vol3@demo.com', NULL, 2, '翠苑社区8栋101', 0, 158, '普通居民', '["代买取药","家务协助","政务代办"]', 1, DATE_SUB(NOW(3), INTERVAL 6 HOUR), NOW(3), NOW(3), 0),
(11, 'pending2', 'e10adc3949ba59abbe56e057f20f883e', 3, 1, NULL, 1, '待审核住户', '13800000032', 'pending2@demo.com', NULL, 1, '翠苑社区9栋202', 0, 0, '普通居民', NULL, 1, NOW(3), NOW(3), NOW(3), 0)
ON DUPLICATE KEY UPDATE
  community_id = VALUES(community_id),
  community_join_status = VALUES(community_join_status),
  real_name = VALUES(real_name),
  phone = VALUES(phone),
  email = VALUES(email),
  gender = VALUES(gender),
  address = VALUES(address),
  time_coins = VALUES(time_coins),
  points = VALUES(points),
  identity_tag = VALUES(identity_tag),
  skill_tags = VALUES(skill_tags),
  status = VALUES(status),
  last_login_at = VALUES(last_login_at),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO community_join_application
  (id, user_id, community_id, invite_code, real_name, phone, address, status, reviewer_user_id, reviewed_at, reject_reason)
VALUES
  (9105, 11, 3001, 'DEMO3001', '待审核住户', '13800000032', '翠苑社区9栋202', 0, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  reviewer_user_id = VALUES(reviewer_user_id),
  reviewed_at = VALUES(reviewed_at),
  reject_reason = VALUES(reject_reason),
  updated_at = NOW(3);

UPDATE community_invite_code
SET used_count = GREATEST(used_count, 2),
    updated_at = NOW(3)
WHERE id = 9001;

INSERT INTO volunteer_profile
  (id, user_id, community_id, cert_status, real_name, id_card_no, skill_tags, service_radius_km, available_time, service_count, completed_count, cancelled_count, avg_rating, reviewer_user_id, certified_at)
VALUES
  (5002, 9, 3001, 2, '志愿者小刘', '330100199303030022', '["助浴","护理","陪伴"]', 2.50, '工作日上午、周末下午', 15, 13, 1, 4.85, 2, NOW(3)),
  (5005, 10, 3001, 2, '志愿者小吴', '330100199507070023', '["代买取药","家务协助","政务代办"]', 4.00, '工作日晚间、周末全天', 9, 8, 0, 4.70, 2, NOW(3))
ON DUPLICATE KEY UPDATE
  community_id = VALUES(community_id),
  cert_status = VALUES(cert_status),
  skill_tags = VALUES(skill_tags),
  service_radius_km = VALUES(service_radius_km),
  available_time = VALUES(available_time),
  service_count = VALUES(service_count),
  completed_count = VALUES(completed_count),
  cancelled_count = VALUES(cancelled_count),
  avg_rating = VALUES(avg_rating),
  updated_at = NOW(3);

INSERT INTO care_subject_profile
  (id, user_id, community_id, care_type, care_level, living_status, health_note, emergency_contact_name, emergency_contact_phone, emergency_contact_relation, monitor_enabled, last_visit_at, created_by)
VALUES
  (6002, 8, 3001, '慢病老人', 2, '与老伴同住', '高血压，近期复诊频率较高。', '陈女士', '13900000014', '女儿', 1, DATE_SUB(NOW(3), INTERVAL 6 DAY), 2)
ON DUPLICATE KEY UPDATE
  care_type = VALUES(care_type),
  care_level = VALUES(care_level),
  living_status = VALUES(living_status),
  health_note = VALUES(health_note),
  emergency_contact_name = VALUES(emergency_contact_name),
  emergency_contact_phone = VALUES(emergency_contact_phone),
  emergency_contact_relation = VALUES(emergency_contact_relation),
  monitor_enabled = VALUES(monitor_enabled),
  last_visit_at = VALUES(last_visit_at),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO service_request
  (id, requester_user_id, target_user_id, community_id, title, service_type, description, service_address, contact_phone, expected_time, urgency_level, priority_flag, emergency_contact_name, emergency_contact_phone, emergency_contact_relation, special_tags, status, audit_by_user_id, audit_at, published_at, claimed_at, completed_at)
VALUES
  (8004, 7, 7, 3001, '今晚帮忙代买降压药', '代买取药', '药快吃完了，希望今晚能帮忙去社区药房取药。', '翠苑社区3栋303', '13800000013', DATE_ADD(NOW(3), INTERVAL 4 HOUR), 3, 1, NULL, NULL, NULL, '["代买","慢病家庭"]', 1, 2, NOW(3), NOW(3), NULL, NULL),
  (8005, 8, 8, 3001, '明天上午陪诊复查', '陪诊陪同', '需要有人陪同挂号、看诊和回社区。', '翠苑社区4栋505', '13800000014', DATE_ADD(NOW(3), INTERVAL 1 DAY), 4, 1, '陈女士', '13900000014', '女儿', '["重点关怀","陪诊"]', 2, 2, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 2 HOUR), NULL),
  (8006, 4, 4, 3001, '下午上门助浴', '助浴护理', '洗澡不方便，希望安排有经验的志愿者上门协助。', '翠苑社区2栋202', '13800000012', DATE_ADD(NOW(3), INTERVAL 8 HOUR), 4, 1, '李女士', '13900000012', '女儿', '["独居老人","助浴"]', 3, 2, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 5 HOUR), NULL),
  (8007, 3, 3, 3001, '周末整理厨房', '家务协助', '希望有人帮忙整理厨房并擦拭灶台。', '翠苑社区1栋101', '13800000011', DATE_SUB(NOW(3), INTERVAL 3 DAY), 2, 0, NULL, NULL, NULL, '["家务"]', 5, 2, DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY)),
  (8008, 7, 7, 3001, '咨询老年卡补办流程', '政务代办', '想先了解补办流程和所需材料。', '翠苑社区3栋303', '13800000013', DATE_ADD(NOW(3), INTERVAL 3 DAY), 1, 0, NULL, NULL, NULL, '["政务","咨询"]', 0, NULL, NULL, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  service_type = VALUES(service_type),
  description = VALUES(description),
  service_address = VALUES(service_address),
  contact_phone = VALUES(contact_phone),
  expected_time = VALUES(expected_time),
  urgency_level = VALUES(urgency_level),
  priority_flag = VALUES(priority_flag),
  special_tags = VALUES(special_tags),
  status = VALUES(status),
  audit_by_user_id = VALUES(audit_by_user_id),
  audit_at = VALUES(audit_at),
  published_at = VALUES(published_at),
  claimed_at = VALUES(claimed_at),
  completed_at = VALUES(completed_at),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO service_claim
  (id, request_id, volunteer_user_id, claim_at, claim_status, service_hours, hours_submitted_at, completion_note)
VALUES
  (8103, 8005, 5, DATE_SUB(NOW(3), INTERVAL 2 HOUR), 1, NULL, NULL, NULL),
  (8104, 8006, 9, DATE_SUB(NOW(3), INTERVAL 5 HOUR), 2, 1.50, DATE_SUB(NOW(3), INTERVAL 1 HOUR), '已完成助浴，老人状态稳定。'),
  (8105, 8007, 10, DATE_SUB(NOW(3), INTERVAL 4 DAY), 2, 2.00, DATE_SUB(NOW(3), INTERVAL 3 DAY), '已完成厨房整理，并提醒注意燃气阀门。')
ON DUPLICATE KEY UPDATE
  request_id = VALUES(request_id),
  volunteer_user_id = VALUES(volunteer_user_id),
  claim_status = VALUES(claim_status),
  service_hours = VALUES(service_hours),
  hours_submitted_at = VALUES(hours_submitted_at),
  completion_note = VALUES(completion_note),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO service_order
  (id, request_id, claim_id, volunteer_user_id, community_id, status)
VALUES
  (8203, 8005, 8103, 5, 3001, 2),
  (8204, 8006, 8104, 9, 3001, 3),
  (8205, 8007, 8105, 10, 3001, 5)
ON DUPLICATE KEY UPDATE
  claim_id = VALUES(claim_id),
  volunteer_user_id = VALUES(volunteer_user_id),
  status = VALUES(status),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO service_evaluation
  (id, claim_id, request_id, resident_user_id, volunteer_user_id, evaluator_role, rating, content)
VALUES
  (8302, 8105, 8007, 3, 10, 1, 5, '做事很细致，还顺手帮我把灶台擦干净了。')
ON DUPLICATE KEY UPDATE
  rating = VALUES(rating),
  content = VALUES(content),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO announcement
  (id, title, content_html, content_text, target_scope, target_community_id, status, is_top, top_at, publisher_user_id, published_at)
VALUES
  (7003, '翠苑社区本周陪诊排班提醒', '<p>本周六上午集中安排陪诊服务，请志愿者在前一晚完成报名。</p>', '本周六上午集中安排陪诊服务，请志愿者在前一晚完成报名。', 1, 3001, 1, 0, NULL, 2, NOW(3)),
  (7004, '老年人防诈骗讲座通知', '<p>周四下午两点在社区活动室开展防诈骗讲座。</p>', '周四下午两点在社区活动室开展防诈骗讲座。', 1, 3001, 1, 0, NULL, 2, NOW(3)),
  (7005, '翠苑社区便民服务周安排', '<p>本周将集中提供助浴、陪诊、代买登记服务，居民可在服务站预约。</p>', '本周将集中提供助浴、陪诊、代买登记服务，居民可在服务站预约。', 1, 3001, 1, 1, NOW(3), 2, NOW(3))
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  content_html = VALUES(content_html),
  content_text = VALUES(content_text),
  status = VALUES(status),
  is_top = VALUES(is_top),
  top_at = VALUES(top_at),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO convenience_info
  (id, community_id, category, title, content, contact_phone, address, sort_no, status, created_by)
VALUES
  (7204, 3001, '政务服务', '社区便民代办窗口', '可咨询老年卡补办、医保报销和高龄补贴。', '0571-88886789', '翠苑社区服务站二楼', 4, 1, 2),
  (7205, 3001, '社区食堂', '翠苑长者食堂', '中午和傍晚开放，可电话咨询送餐。', '0571-88887890', '翠苑社区东门南侧', 5, 1, 2),
  (7206, 3001, '志愿服务', '陪诊服务登记台', '工作日上午受理陪诊、助浴和上门探访登记。', '0571-88880001', '翠苑社区服务站一楼', 6, 1, 2)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  content = VALUES(content),
  contact_phone = VALUES(contact_phone),
  address = VALUES(address),
  sort_no = VALUES(sort_no),
  status = VALUES(status),
  updated_at = NOW(3),
  is_deleted = 0;

INSERT INTO sys_user_skill (user_id, skill_tag) VALUES
  (9, '助浴'),
  (9, '护理'),
  (9, '陪伴'),
  (10, '代买取药'),
  (10, '家务协助'),
  (10, '政务代办')
ON DUPLICATE KEY UPDATE created_at = NOW(3);

INSERT INTO service_request_tag (request_id, tag_name) VALUES
  (8004, '代买取药'),
  (8005, '陪诊'),
  (8005, '重点关怀'),
  (8006, '助浴护理'),
  (8007, '家务协助'),
  (8008, '政务咨询')
ON DUPLICATE KEY UPDATE created_at = NOW(3);

INSERT INTO volunteer_match_snapshot
  (id, request_id, volunteer_user_id, total_score, skill_score, area_score, priority_score, rating_score, reason_tags_json)
VALUES
  (8402, 8005, 5, 92.40, 35.00, 25.00, 20.00, 12.40, '["同社区","陪诊经验匹配","历史评分高"]'),
  (8403, 8006, 9, 94.10, 37.00, 24.00, 20.00, 13.10, '["助浴技能匹配","重点需求优先","履约稳定"]'),
  (8404, 8008, 10, 80.60, 29.00, 25.00, 14.00, 12.60, '["可做代办咨询","响应速度快"]')
ON DUPLICATE KEY UPDATE
  total_score = VALUES(total_score),
  skill_score = VALUES(skill_score),
  area_score = VALUES(area_score),
  priority_score = VALUES(priority_score),
  rating_score = VALUES(rating_score),
  reason_tags_json = VALUES(reason_tags_json);

INSERT INTO volunteer_credit_snapshot
  (user_id, total_hours, avg_rating_30d, completion_rate_30d, credit_score)
VALUES
  (9, 24.50, 4.85, 0.91, 87.80),
  (10, 18.00, 4.72, 0.94, 84.60)
ON DUPLICATE KEY UPDATE
  total_hours = VALUES(total_hours),
  avg_rating_30d = VALUES(avg_rating_30d),
  completion_rate_30d = VALUES(completion_rate_30d),
  credit_score = VALUES(credit_score),
  updated_at = NOW(3);

INSERT INTO anomaly_alert_event
  (id, alert_code, community_id, request_id, target_user_id, severity, trigger_rule, suggestion_action, rule_snapshot, dedup_key, status)
VALUES
  (8502, 'URGENT_REQUEST_PENDING', 3001, 8005, 8, 3, '高优先级需求待接单超过2小时', '建议优先推送给陪诊志愿者并电话确认', '{"hours":2}', CONCAT('URGENT_REQUEST_PENDING:8005:', CURDATE()), 0),
  (8503, 'CARE_INACTIVE', 3001, NULL, 8, 2, '重点对象连续3天未登录', '建议社区管理员电话关怀并确认近期复诊安排', '{"days":3}', CONCAT('CARE_INACTIVE:8:', CURDATE()), 0)
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  trigger_rule = VALUES(trigger_rule),
  suggestion_action = VALUES(suggestion_action);

INSERT INTO sys_notification
  (recipient_user_id, title, summary, msg_category, read_status, ref_type, ref_id)
VALUES
  (2, '紧急需求提醒：陪诊单待分配', '翠苑社区有一条高优先级陪诊需求待志愿者响应。', 1, 0, 'REQUEST_ALERT', 8502),
  (2, '关怀预警：重点对象待回访', '陈伯伯近几日未登录，建议尽快电话联系。', 1, 0, 'CARE_ALERT', 8503);
