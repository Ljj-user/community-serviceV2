# 数据库说明与 ER 资料

当前完整表结构脚本：`schema_v2_prd.sql`

用于整理数据库设计、绘制 ER 图的说明文档位于：
`backend/docs/database-design.md`

常用文件如下：

| 文件 | 说明 |
|------|------|
| `schema_v2_prd.sql` | 当前 PRD v2 完整建表脚本 |
| `temp_data.sql` | 当前模拟数据主入口 |
| `min_demo_data_v2.sql` | 兼容保留的旧数据脚本 |
| `init_all.sql` | 一键初始化入口 |
| `init-db.cmd` | Windows 一键初始化脚本 |
| `README-usage.md` | DB 目录脚本分工说明 |

如果论文、ER 图、旧说明文档和代码不一致，优先以当前 `schema_v2_prd.sql` 为准。
