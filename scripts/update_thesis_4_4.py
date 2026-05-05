from pathlib import Path
from docx import Document


DOC_PATH = Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1.docx"


def fill(paragraphs, start: int, end: int, lines: list[str]) -> None:
    length = end - start + 1
    data = list(lines) + [""] * max(0, length - len(lines))
    for idx, text in enumerate(data[:length], start):
        paragraphs[idx].text = text


def main() -> None:
    doc = Document(str(DOC_PATH))
    paras = doc.paragraphs

    fill(paras, 276, 318, [
        "（1）页面布局与实现",
        "登录/注册页与社区加入页共同构成移动端的初始入口。登录页负责账号认证，注册页负责采集基础资料与身份信息，社区加入页负责邀请码校验与社区绑定，三者前后衔接后形成完整的首登流程。",
        "登录页主要包含账号密码输入区、忘记密码入口和跳转注册入口，支持用户快速完成身份验证；注册页主要包含身份选择、手机号/邮箱注册切换、基础信息填写和志愿者技能补充，注册成功后可直接进入系统。",
        "社区加入页主要包含邀请码输入、邀请码校验、社区预览和确认加入操作。用户在登录后若尚未绑定社区，系统会优先引导其进入该页面，完成绑定后才能继续访问本社区业务数据。",
        "通过这一设计，系统将“账号建立”和“社区归属”拆分处理，既降低了首次使用门槛，也保证了后续公告、需求和服务数据都能按 community_id 进行隔离。登录/注册页与社区加入页实现效果如图16所示。",
        "",
        "",
        "",
        "",
        "",
        "图 16 登录/注册页与社区加入页实现图",
        "",
        "（2）关键技术与代码",
        "本功能的关键在于“认证状态管理 + 路由守卫引导”。前端将登录态写入 Pinia 和本地缓存，并在路由守卫中判断用户是否已绑定社区；若未绑定，则强制跳转到加入社区页，保证业务数据范围始终正确。",
        "邀请码流程采用“先校验、后绑定”的交互方式。系统先调用邀请码校验接口返回社区预览信息，再由用户确认提交加入请求，成功后刷新当前用户资料并跳回业务首页。",
        "关键代码如下（节选）：",
        "// 路由守卫：未绑定社区时强制进入加入社区页",
        "const allow = new Set(['login', 'register', 'join-community', 'scan', 'reload'])",
        "if (!allow.has(String(to.name)) && !appAuthStore.user?.communityId) {",
        "  return { name: 'join-community' }",
        "}",
        "",
        "// 加入社区页：先校验邀请码，再正式提交绑定",
        "const res = await apiApp.verifyInviteCode({ code: v })",
        "preview.value = { communityId: res.data.communityId, communityName: res.data.communityName }",
        "const joinRes = await apiApp.joinCommunity({ code: v })",
        "if (joinRes.code === 200) {",
        "  await appAuthStore.hydrateUser()",
        "  router.replace('/hall')",
        "}",
    ])

    fill(paras, 321, 342, [
        "（1）页面布局与实现",
        "移动端首页承担平台导览与快速分流作用，页面由顶部社区栏、轮播区、公告提示、快捷功能卡片和 AI 输入入口组成。用户进入首页后，可先查看当前绑定社区、浏览公告，再根据需要进入“我要帮忙”“我有难处”“志愿者认证”或“便民信息”等功能。",
        "首页将社区公告放入轮播图下方的醒目区域，并提供消息入口与切换社区入口，既满足居民查看信息的需求，也方便志愿者快速进入接单或认证流程。移动端首页实现效果如图17所示。",
        "",
        "图 17 移动端首页实现图",
        "",
        "（2）关键技术与代码",
        "该页面的关键实现是“多接口并发加载 + 兜底渲染”。页面初始化时同时请求轮播图和公告列表，若轮播数据为空则回退到默认展示素材，避免首页因单一接口异常而出现空白。",
        "首页还通过统一入口函数完成社区切换、公告详情和 AI 助手跳转，使导航逻辑集中在页面层处理，便于后续维护。",
        "关键代码如下（节选）：",
        "async function loadData() {",
        "  await appAuthStore.hydrateUserThrottled?.()",
        "  const [bannerResult, annResult] = await Promise.allSettled([",
        "    listBanners(),",
        "    listUserAnnouncements(1, 30),",
        "  ])",
        "  bannerList.value = mapped.length ? mapped : bannerFallback",
        "  announcementRows.value = annResult.value.data.records || []",
        "}",
        "",
        "function onChangeCommunity() {",
        "  router.push('/join-community')",
        "}",
    ])

    fill(paras, 344, 359, [
        "（1）页面布局与实现",
        "服务大厅与需求发布页共同构成移动端的业务主入口。服务大厅页整合“正在进行的帮助”和“正在进行的求助”两类视图，用户可在同一页面查看任务状态、进入详情弹窗、切换筛选条件，并跳转到发布页或接单页。",
        "需求发布页则面向求助居民，页面由服务类型选择、紧急程度选择、服务地址、需求描述和联系人信息等表单区域组成，用户填写完成后即可提交需求进入审核流程。服务大厅与需求发布页实现效果如图18所示。",
        "图 18 服务大厅与需求发布实现图",
        "",
        "（2）关键技术与代码",
        "本部分的关键实现包括“状态驱动列表切换”和“结构化表单提交”。服务大厅通过 activeTab 和计算属性复用一套任务卡片模板；需求发布页则通过 reactive 管理表单状态，并在提交时统一组装参数请求后端接口。",
        "关键代码如下（节选）：",
        "const activeTab = ref<'joined' | 'published'>('joined')",
        "const feedRows = computed(() =>",
        "  activeTab.value === 'joined' ? joinedRows.value : publishedRows.value",
        ")",
        "function openListTab(tab: 'joined' | 'published') {",
        "  activeTab.value = tab",
        "}",
        "const res = await createServiceRequest({ ...formPayload })",
        "if (res.code === 200) router.back()",
    ])

    fill(paras, 361, 390, [
        "（1）页面布局与实现",
        "“我的需求、我的服务与评价反馈”对应居民端和志愿者端的过程追踪页面。我的需求页展示本人已发布求助的审核状态、认领状态和完成状态；我的服务页展示本人已认领任务的执行情况；评价反馈页则在服务完成后提供打分与文字反馈入口。",
        "用户在这些页面中可以继续查看任务详情、确认服务完成、进入评价页面或再次发起求助，从而形成一条清晰的个人业务闭环。我的需求、我的服务与评价反馈实现效果如图19所示。",
        "图 19 我的需求、我的服务与评价反馈实现图",
        "（2）关键技术与代码",
        "该部分的关键实现是“公共任务状态管理 + 评价链路联动”。我的需求页和我的服务页通过 useHallTaskCenter 复用筛选、详情弹窗和状态刷新逻辑；在居民确认完成后，页面会把 claimId 带入评价页，保证评价对象准确无误。",
        "关键代码如下（节选）：",
        "loadData('published')",
        "watch([publishedStatusFilter, publishedSortFilter], () => {",
        "  loadData('published')",
        "})",
        "",
        "// 服务评价提交",
        "const res = await api.post('/service-evaluation', {",
        "  claimId: claimId.value,",
        "  rating: Number(rating.value),",
        "  content: content.value.trim(),",
        "})",
        "if (res.code !== 200) throw new Error(res.message || '提交失败')",
        "router.replace({ path: '/hall-overview', query: { kind: 'reviews' } })",
        "",
        "// 居民确认完成后跳转到评价页",
        "const confirmRes = await confirmClaimService({ claimId })",
        "if (confirmRes.code === 200) {",
        "  router.push({ path: '/service-evaluate', query: { claimId: String(claimId) } })",
        "}",
    ])

    fill(paras, 393, 416, [
        "（1）页面布局与实现",
        "志愿者认证页用于补充志愿服务能力信息，页面主要由认证状态卡片、技能标签区、证件与服务半径信息区、可服务时间区以及提交按钮组成。用户可查看当前认证状态，选择擅长技能并填写服务范围后提交审核。",
        "当用户尚未加入社区时，页面会先提示完成社区绑定；只有社区归属明确后，认证资料才会进入对应社区管理员的审核范围。志愿者认证实现效果如图20所示。",
        "",
        "图 20 志愿者认证实现图",
        "（2）关键技术与代码",
        "本页面的关键实现是“认证状态驱动 + 技能标签归并”。系统先拉取当前认证档案并回填已提交数据，再把预设技能和自定义技能合并后提交，既保留统一标签结构，也兼顾个性化服务能力填写。",
        "关键代码如下（节选）：",
        "const canApply = computed(() => !!appAuthStore.user?.communityId)",
        "if (!canApply.value) {",
        "  router.push('/join-community')",
        "  return",
        "}",
        "const skills = mergedSkills()",
        "await applyVolunteerProfile({",
        "  skillTags: skills,",
        "  serviceRadiusKm: Number(form.serviceRadiusKm || 3),",
        "  availableTime: form.availableTime.trim(),",
        "})",
        "await load()",
        "",
        "const tags = parseSkillTags(res.data.skillTags)",
        "selectedSkills.value = tags.filter(x => skillTemplates.includes(x))",
        "customSkill.value = tags.find(x => !skillTemplates.includes(x)) || ''",
    ])

    fill(paras, 419, 428, [
        "（1）页面布局与实现",
        "社区公告与便民信息页用于承接社区公开信息展示。公告页面向居民展示本社区已发布通知，支持点击查看详情；便民信息页集中展示药店、医院、服务热线和地址说明等常用服务内容，方便居民在求助之外快速获取社区生活信息。",
        "两类页面都以列表化阅读为主，强调“按社区查看、按内容直达”，既减轻了首页信息压力，也让居民在日常使用中更容易找到实用信息。社区公告与便民信息实现效果如图21所示。",
        "",
        "图 21 社区公告与便民信息实现图",
        "",
        "（2）关键技术与代码",
        "本部分的关键实现是“社区范围过滤 + 详情弹窗读取”。首页和公告页统一调用公告列表接口读取当前社区公告，点击某条公告后再请求详情接口展示正文内容；便民信息页则按社区返回标题、电话、地址和说明字段。",
        "const res = await listUserAnnouncements(1, 30)",
        "const detailRes = await getUserAnnouncementDetail(n.id)",
    ])

    fill(paras, 431, 447, [
        "（1）页面布局与实现",
        "AI 助手页用于辅助居民整理需求描述。页面采用对话式布局，包含聊天记录区、快捷提问区和底部输入框；当用户输入自然语言需求后，系统会返回结构化建议，并生成可带入发布页的需求草稿卡片。",
        "用户既可以直接把 AI 返回的草稿继续编辑后提交，也可以仅将其作为描述参考，从而降低首次发布求助时的填写难度。AI 助手页实现效果如图22所示。",
        "",
        "图 22 AI 助手页实现图",
        "（2）关键技术与代码",
        "该页面的关键实现是“上下文对话 + 草稿持久化”。前端会截取最近几轮消息作为上下文发送到 aiChat 接口；当接口返回 orderDraft 后，页面把草稿和分析记录号写入本地缓存，再跳转到需求发布页继续完善。",
        "关键代码如下（节选）：",
        "const res = await aiChat(text, history)",
        "chatRows.value.push({",
        "  role: 'ai',",
        "  text: res.data?.reply || '已收到',",
        "  draft: res.data?.orderDraft,",
        "  analysisRecordId: res.data?.analysisRecordId,",
        "})",
        "saveAiDemandDraft({ analysisRecordId: row.analysisRecordId, inputText: row.sourceText, reply: row.text, draft: row.draft })",
        "router.push('/hall-publish')",
    ])

    fill(paras, 450, 467, [
        "",
        "（1）页面布局与实现",
        "管理端数据看板页用于集中展示平台运行总体情况。页面通过统计卡片、趋势图表和风险提示区域呈现用户规模、需求状态、志愿者活跃度、重点关怀数量和异常预警数量等关键指标，便于管理人员快速掌握平台运行态势。",
        "系统还根据当前登录角色自动切换数据范围：社区管理员仅查看本社区数据，超级管理员则查看全平台汇总数据。管理端数据看板实现效果如图26所示。",
        "",
        "图 26 管理端数据看板实现图",
        "",
        "（2）关键技术与代码",
        "看板页的关键实现是“角色范围控制 + 聚合接口输出”。后端在进入统计逻辑前先判断当前用户角色，再按是否需要限定 scopeCommunityId 聚合对应范围内的数据，减少前端多次拼装数据的复杂度。",
        "关键代码如下（节选）：",
        "@GetMapping(\"/stats\")",
        "@PreAuthorize(\"hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')\")",
        "public Result<DashboardStatsVO> getStats() {",
        "    SysUser current = getCurrentUser();",
        "    Long scopeCommunityId = isCommunityAdmin(current) ? current.getCommunityId() : null;",
        "    DashboardStatsVO stats = dashboardService.getStats(scopeCommunityId);",
        "    return Result.success(stats);",
        "}",
    ])

    fill(paras, 470, 486, [
        "（1）页面布局与实现",
        "社区加入审核、需求审核与志愿者审核页共同承担后台业务把关功能。社区加入审核页用于确认居民的社区归属申请；需求审核页用于决定服务需求是否通过并进入可接单状态；志愿者审核页用于核验申请人的技能与服务范围信息。",
        "三类审核页面均采用“列表筛选 + 详情预览 + 审核操作”的结构，管理员可先查看申请内容，再执行通过或驳回，保证关键业务节点都有人工确认。社区加入审核、需求审核与志愿者审核页实现效果如图27所示。",
        "",
        "图 27 社区加入审核、需求审核与志愿者审核实现图",
        "",
        "（2）关键技术与代码",
        "该部分的关键实现是“标准化审核接口 + 权限约束”。不同审核页面虽然业务对象不同，但都遵循列表查询、详情预览、审核提交这一统一交互逻辑，同时通过权限控制限制为管理员角色访问。",
        "关键代码如下（节选）：",
        "const joinRes = await communityJoinList({ status: query.status ?? undefined, page: query.page, size: query.size })",
        "const reqRes = await serviceRequestAudit({ requestId: row.id, approved: true })",
        "const volRes = await approveVolunteerProfile(row.id)",
        "if (reqRes.code === 200 || volRes.code === 200) {",
        "  message.success('审核结果已更新')",
        "  load()",
        "}",
    ])

    fill(paras, 489, 512, [
        "（1）页面布局与实现",
        "重点关怀对象与异常预警页用于支撑社区主动治理。重点关怀对象页主要维护独居老人、残障居民等重点对象的基础档案、关怀等级、紧急联系人和监测开关；异常预警页则按规则展示超时未认领、超时未完成等风险任务，方便管理员及时跟进。",
        "前者强调“对象建档与长期关怀”，后者强调“过程监测与及时处置”，两页联动后可把静态人群档案和动态服务风险结合起来。重点关怀对象与异常预警页实现效果如图28所示。",
        "",
        "图 28 重点关怀对象与异常预警实现图",
        "",
        "（2）关键技术与代码",
        "本部分的关键实现是“重点对象资料维护 + 风险分页监测”。重点关怀对象页通过表单保存 careType、careLevel、monitorEnabled 等字段；异常预警页则按风险类型和社区分页查询，并提供详情查看和催办提醒操作。",
        "关键代码如下（节选）：",
        "const saveRes = await saveCareSubject({",
        "  userId: form.userId, careType: form.careType, careLevel: form.careLevel, monitorEnabled: form.monitorEnabled,",
        "})",
        "const resp = await serviceMonitorList({",
        "  current: pagination.page,",
        "  size: pagination.pageSize,",
        "  riskType: filters.riskType,",
        "  communityId: filters.communityId ?? undefined,",
        "})",
        "tableData.value = resp.data.records || []",
        "await httpClient.post(`/service-claim/remind/${row.requestId}`)",
        "",
        "",
        "",
        "",
    ])

    fill(paras, 515, 537, [
        "（1）页面布局与实现",
        "用户管理与审计日志页分别承担“账号治理”和“操作留痕”两类后台管理职责。用户管理页支持按用户名、角色、状态和所属社区进行筛选，并提供新增、编辑、启用/禁用等操作；审计日志页用于记录关键模块的操作时间、操作者、结果和风险等级，便于后续追溯。",
        "两类页面共同构成平台的运维支撑能力，一方面保证人员与权限信息可维护，另一方面保证关键操作有记录可查。用户管理与审计日志页实现效果如图29所示。",
        "",
        "图 29 用户管理与审计日志页实现图",
        "",
        "（2）关键技术与代码",
        "该部分的关键实现是“分页筛选查询 + 明细追踪”。用户管理页把查询条件与分页参数统一传给列表接口，并在操作完成后刷新表格；审计日志页则支持按模块、结果和风险等级过滤，并可打开详情查看单条日志的请求路径、执行耗时和结果信息。",
        "关键代码如下（节选）：",
        "const res = await adminUserList({",
        "  username: query.username || undefined, role: query.role ?? undefined, status: query.status ?? undefined,",
        "  communityId: query.communityId ?? undefined, page: query.page, size: query.size,",
        "})",
        "rows.value = res.data.records || []",
        "const next = row.status === 1 ? 0 : 1",
        "await adminUserSetStatus(row.id, next as 0 | 1)",
        "const auditRes = await auditLogList({ username: query.username || undefined, module: query.module || undefined, riskLevel: query.riskLevel ?? undefined, page: query.page, size: query.size })",
        "const detailRes = await auditLogDetail(row.id)",
        "detailRow.value = detailRes.data",
        "",
        "",
        "",
    ])

    doc.save(str(DOC_PATH))


if __name__ == "__main__":
    main()
