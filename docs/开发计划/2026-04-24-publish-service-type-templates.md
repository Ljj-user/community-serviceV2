# 发布需求：服务类型限制 + 模板填充 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将“发布需求”的服务类型限制为固定 6 类（存中文），并在移动端选择服务类型后提供模板填充（仅当描述为空时自动填充），为后续志愿者过滤/匹配/异常预警/可视化统一数据口径。

**Architecture:** 前端在 `hall-publish.vue` 用单选展示 6 类，并在切换服务类型时按规则填充描述模板；后端在创建需求入口校验 `serviceType` 是否属于允许集合，非法直接拒绝。

**Tech Stack:** Vue3 + NutUI（移动端），Spring Boot 3（后端），DTO 校验 + Service 校验。

---

### 文件结构（将被修改/新增）

**移动端**
- Modify: `basic-main/apps/example-nut/src/views/hall-publish.vue`（服务类型单选 6 类 + 模板填充）
- (Optional) Add: `basic-main/apps/example-nut/src/utils/serviceTypeTemplates.ts`（模板常量集中，避免组件臃肿）

**后端**
- Modify: `backend/src/main/java/com/community/platform/common/Constants.java`（允许的服务类型常量列表）
- Modify: `backend/src/main/java/com/community/platform/service/impl/ServiceRequestServiceImpl.java`（createRequest 前校验 serviceType）
- (Optional) Modify: `backend/src/main/java/com/community/platform/dto/ServiceRequestCreateDTO.java`（不做复杂枚举校验，保持简单，主要在 Service 校验）

---

### Task 1: 定义允许的服务类型集合（后端常量）

**Files:**
- Modify: `backend/src/main/java/com/community/platform/common/Constants.java`

- [ ] **Step 1: 在 `Constants` 增加允许的 6 类服务类型（存中文）**

新增常量（示例，按项目风格放置）：

```java
public static final List<String> ALLOWED_SERVICE_TYPES = List.of(
    "助老服务（陪护 / 陪诊）",
    "代办服务（买菜 / 取药）",
    "家政清洁",
    "心理陪伴 / 聊天",
    "应急帮助（紧急求助）",
    "社区活动支持"
);
```

- [ ] **Step 2: 编译后端确保无语法问题**

Run: `mvn -q -DskipTests compile` (in `backend/`)
Expected: exit code 0

---

### Task 2: 后端发布需求时校验 serviceType（拒绝非法输入）

**Files:**
- Modify: `backend/src/main/java/com/community/platform/service/impl/ServiceRequestServiceImpl.java`

- [ ] **Step 1: 在 `createRequest(userId, dto)` 入口处校验**

规则：
- `dto.getServiceType()` 必须非空且在 `Constants.ALLOWED_SERVICE_TYPES` 中
- 不满足则 `throw new RuntimeException("服务类型不支持")`

伪代码示例：

```java
String type = dto.getServiceType();
if (!StringUtils.hasText(type) || !Constants.ALLOWED_SERVICE_TYPES.contains(type.trim())) {
    throw new RuntimeException("服务类型不支持");
}
```

- [ ] **Step 2: 编译后端确保无语法问题**

Run: `mvn -q -DskipTests compile`
Expected: exit code 0

---

### Task 3: 移动端发布页改为 6 类单选 + 模板填充（仅当描述为空）

**Files:**
- Modify: `basic-main/apps/example-nut/src/views/hall-publish.vue`
- Optional Add: `basic-main/apps/example-nut/src/utils/serviceTypeTemplates.ts`

- [ ] **Step 1: 将服务类型单选改为 6 类（文本与后端常量一致）**

替换现有 `NutRadioGroup` 的三项为六项，label 为完整中文值（与后端一致）。

- [ ] **Step 2: 加入模板映射（在组件内或抽到 util）**

模板要求：
- 仅做模板文本（不做复杂表单/多级分类/AI 自动生成）
- 切换服务类型时：若 `form.description.trim()` 为空，则填充模板
- “代办服务模板”中的“配送地址”行：将 `form.serviceAddress` 带入（若为空则留空占位）

模板建议（最终可微调）：
- 助老服务（陪护 / 陪诊）：
  - 【服务对象】：老人
  - 【服务内容】：陪同就医 / 日常照料
  - 【时间要求】：具体时间段
  - 【注意事项】：是否行动不便 / 是否需要轮椅
- 代办服务（买菜 / 取药）：
  - 【代办事项】：买菜 / 取药
  - 【物品清单】：（可填写）
  - 【预算范围】：（可选）
  - 【配送地址】：<服务地址>
- 家政清洁：
  - 【清洁范围】：客厅 / 厨房 / 卫生间
  - 【时间要求】：具体时间段
  - 【注意事项】：是否有清洁工具/特殊材质
- 心理陪伴 / 聊天：
  - 【陪伴方式】：线下陪伴 / 电话聊天
  - 【时间要求】：具体时间段
  - 【主题倾向】：日常聊天 / 情绪疏导
- 应急帮助（紧急求助）：
  - 【紧急情况】：摔倒 / 突发不适 / 其他
  - 【需要帮助】：联系家属 / 陪同就医 / 现场协助
  - 【注意事项】：请保持电话畅通
- 社区活动支持：
  - 【活动类型】：秩序维护 / 搬运布置 / 引导签到
  - 【活动时间】：具体时间段
  - 【集合地点】：（可填写）

- [ ] **Step 3: 前端类型检查**

Run: `pnpm -C "basic-main/apps/example-nut" exec vue-tsc --noEmit`
Expected: exit code 0

---

### Task 4: 快速验收（手动）

**目标：确认功能满足“只做单选 + 模板填充 + 基础字段”。**

- [ ] **Step 1: 发布页选择每个服务类型**
  - 描述为空时：自动填模板
  - 描述有内容时：不覆盖
- [ ] **Step 2: 填写地址后选择“代办服务”**
  - 模板中“配送地址”行应包含地址
- [ ] **Step 3: 点击提交发布**
  - 后端返回成功：提示“发布成功，已提交审核”
  - 若将 serviceType 改为非法值（仅开发者手动篡改）：后端返回“服务类型不支持”

