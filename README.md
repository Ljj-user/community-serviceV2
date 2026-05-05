# 社区公益服务对接管理平台

一个面向社区场景的公益服务平台。

它主要解决三类人的协作问题：

- 居民发布求助
- 志愿者接单服务
- 社区管理员审核与治理

系统主线围绕一条闭环展开：

`发布求助 -> 后台审核 -> 推荐/接单 -> 服务执行 -> 确认完成 -> 评价反馈 -> 数据沉淀 -> 异常预警`

## 项目定位

这个项目不是电商，不是论坛，也不是商业跑腿平台。

当前仓库已经收束到“社区公益服务对接与治理”主线，重点围绕这些能力展开：

- 公益需求发布与审核
- 志愿者认证与服务认领
- 社区绑定与加入审核
- 社区公告与便民信息
- 重点关怀对象管理
- 异常预警与后台治理
- AI 草稿辅助与 AI 分析记录

## 仓库结构

```text
backend/      Spring Boot 后端
frontend/     Vue 3 管理端
basic-main/   Vue 3 + NutUI 移动端
docs/         产品、设计、论文配套资料
毕业论文/      论文修订稿与导出文件
scripts/      启动脚本与辅助脚本
```

## 技术栈

### 后端

- Java 17
- Spring Boot 3
- Spring Security
- MyBatis-Plus
- MySQL 8

### 管理端

- Vue 3
- Vite
- TypeScript
- Naive UI
- Pinia
- ECharts

### 移动端

- Vue 3
- Vite
- TypeScript
- NutUI
- Pinia

## 核心业务规则

### 统一账号

移动端采用统一账号体系。

- 用户默认是居民
- 通过志愿者认证后，可同时具备志愿者能力
- 不拆成“求助者账号”和“志愿者账号”两套体系

### 社区绑定

社区是数据边界核心。

- 用户注册后默认未加入社区
- 可通过邀请码或管理员审核加入社区
- 数据隔离依赖 `community_id`
- 用户只能看自己社区的数据
- 管理员只能管理自己辖区的数据

## 角色说明

### 居民

- 发布求助
- 查看我的求助
- 确认服务完成
- 评价服务
- 查看社区公告和便民信息

### 志愿者

- 提交志愿者认证
- 查看推荐需求
- 接单并更新服务进度
- 查看我的服务记录

### 社区管理员

- 审核求助单
- 审核社区加入申请
- 审核志愿者认证
- 维护重点关怀对象
- 发布公告和便民信息
- 处理异常预警
- 查看 AI 分析记录和统计数据

## 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 22+
- pnpm 10+
- MySQL 8.0

## 快速开始

### 1. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS community_service
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;
```

### 2. 导入表结构

```sql
USE community_service;
SOURCE backend/src/main/resources/db/schema_v2_prd.sql;
```

### 3. 导入演示数据

```sql
USE community_service;
SOURCE backend/src/main/resources/db/min_demo_data_v2.sql;
```

也可以直接运行根目录脚本：

```bat
init-db.cmd
```

### 4. 配置后端数据库

当前开发配置文件为：

`backend/src/main/resources/application-dev.yml`

当前仓库里的开发配置默认指向：

- 数据库地址：`jdbc:mysql://localhost:3306/community_service`
- 用户名：`root`
- 密码：`123456`

如果你本地环境不同，请先修改该文件再启动后端。

### 5. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认地址：`http://localhost:8080`

### 6. 启动管理端

```bash
cd frontend
pnpm install
pnpm dev
```

默认地址：`http://localhost:7000`

### 7. 启动移动端

```bash
cd basic-main
pnpm install
pnpm dev
```

默认地址：`http://localhost:9000`

### 8. 一键启动

Windows 环境下可直接运行：

```bat
start-all.cmd
```

它会分别拉起后端、管理端和移动端。

## 常用文档入口

- [PRD](D:/Coding/毕设/毕设/docs/产品与技术资料/PRD.md)
- [文档索引](D:/Coding/毕设/毕设/docs/README.md)
- [数据库设计](D:/Coding/毕设/毕设/backend/docs/database-design.md)
- [ER 说明](D:/Coding/毕设/毕设/backend/src/main/resources/db/README-ER-notes.md)
- [主库脚本](D:/Coding/毕设/毕设/backend/src/main/resources/db/schema_v2_prd.sql)

## 当前默认地址

- 后端：`http://localhost:8080`
- 管理端：`http://localhost:7000`
- 移动端：`http://localhost:9000`

## 已实现的关键点

### 1. 公益服务闭环

系统不是简单的信息展示，而是完整流程管理：

- 居民发布求助
- 管理员审核
- 志愿者接单或系统推荐
- 服务执行与完成确认
- 评价反馈与数据沉淀

### 2. 社区治理闭环

系统支持社区归属、加入审核和辖区数据隔离：

- 用户加入社区后才能进入完整业务流程
- 管理员围绕本社区进行治理
- 平台重点强调“社区内服务对接”

### 3. 重点关怀与异常预警

系统支持对重点关怀对象进行管理，并通过规则发现异常事件，例如：

- 多日未登录
- 短时间求助异常增长
- 特定对象需要优先关注

### 4. AI 辅助能力

当前 AI 能力用于辅助，不替代人工决策：

- 生成求助草稿
- 生成结构化补充建议
- 记录 AI 分析过程
- 在管理端查看 AI 分析记录

## 常见问题

### 后端连不上数据库

优先检查这三项：

- MySQL 是否已启动
- `community_service` 是否已创建
- `application-dev.yml` 中的连接信息是否和本地一致

### 登录后接口返回 401

通常是下面几种情况：

- 后端没有启动
- 本地 token 已过期
- 前端缓存里还是旧登录态

### 管理端和移动端能否同时使用

可以。

- 管理端主要给社区管理员和系统管理员使用
- 移动端主要给居民和志愿者使用

### 仓库里的数据是否为生产数据

不是。

当前仓库以演示数据、本地开发配置和论文配套资料为主，不包含生产环境凭据。

## 说明

这是一个毕业设计项目仓库。

