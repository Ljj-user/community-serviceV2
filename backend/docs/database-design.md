# 社区公益服务对接管理平台 - 数据库设计说明

> 数据库名建议：`community_service`  
> 引擎：MySQL 8.0  
> 字符集：`utf8mb4`  
> 当前主 DDL 脚本：`src/main/resources/db/schema_v2_prd.sql`  
> 当前主模拟数据脚本：`src/main/resources/db/temp_data.sql`

## 1. 当前脚本分工

| 文件 | 作用 | 是否主入口 |
|------|------|-----------|
| `src/main/resources/db/schema_v2_prd.sql` | 当前 PRD v2 完整建表脚本 | 是 |
| `src/main/resources/db/temp_data.sql` | 当前模拟数据主入口 | 是 |
| `src/main/resources/db/min_demo_data_v2.sql` | 兼容保留的旧数据脚本 | 否 |
| `src/main/resources/db/init_all.sql` | 本地一键初始化入口，内部调用主脚本 | 是 |
| `src/main/resources/db/init-db.cmd` | Windows 初始化脚本，内部调用 `init_all.sql` | 是 |
| `src/main/resources/db/archive/legacy-sql/schema.sql` | 旧版完整建表脚本，仅保留作历史参考 | 否 |
| `src/main/resources/db/archive/legacy-sql/migration_*.sql` | 旧库增量迁移脚本 | 否 |
| `src/main/resources/db/archive/legacy-sql/rollback_migration_*.sql` | 对应迁移的回滚脚本 | 否 |
| `src/main/resources/db/demo_*.sql` | 现场演示补充数据 | 否 |

## 2. 核心业务表

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户账号、角色、社区绑定、登录信息 |
| `sys_region` | 区域与社区树结构 |
| `community_join_application` | 社区加入申请与审核 |
| `volunteer_profile` | 志愿者认证资料 |
| `care_subject_profile` | 重点关怀对象档案 |
| `service_request` | 公益求助需求 |
| `service_claim` | 志愿者认领与服务执行记录 |
| `service_evaluation` | 服务完成后的评价反馈 |
| `announcement` | 社区公告 |
| `convenience_info` | 便民信息 |
| `anomaly_alert_event` | 异常预警事件 |
| `audit_log` | 后台治理与操作审计 |
| `ai_analysis_record` | AI 分析辅助记录 |

## 3. 关键设计口径

1. `community_id` 是数据隔离主键。用户、需求、公告等数据都围绕社区范围组织。
2. 移动端采用统一账号。一个用户默认是居民，认证后可同时具备志愿者能力。
3. 业务主链路围绕“发布求助 -> 审核 -> 接单 -> 完成 -> 评价”展开。
4. AI 相关表只作为辅助记录，不替代人工审核决策。

## 4. 使用建议

1. 新环境初始化：命令行可直接执行 `init_all.sql`，Windows 环境也可运行 `init-db.cmd`。
2. 需要模拟数据：执行 `temp_data.sql`，或直接跑 `init_all.sql`。
3. 旧迁移脚本只在“历史数据库升级”场景下按需执行，不应再作为新库初始化步骤。
4. 若论文、ER 图、数据库说明与代码不一致，优先以 `schema_v2_prd.sql` 为准。

## 5. 旧文件说明

以下文件仍保留，但不再作为当前系统主入口：

- `archive/legacy-sql/schema.sql`
- `archive/legacy-sql/migration_grid_timebank.sql`
- `archive/legacy-sql/service_request_emergency.sql`
- `archive/legacy-sql/sys_region_province_city.sql`
- `archive/legacy-sql/patch_user_bind_community.sql`
- `archive/legacy-sql/migration_identity_no_dual.sql`

这些文件主要用于旧版本结构升级、兼容过渡或临时修补，后续如继续整理，建议统一归档到历史目录并补充时间说明。
