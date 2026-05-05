package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.platform.common.Constants;
import com.community.platform.common.ServiceRequestStateMachine;
import com.community.platform.dto.ServiceRequestAuditDTO;
import com.community.platform.dto.ServiceRequestCreateDTO;
import com.community.platform.dto.ServiceRequestQueryDTO;
import com.community.platform.dto.ServiceRequestVO;
import com.community.platform.dto.ServiceMonitorQueryDTO;
import com.community.platform.dto.ServiceMonitorVO;
import com.community.platform.dto.MatchExplainVO;
import com.community.platform.generated.entity.ServiceClaim;
import com.community.platform.generated.entity.ServiceEvaluation;
import com.community.platform.generated.entity.ServiceRequest;
import com.community.platform.generated.entity.SysRegion;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.ServiceClaimMapper;
import com.community.platform.generated.mapper.ServiceEvaluationMapper;
import com.community.platform.generated.mapper.ServiceRequestMapper;
import com.community.platform.generated.mapper.SysRegionMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.service.ServiceRequestService;
import com.community.platform.service.UserNotificationService;
import com.community.platform.util.SubsidyUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 需求管理服务实现
 */
@Service
public class ServiceRequestServiceImpl extends ServiceImpl<ServiceRequestMapper, ServiceRequest> implements ServiceRequestService {
    
    @Autowired
    private ServiceRequestMapper serviceRequestMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRegionMapper sysRegionMapper;

    @Autowired
    private ServiceClaimMapper serviceClaimMapper;

    @Autowired
    private ServiceEvaluationMapper serviceEvaluationMapper;

    @Autowired
    private UserNotificationService userNotificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AiChatService aiChatService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    @Transactional
    public ServiceRequestVO createRequest(Long userId, ServiceRequestCreateDTO dto) {
        if (dto == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        // 普通用户（role=3）均可发布需求（全能互助）；但必须绑定社区，并满足时间币规则
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        if (!Constants.ROLE_NORMAL_USER.equals(user.getRole())) {
            throw new RuntimeException("只有普通用户可以发布需求");
        }
        if (user.getCommunityId() == null) {
            throw new RuntimeException("请先在个人资料中选择所属社区");
        }
        String serviceType = dto.getServiceType() == null ? null : dto.getServiceType().trim();
        if (!StringUtils.hasText(serviceType) || !Constants.ALLOWED_SERVICE_TYPES.contains(serviceType)) {
            throw new RuntimeException("服务类型不支持");
        }

        boolean subsidized = SubsidyUtil.isSubsidized(user.getIdentityTag());
        long coins = user.getTimeCoins() == null ? 0 : user.getTimeCoins();
        // 发布需求时做简单余额校验：余额需 >0；更精确的扣费在“核销确认”阶段完成
        if (!subsidized && coins <= 0) {
            throw new RuntimeException("时间币余额不足，无法发布需求");
        }
        
        // 创建需求记录
        ServiceRequest request = new ServiceRequest();
        request.setRequesterUserId(userId);
        request.setCommunityId(user.getCommunityId());
        request.setServiceType(serviceType);
        request.setDescription(dto.getDescription());
        request.setServiceAddress(dto.getServiceAddress());
        request.setExpectedTime(dto.getExpectedTime());
        request.setUrgencyLevel(dto.getUrgencyLevel() != null ? dto.getUrgencyLevel() : (byte) 2);  // 默认中等紧急程度
        request.setEmergencyContactName(dto.getEmergencyContactName());
        request.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        request.setEmergencyContactRelation(dto.getEmergencyContactRelation());
        request.setStatus(Constants.REQUEST_STATUS_PENDING);  // 待审核
        
        // 处理特殊标签（转为JSON字符串）
        if (dto.getSpecialTags() != null && !dto.getSpecialTags().isEmpty()) {
            try {
                request.setSpecialTags(objectMapper.writeValueAsString(dto.getSpecialTags()));
            } catch (Exception e) {
                throw new RuntimeException("特殊标签格式错误", e);
            }
        }
        
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        request.setIsDeleted((byte) 0);
        
        serviceRequestMapper.insert(request);
        aiChatService.markSubmitted(userId, dto.getAiAnalysisRecordId());
        
        return convertToVO(request);
    }
    
    @Override
    @Transactional
    public void auditRequest(Long auditorId, ServiceRequestAuditDTO dto) {
        // 验证审核人身份（必须是社区管理员或超级管理员）
        SysUser auditor = sysUserMapper.selectById(auditorId);
        if (auditor == null || auditor.getIsDeleted() == 1) {
            throw new RuntimeException("审核人不存在");
        }
        
        if (auditor.getRole() != Constants.ROLE_COMMUNITY_ADMIN && auditor.getRole() != Constants.ROLE_SUPER_ADMIN) {
            throw new RuntimeException("只有社区管理员可以审核需求");
        }
        
        // 查询需求
        ServiceRequest request = serviceRequestMapper.selectById(dto.getRequestId());
        if (request == null || request.getIsDeleted() == 1) {
            throw new RuntimeException("需求不存在");
        }
        
        if (request.getStatus() != Constants.REQUEST_STATUS_PENDING) {
            throw new RuntimeException("该需求已审核，无法重复审核");
        }

        if (auditor.getRole() == Constants.ROLE_COMMUNITY_ADMIN
                && (auditor.getCommunityId() == null || !auditor.getCommunityId().equals(request.getCommunityId()))) {
            throw new RuntimeException("仅可审核本社区需求");
        }
        
        // 更新审核信息
        request.setAuditByUserId(auditorId);
        request.setAuditAt(LocalDateTime.now());
        
        if (dto.getApproved()) {
            // 审核通过：状态改为已发布
            ServiceRequestStateMachine.assertTransition(request.getStatus(), Constants.REQUEST_STATUS_PUBLISHED);
            request.setStatus(Constants.REQUEST_STATUS_PUBLISHED);
            request.setPublishedAt(LocalDateTime.now());
            request.setRejectReason(null);
        } else {
            // 审核驳回：状态改为已驳回
            ServiceRequestStateMachine.assertTransition(request.getStatus(), Constants.REQUEST_STATUS_REJECTED);
            request.setStatus(Constants.REQUEST_STATUS_REJECTED);
            if (!StringUtils.hasText(dto.getRejectReason())) {
                throw new RuntimeException("驳回时必须填写驳回原因");
            }
            request.setRejectReason(dto.getRejectReason());
        }
        
        request.setUpdatedAt(LocalDateTime.now());
        serviceRequestMapper.updateById(request);

        userNotificationService.notifyRequestAudited(request, dto.getApproved(), dto.getRejectReason());
    }
    
    @Override
    public IPage<ServiceRequestVO> listRequests(ServiceRequestQueryDTO queryDTO, Long currentUserId) {
        // 构建查询条件
        LambdaQueryWrapper<ServiceRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceRequest::getIsDeleted, 0);
        boolean strictHallMode = false;
        
        if (queryDTO.getStatus() != null) {
            wrapper.eq(ServiceRequest::getStatus, queryDTO.getStatus());
        }
        
        if (StringUtils.hasText(queryDTO.getServiceType())) {
            wrapper.eq(ServiceRequest::getServiceType, queryDTO.getServiceType());
        }
        
        if (queryDTO.getUrgencyLevel() != null) {
            wrapper.eq(ServiceRequest::getUrgencyLevel, queryDTO.getUrgencyLevel());
        }
        if (queryDTO.getCommunityId() != null) {
            wrapper.eq(ServiceRequest::getCommunityId, queryDTO.getCommunityId());
        }
        
        // 普通用户：需求池默认只展示同社区（网格化隔离） + 自己发布的需求
        if (currentUserId != null) {
            SysUser user = sysUserMapper.selectById(currentUserId);
            if (user != null && user.getRole() == Constants.ROLE_NORMAL_USER) {
                Long communityId = user.getCommunityId();
                List<Long> sameCommunityUserIds = List.of();
                if (communityId != null) {
                    LambdaQueryWrapper<SysUser> uw = new LambdaQueryWrapper<>();
                    uw.eq(SysUser::getIsDeleted, 0).eq(SysUser::getRole, Constants.ROLE_NORMAL_USER).eq(SysUser::getCommunityId, communityId);
                    sameCommunityUserIds = sysUserMapper.selectList(uw).stream().map(SysUser::getId).toList();
                }

                if (queryDTO.getStatus() != null && queryDTO.getStatus().equals(Constants.REQUEST_STATUS_PUBLISHED)) {
                    strictHallMode = true;
                    wrapper.eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_PUBLISHED);
                    wrapper.ne(ServiceRequest::getRequesterUserId, currentUserId);
                    if (communityId != null) {
                        wrapper.eq(ServiceRequest::getCommunityId, communityId);
                    } else {
                        wrapper.eq(ServiceRequest::getRequesterUserId, -1L);
                    }
                } else {
                    List<Long> finalSameCommunityUserIds = sameCommunityUserIds;
                    wrapper.and(w -> w.eq(ServiceRequest::getRequesterUserId, currentUserId)
                            .or(x -> x.eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_PUBLISHED)
                                    .in(finalSameCommunityUserIds != null && !finalSameCommunityUserIds.isEmpty(), ServiceRequest::getRequesterUserId, finalSameCommunityUserIds)));
                }
            } else if (user != null && user.getRole() == Constants.ROLE_COMMUNITY_ADMIN) {
                wrapper.eq(ServiceRequest::getCommunityId, user.getCommunityId());
            }
        }

        if (strictHallMode && currentUserId != null) {
            wrapper.orderByDesc(ServiceRequest::getUrgencyLevel)
                    .orderByDesc(ServiceRequest::getPublishedAt)
                    .orderByDesc(ServiceRequest::getCreatedAt);
            List<ServiceRequest> allRows = serviceRequestMapper.selectList(wrapper);
            List<ServiceRequestVO> allVos = allRows.stream().map(this::convertToVO).collect(Collectors.toList());
            fillUserInfo(allVos);
            applyHallRuleMatching(allVos, currentUserId);
            allVos.sort(Comparator
                    .comparing((ServiceRequestVO x) -> x.getUrgencyLevel() == null ? 0 : x.getUrgencyLevel(), Comparator.reverseOrder())
                    .thenComparing((ServiceRequestVO x) -> x.getPublishedAt() == null ? x.getCreatedAt() : x.getPublishedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing((ServiceRequestVO x) -> isSkillMatched(x), Comparator.reverseOrder())
                    .thenComparing(ServiceRequestVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

            long total = allVos.size();
            int current = queryDTO.getCurrent() == null ? 1 : queryDTO.getCurrent();
            int size = queryDTO.getSize() == null ? 10 : queryDTO.getSize();
            int from = Math.max(0, (current - 1) * size);
            int to = Math.min(allVos.size(), from + size);
            List<ServiceRequestVO> pageRows = from >= to ? List.of() : allVos.subList(from, to);
            Page<ServiceRequestVO> voPage = new Page<>(current, size, total);
            voPage.setRecords(pageRows);
            return voPage;
        }

        applyMyRequestOrder(wrapper, queryDTO);
        Page<ServiceRequest> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        IPage<ServiceRequest> requestPage = serviceRequestMapper.selectPage(page, wrapper);
        List<ServiceRequestVO> voList = requestPage.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        fillUserInfo(voList);
        if (currentUserId != null) {
            fillMatchExplain(voList, currentUserId);
        }
        Page<ServiceRequestVO> voPage = new Page<>(requestPage.getCurrent(), requestPage.getSize(), requestPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private void applyMyRequestOrder(LambdaQueryWrapper<ServiceRequest> wrapper, ServiceRequestQueryDTO queryDTO) {
        String sortBy = queryDTO.getSortBy() == null ? "" : queryDTO.getSortBy().trim();
        String sortOrder = queryDTO.getSortOrder() == null ? "desc" : queryDTO.getSortOrder().trim().toLowerCase();
        boolean asc = "asc".equals(sortOrder);

        switch (sortBy) {
            case "expectedTime" -> {
                wrapper.orderBy(true, asc, ServiceRequest::getExpectedTime);
                wrapper.orderByDesc(ServiceRequest::getCreatedAt);
            }
            case "urgencyLevel" -> {
                wrapper.orderBy(true, asc, ServiceRequest::getUrgencyLevel);
                wrapper.orderByDesc(ServiceRequest::getCreatedAt);
            }
            case "publishedAt" -> {
                wrapper.orderBy(true, asc, ServiceRequest::getPublishedAt);
                wrapper.orderByDesc(ServiceRequest::getCreatedAt);
            }
            default -> wrapper.orderBy(true, asc, ServiceRequest::getCreatedAt);
        }
    }

    @Override
    public IPage<ServiceRequestVO> listMyRequests(ServiceRequestQueryDTO queryDTO, Long currentUserId) {
        if (currentUserId == null) {
            throw new RuntimeException("未登录");
        }

        // 全能互助：普通用户均可查看“我的需求进度”
        SysUser user = sysUserMapper.selectById(currentUserId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getRole() != Constants.ROLE_NORMAL_USER) {
            throw new RuntimeException("只有普通用户可以查看我的需求");
        }

        LambdaQueryWrapper<ServiceRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceRequest::getIsDeleted, 0);
        wrapper.eq(ServiceRequest::getRequesterUserId, currentUserId);

        if (queryDTO.getStatus() != null) {
            wrapper.eq(ServiceRequest::getStatus, queryDTO.getStatus());
        }
        if (StringUtils.hasText(queryDTO.getServiceType())) {
            wrapper.eq(ServiceRequest::getServiceType, queryDTO.getServiceType());
        }
        if (queryDTO.getUrgencyLevel() != null) {
            wrapper.eq(ServiceRequest::getUrgencyLevel, queryDTO.getUrgencyLevel());
        }

        wrapper.orderByDesc(ServiceRequest::getCreatedAt);

        Page<ServiceRequest> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        IPage<ServiceRequest> requestPage = serviceRequestMapper.selectPage(page, wrapper);

        List<ServiceRequestVO> voList = requestPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        fillUserInfo(voList);
        fillCommunityInfo(voList);
        fillLatestClaimInfo(voList);

        Page<ServiceRequestVO> voPage = new Page<>(requestPage.getCurrent(), requestPage.getSize(), requestPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
    
    @Override
    public ServiceRequestVO getRequestDetail(Long requestId, Long currentUserId) {
        if (currentUserId == null) {
            throw new RuntimeException("未登录");
        }
        ServiceRequest request = serviceRequestMapper.selectById(requestId);
        if (request == null || request.getIsDeleted() == 1) {
            throw new RuntimeException("需求不存在");
        }
        assertCanViewRequestDetail(request, currentUserId);
        
        ServiceRequestVO vo = convertToVO(request);
        fillUserInfo(List.of(vo));
        fillCommunityInfo(List.of(vo));
        fillLatestClaimInfo(List.of(vo));
        return vo;
    }

    @Override
    public IPage<ServiceMonitorVO> listMonitor(ServiceMonitorQueryDTO queryDTO, Long currentUserId) {
        LocalDateTime now = LocalDateTime.now();
        SysUser currentUser = currentUserId == null ? null : sysUserMapper.selectById(currentUserId);

        LambdaQueryWrapper<ServiceRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceRequest::getIsDeleted, 0);

        // 只关注已发布/已认领的需求
        wrapper.in(ServiceRequest::getStatus,
                Constants.REQUEST_STATUS_PUBLISHED,
                Constants.REQUEST_STATUS_CLAIMED,
                Constants.REQUEST_STATUS_PENDING_CONFIRM);

        // 超时规则：expected_time 早于当前时间
        wrapper.lt(ServiceRequest::getExpectedTime, now);

        // 风险类型过滤
        if (queryDTO.getRiskType() != null) {
            if (queryDTO.getRiskType() == 1) {
                // 超时未认领：已发布且尚未被认领
                wrapper.eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_PUBLISHED);
            } else if (queryDTO.getRiskType() == 2) {
                // 超时未完成：已认领但未完成
                wrapper.in(ServiceRequest::getStatus, Constants.REQUEST_STATUS_CLAIMED, Constants.REQUEST_STATUS_PENDING_CONFIRM);
            }
        }
        if (queryDTO.getCommunityId() != null) {
            wrapper.eq(ServiceRequest::getCommunityId, queryDTO.getCommunityId());
        }
        if (currentUser != null && currentUser.getRole() != null
                && currentUser.getRole().equals(Constants.ROLE_COMMUNITY_ADMIN)) {
            if (currentUser.getCommunityId() == null) {
                throw new RuntimeException("管理员未绑定社区，无法查看监控数据");
            }
            wrapper.eq(ServiceRequest::getCommunityId, currentUser.getCommunityId());
        }

        Page<ServiceRequest> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        IPage<ServiceRequest> requestPage = serviceRequestMapper.selectPage(page, wrapper);

        List<ServiceRequest> requests = requestPage.getRecords();
        List<ServiceMonitorVO> voList = new ArrayList<>();

        if (!requests.isEmpty()) {
            // 填充需求发起人姓名
            List<ServiceRequestVO> requestVOs = requests.stream().map(this::convertToVO).toList();
            fillUserInfo(requestVOs);
            Map<Long, String> requesterNameMap = requestVOs.stream()
                    .collect(Collectors.toMap(ServiceRequestVO::getId, ServiceRequestVO::getRequesterName));

            // 查询认领记录
            List<Long> requestIds = requests.stream().map(ServiceRequest::getId).toList();
            List<ServiceClaim> claims = serviceClaimMapper.selectList(
                    new LambdaQueryWrapper<ServiceClaim>()
                            .in(ServiceClaim::getRequestId, requestIds)
                            .eq(ServiceClaim::getIsDeleted, 0)
            );

            Map<Long, ServiceClaim> claimMap = claims.stream()
                    .collect(Collectors.toMap(ServiceClaim::getRequestId, c -> c, (c1, c2) -> c1));

            // 查询评价（按需求维度取一条最新评价）
            List<ServiceEvaluation> evaluations = serviceEvaluationMapper.selectList(
                    new LambdaQueryWrapper<ServiceEvaluation>()
                            .in(ServiceEvaluation::getRequestId, requestIds)
                            .eq(ServiceEvaluation::getIsDeleted, 0)
            );
            Map<Long, ServiceEvaluation> evaluationMap = evaluations.stream()
                    .collect(Collectors.toMap(ServiceEvaluation::getRequestId, e -> e, (e1, e2) -> e1));
            Map<Long, AlertMeta> alertMetaMap = loadAlertMeta(requestIds);

            // 批量查询志愿者姓名
            List<Long> volunteerIds = claims.stream()
                    .map(ServiceClaim::getVolunteerUserId)
                    .distinct()
                    .toList();
            Map<Long, SysUser> volunteerMap = new java.util.HashMap<>();
            if (!volunteerIds.isEmpty()) {
                List<SysUser> volunteers = sysUserMapper.selectBatchIds(volunteerIds);
                volunteerMap = volunteers.stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));
            }

            for (ServiceRequest request : requests) {
                ServiceMonitorVO vo = new ServiceMonitorVO();
                vo.setRequestId(request.getId());
                vo.setStatus(request.getStatus());
                vo.setServiceType(request.getServiceType());
                vo.setCommunityId(request.getCommunityId());
                vo.setServiceAddress(request.getServiceAddress());
                vo.setExpectedTime(request.getExpectedTime());
                vo.setUrgencyLevel(request.getUrgencyLevel());
                vo.setRequesterName(requesterNameMap.get(request.getId()));
                vo.setClaimedAt(request.getClaimedAt());
                vo.setCompletedAt(request.getCompletedAt());

                long minutes = Duration.between(request.getExpectedTime(), now).toMinutes();
                vo.setOvertimeMinutes(Math.max(minutes, 0L));

                ServiceClaim claim = claimMap.get(request.getId());
                if (claim != null) {
                    vo.setClaimId(claim.getId());
                    vo.setClaimStatus(claim.getClaimStatus());
                    SysUser volunteer = volunteerMap.get(claim.getVolunteerUserId());
                    if (volunteer != null) {
                        vo.setVolunteerName(volunteer.getRealName());
                    }
                }

                ServiceEvaluation evaluation = evaluationMap.get(request.getId());
                if (evaluation != null) {
                    vo.setRating(evaluation.getRating());
                }

                // 风险类型：根据状态推断
                if (request.getStatus() == Constants.REQUEST_STATUS_PUBLISHED) {
                    vo.setRiskType(1);
                    vo.setAlertSource("TIMEOUT_UNCLAIMED");
                    vo.setTriggerRule("期望服务时间已过且需求仍未被认领");
                    vo.setSuggestionAction("建议优先提醒志愿者认领，必要时协调社区管理员人工派单");
                } else if (request.getStatus() == Constants.REQUEST_STATUS_CLAIMED) {
                    vo.setRiskType(2);
                    vo.setAlertSource("TIMEOUT_INCOMPLETE");
                    vo.setTriggerRule("期望服务时间已过且服务仍未完成");
                    vo.setSuggestionAction("建议联系志愿者确认进展，并评估是否需要转单或干预");
                } else if (request.getStatus() == Constants.REQUEST_STATUS_PENDING_CONFIRM) {
                    vo.setRiskType(2);
                    vo.setAlertSource("PENDING_CONFIRM");
                    vo.setTriggerRule("志愿者已提交完成信息，等待需求方确认（超时将自动完成）");
                    vo.setSuggestionAction("建议提醒需求方尽快确认完成或申诉，避免长期滞留");
                }

                AlertMeta alertMeta = alertMetaMap.get(request.getId());
                if (alertMeta != null) {
                    if (alertMeta.alertCode != null && !alertMeta.alertCode.isBlank()) {
                        vo.setAlertSource(alertMeta.alertCode);
                    }
                    if (alertMeta.triggerRule != null && !alertMeta.triggerRule.isBlank()) {
                        vo.setTriggerRule(alertMeta.triggerRule);
                    }
                    if (alertMeta.suggestionAction != null && !alertMeta.suggestionAction.isBlank()) {
                        vo.setSuggestionAction(alertMeta.suggestionAction);
                    }
                }

                voList.add(vo);
            }
            List<Long> communityIds = requests.stream()
                    .map(ServiceRequest::getCommunityId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            Map<Long, SysRegion> regionMap = communityIds.isEmpty()
                    ? Map.of()
                    : sysRegionMapper.selectBatchIds(communityIds).stream()
                    .collect(Collectors.toMap(SysRegion::getId, r -> r, (a, b) -> a));
            for (ServiceMonitorVO vo : voList) {
                if (vo.getCommunityId() != null) {
                    SysRegion r = regionMap.get(vo.getCommunityId());
                    if (r != null) {
                        vo.setCommunityName(r.getName());
                    }
                }
            }
        }

        Page<ServiceMonitorVO> voPage = new Page<>(requestPage.getCurrent(), requestPage.getSize(), requestPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private Map<Long, AlertMeta> loadAlertMeta(List<Long> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            return Map.of();
        }
        String inSql = requestIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT request_id, alert_code, trigger_rule, suggestion_action " +
                        "FROM anomaly_alert_event WHERE request_id IS NOT NULL AND request_id IN (" + inSql + ") " +
                        "ORDER BY id DESC");
        Map<Long, AlertMeta> out = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            Long requestId = row.get("request_id") instanceof Number n ? n.longValue() : null;
            if (requestId == null || out.containsKey(requestId)) {
                continue;
            }
            out.put(requestId, new AlertMeta(
                    row.get("alert_code") == null ? null : String.valueOf(row.get("alert_code")),
                    row.get("trigger_rule") == null ? null : String.valueOf(row.get("trigger_rule")),
                    row.get("suggestion_action") == null ? null : String.valueOf(row.get("suggestion_action"))
            ));
        }
        return out;
    }

    private record AlertMeta(String alertCode, String triggerRule, String suggestionAction) {}
    
    /**
     * 转换为VO
     */
    private ServiceRequestVO convertToVO(ServiceRequest request) {
        ServiceRequestVO vo = new ServiceRequestVO();
        BeanUtils.copyProperties(request, vo);
        
        // 解析特殊标签JSON
        if (StringUtils.hasText(request.getSpecialTags())) {
            try {
                List<String> tags = objectMapper.readValue(request.getSpecialTags(), new TypeReference<List<String>>() {});
                vo.setSpecialTags(tags);
            } catch (Exception e) {
                vo.setSpecialTags(new ArrayList<>());
            }
        }
        
        return vo;
    }

    /**
     * 填充社区名称
     */
    private void fillCommunityInfo(List<ServiceRequestVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        List<Long> ids = voList.stream()
                .map(ServiceRequestVO::getCommunityId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, SysRegion> regionMap = sysRegionMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysRegion::getId, r -> r, (a, b) -> a));
        for (ServiceRequestVO vo : voList) {
            if (vo.getCommunityId() != null) {
                SysRegion r = regionMap.get(vo.getCommunityId());
                if (r != null) {
                    vo.setCommunityName(r.getName());
                    vo.setProvince(r.getProvince());
                    vo.setCity(r.getCity());
                }
            }
        }
    }
    
    /**
     * 填充用户信息（需求发起人、审核人）
     */
    private void fillUserInfo(List<ServiceRequestVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        
        // 收集所有用户ID
        List<Long> userIds = new ArrayList<>();
        for (ServiceRequestVO vo : voList) {
            if (vo.getRequesterUserId() != null) {
                userIds.add(vo.getRequesterUserId());
            }
            if (vo.getAuditByUserId() != null) {
                userIds.add(vo.getAuditByUserId());
            }
        }
        
        if (userIds.isEmpty()) {
            return;
        }
        
        // 批量查询用户
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        Map<Long, SysUser> userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
        
        // 填充用户姓名
        for (ServiceRequestVO vo : voList) {
            if (vo.getRequesterUserId() != null) {
                SysUser requester = userMap.get(vo.getRequesterUserId());
                if (requester != null) {
                    vo.setRequesterName(requester.getRealName());
                }
            }
            if (vo.getAuditByUserId() != null) {
                SysUser auditor = userMap.get(vo.getAuditByUserId());
                if (auditor != null) {
                    vo.setAuditorName(auditor.getRealName());
                }
            }
        }
    }

    /**
     * 填充最新认领记录（claimId/claimStatus），便于移动端做“确认完成/评价”动作。
     */
    private void fillLatestClaimInfo(List<ServiceRequestVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        List<Long> requestIds = voList.stream()
                .map(ServiceRequestVO::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (requestIds.isEmpty()) {
            return;
        }
        List<ServiceClaim> claims = serviceClaimMapper.selectList(
                new LambdaQueryWrapper<ServiceClaim>()
                        .in(ServiceClaim::getRequestId, requestIds)
                        .eq(ServiceClaim::getIsDeleted, 0)
                        .orderByDesc(ServiceClaim::getId)
        );
        Map<Long, ServiceClaim> latest = claims.stream()
                .filter(c -> c.getRequestId() != null)
                .collect(Collectors.toMap(ServiceClaim::getRequestId, c -> c, (a, b) -> a));
        for (ServiceRequestVO vo : voList) {
            ServiceClaim c = vo.getId() == null ? null : latest.get(vo.getId());
            if (c != null) {
                vo.setLatestClaimId(c.getId());
                vo.setLatestClaimStatus(c.getClaimStatus());
                if (c.getVolunteerUserId() != null) {
                    SysUser volunteer = sysUserMapper.selectById(c.getVolunteerUserId());
                    if (volunteer != null) {
                        vo.setLatestVolunteerName(volunteer.getRealName());
                    }
                }
            }
        }
    }

    private void assertCanViewRequestDetail(ServiceRequest request, Long currentUserId) {
        SysUser currentUser = sysUserMapper.selectById(currentUserId);
        if (currentUser == null || currentUser.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }

        if (Constants.ROLE_SUPER_ADMIN.equals(currentUser.getRole())) {
            return;
        }

        if (Constants.ROLE_COMMUNITY_ADMIN.equals(currentUser.getRole())) {
            if (currentUser.getCommunityId() != null && currentUser.getCommunityId().equals(request.getCommunityId())) {
                return;
            }
            throw new RuntimeException("无权查看该需求");
        }

        if (!Constants.ROLE_NORMAL_USER.equals(currentUser.getRole())) {
            throw new RuntimeException("无权查看该需求");
        }

        if (currentUserId.equals(request.getRequesterUserId())) {
            return;
        }

        boolean isVolunteerParticipant = serviceClaimMapper.selectCount(
                new LambdaQueryWrapper<ServiceClaim>()
                        .eq(ServiceClaim::getRequestId, request.getId())
                        .eq(ServiceClaim::getVolunteerUserId, currentUserId)
                        .eq(ServiceClaim::getIsDeleted, 0)
        ) > 0;
        if (isVolunteerParticipant) {
            return;
        }

        if (request.getStatus() == Constants.REQUEST_STATUS_PUBLISHED
                && currentUser.getCommunityId() != null
                && currentUser.getCommunityId().equals(request.getCommunityId())) {
            return;
        }

        throw new RuntimeException("无权查看该需求");
    }

    private void fillMatchExplain(List<ServiceRequestVO> voList, Long currentUserId) {
        if (voList == null || voList.isEmpty() || currentUserId == null) {
            return;
        }
        SysUser volunteer = sysUserMapper.selectById(currentUserId);
        if (volunteer == null || volunteer.getIsDeleted() == 1 || volunteer.getRole() != Constants.ROLE_NORMAL_USER) {
            return;
        }
        double volunteerRatingScore = calcVolunteerRatingScore(currentUserId);
        Set<String> volunteerSkills = loadVolunteerSkills(currentUserId, volunteer.getSkillTags());
        for (ServiceRequestVO vo : voList) {
            MatchExplainVO explain = new MatchExplainVO();
            List<String> requestTags = loadRequestTags(vo.getId(), vo.getSpecialTags());
            double skillScore = calcSkillBinaryScore(volunteerSkills, requestTags, vo.getServiceType());
            double areaScore = calcAreaScore(volunteer.getCommunityId(), vo.getCommunityId());
            double priorityScore = calcPriorityScore(vo.getUrgencyLevel());
            double ratingScore = volunteerRatingScore;
            double totalScore = 0.5D * skillScore + 0.3D * areaScore + 0.1D * priorityScore + 0.1D * ratingScore;
            explain.setSkillScore(round1(skillScore));
            explain.setAreaScore(round1(areaScore));
            explain.setPriorityScore(round1(priorityScore));
            explain.setRatingScore(round1(ratingScore));
            explain.setTotalScore(round1(totalScore));
            vo.setMatchExplain(explain);
            vo.setMatchReasons(buildMatchReasons(skillScore, areaScore, priorityScore, ratingScore));
        }
    }

    /**
     * 志愿者接单广场：三层规则（硬过滤已在 SQL，标签匹配 + 排序在内存）
     */
    private void applyHallRuleMatching(List<ServiceRequestVO> voList, Long currentUserId) {
        if (voList == null || voList.isEmpty() || currentUserId == null) {
            return;
        }
        SysUser volunteer = sysUserMapper.selectById(currentUserId);
        if (volunteer == null || volunteer.getIsDeleted() == 1 || volunteer.getRole() != Constants.ROLE_NORMAL_USER) {
            return;
        }
        Set<String> volunteerSkills = loadVolunteerSkills(currentUserId, volunteer.getSkillTags());
        for (ServiceRequestVO vo : voList) {
            List<String> requestTags = loadRequestTags(vo.getId(), vo.getSpecialTags());
            boolean skillMatched = isSkillTagMatched(volunteerSkills, requestTags, vo.getServiceType());
            MatchExplainVO explain = new MatchExplainVO();
            explain.setSkillScore(skillMatched ? 100D : 0D);
            explain.setAreaScore(100D); // 同社区已在第一层硬过滤
            explain.setPriorityScore(calcPriorityScore(vo.getUrgencyLevel()));
            explain.setRatingScore(0D);
            explain.setTotalScore(skillMatched ? 100D : 0D);
            explain.setW1(1D);
            explain.setW2(0D);
            explain.setW3(0D);
            explain.setW4(0D);
            vo.setMatchExplain(explain);
            vo.setMatchReasons(skillMatched
                    ? List.of("技能标签匹配")
                    : List.of("技能标签暂不匹配"));
        }
    }

    private Set<String> loadVolunteerSkills(Long userId, String fallbackJson) {
        try {
            List<String> tags = jdbcTemplate.query(
                    "SELECT skill_tag FROM sys_user_skill WHERE user_id=?",
                    (rs, rowNum) -> rs.getString("skill_tag"),
                    userId
            );
            if (tags != null && !tags.isEmpty()) {
                return tags.stream().filter(StringUtils::hasText).map(String::trim).collect(Collectors.toSet());
            }
        } catch (Exception ignored) {
            // fallback to json
        }
        return parseTags(fallbackJson);
    }

    private List<String> loadRequestTags(Long requestId, List<String> fallback) {
        if (requestId == null) {
            return fallback == null ? List.of() : fallback;
        }
        try {
            List<String> tags = jdbcTemplate.query(
                    "SELECT tag_name FROM service_request_tag WHERE request_id=?",
                    (rs, rowNum) -> rs.getString("tag_name"),
                    requestId
            );
            if (tags != null && !tags.isEmpty()) {
                return tags.stream().filter(StringUtils::hasText).map(String::trim).toList();
            }
        } catch (Exception ignored) {
        }
        return fallback == null ? List.of() : fallback;
    }

    private double calcVolunteerRatingScore(Long volunteerUserId) {
        List<ServiceEvaluation> evaluations = serviceEvaluationMapper.selectList(new LambdaQueryWrapper<ServiceEvaluation>()
                .eq(ServiceEvaluation::getVolunteerUserId, volunteerUserId)
                .eq(ServiceEvaluation::getIsDeleted, 0));
        if (evaluations.isEmpty()) {
            return 70D;
        }
        double avg = evaluations.stream().map(ServiceEvaluation::getRating).filter(java.util.Objects::nonNull)
                .mapToInt(Byte::intValue).average().orElse(3.5D);
        return Math.max(0D, Math.min(100D, avg * 20D));
    }

    private Set<String> parseTags(String rawTags) {
        if (!StringUtils.hasText(rawTags)) {
            return Set.of();
        }
        try {
            List<String> tags = objectMapper.readValue(rawTags, new TypeReference<List<String>>() {});
            return tags.stream().filter(StringUtils::hasText).map(String::trim).collect(Collectors.toSet());
        } catch (Exception ignored) {
            return java.util.Arrays.stream(rawTags.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        }
    }

    private double calcSkillScore(Set<String> volunteerSkills, List<String> requestTags) {
        if (requestTags == null || requestTags.isEmpty()) {
            return 60D;
        }
        if (volunteerSkills == null || volunteerSkills.isEmpty()) {
            return calcColdStartSkillScore(requestTags);
        }
        Set<String> req = requestTags.stream().filter(StringUtils::hasText).map(String::trim).collect(Collectors.toSet());
        if (req.isEmpty()) {
            return 60D;
        }
        Set<String> hit = new HashSet<>(req);
        hit.retainAll(volunteerSkills);
        return Math.min(100D, (hit.size() * 1.0D / req.size()) * 100D);
    }

    private double calcSkillBinaryScore(Set<String> volunteerSkills, List<String> requestTags, String serviceType) {
        Set<String> req = new HashSet<>();
        if (requestTags != null) {
            req.addAll(requestTags.stream().filter(StringUtils::hasText).map(String::trim).collect(Collectors.toSet()));
        }
        if (StringUtils.hasText(serviceType)) {
            req.add(serviceType.trim());
        }
        if (req.isEmpty() || volunteerSkills == null || volunteerSkills.isEmpty()) {
            return 0D;
        }
        boolean matched = req.stream().anyMatch(volunteerSkills::contains);
        return matched ? 100D : 0D;
    }

    private boolean isSkillTagMatched(Set<String> volunteerSkills, List<String> requestTags, String serviceType) {
        return calcSkillBinaryScore(volunteerSkills, requestTags, serviceType) >= 99.9D;
    }

    private double calcColdStartSkillScore(List<String> requestTags) {
        if (requestTags == null || requestTags.isEmpty()) {
            return 50D;
        }
        List<String> req = requestTags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (req.isEmpty()) {
            return 50D;
        }
        try {
            Integer maxCount = jdbcTemplate.queryForObject("SELECT MAX(user_count) FROM skill_tag_stat", Integer.class);
            int max = maxCount == null ? 0 : maxCount;
            if (max <= 0) {
                return 45D;
            }
            double ratioSum = 0D;
            for (String tag : req) {
                Integer c = jdbcTemplate.queryForObject(
                        "SELECT user_count FROM skill_tag_stat WHERE skill_tag=?",
                        Integer.class,
                        tag
                );
                int count = c == null ? 0 : c;
                ratioSum += (count * 1.0D / max);
            }
            double avgRatio = ratioSum / req.size();
            // 冷启动区间映射：至少给 40 分，避免新用户永远排在末尾
            return Math.max(40D, Math.min(80D, 40D + avgRatio * 40D));
        } catch (Exception ignored) {
            return 45D;
        }
    }

    private double calcAreaScore(Long volunteerCommunityId, Long requestCommunityId) {
        if (volunteerCommunityId == null || requestCommunityId == null) {
            return 40D;
        }
        return volunteerCommunityId.equals(requestCommunityId) ? 100D : 20D;
    }

    private double calcPriorityScore(Byte urgencyLevel) {
        if (urgencyLevel == null) {
            return 60D;
        }
        return switch (urgencyLevel) {
            case 4 -> 100D;
            case 3 -> 85D;
            case 2 -> 70D;
            default -> 50D;
        };
    }

    private List<String> buildMatchReasons(double skillScore, double areaScore, double priorityScore, double ratingScore) {
        List<String> reasons = new ArrayList<>();
        if (skillScore >= 80D) reasons.add("技能高度匹配");
        else if (skillScore >= 50D) reasons.add("技能部分匹配");
        if (areaScore >= 80D) reasons.add("同社区就近服务");
        if (priorityScore >= 85D) reasons.add("高优先级需求");
        if (ratingScore >= 80D) reasons.add("历史服务评价较好");
        if (reasons.isEmpty()) reasons.add("综合评分推荐");
        return reasons;
    }

    private double round1(double val) {
        return Math.round(val * 10.0D) / 10.0D;
    }

    private boolean isSkillMatched(ServiceRequestVO vo) {
        MatchExplainVO explain = vo == null ? null : vo.getMatchExplain();
        if (explain == null || explain.getSkillScore() == null) {
            return false;
        }
        return explain.getSkillScore() >= 99.9D;
    }
}
