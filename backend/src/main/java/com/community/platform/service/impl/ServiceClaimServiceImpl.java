package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.Constants;
import com.community.platform.common.ServiceRequestStateMachine;
import com.community.platform.dto.ServiceClaimDTO;
import com.community.platform.dto.ServiceClaimVO;
import com.community.platform.dto.ServiceCompleteDTO;
import com.community.platform.dto.ServiceDisputeDTO;
import com.community.platform.generated.entity.ServiceClaim;
import com.community.platform.generated.entity.ServiceOrder;
import com.community.platform.generated.entity.ServiceRequest;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.ServiceClaimMapper;
import com.community.platform.generated.mapper.ServiceOrderMapper;
import com.community.platform.generated.mapper.ServiceRequestMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.generated.mapper.TimeTransactionMapper;
import com.community.platform.service.ServiceClaimService;
import com.community.platform.service.UserNotificationService;
import com.community.platform.util.SubsidyUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 服务认领服务实现
 */
@Service
public class ServiceClaimServiceImpl implements ServiceClaimService {
    
    @Autowired
    private ServiceClaimMapper serviceClaimMapper;
    
    @Autowired
    private ServiceRequestMapper serviceRequestMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ServiceOrderMapper serviceOrderMapper;

    @Autowired
    private TimeTransactionMapper timeTransactionMapper;

    @Autowired
    private UserNotificationService userNotificationService;

    @Autowired
    private VolunteerCreditService volunteerCreditService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void claimService(Long volunteerId, ServiceClaimDTO dto) {
        // 验证志愿者身份
        SysUser volunteer = sysUserMapper.selectById(volunteerId);
        if (volunteer == null || volunteer.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        if (volunteer.getRole() != Constants.ROLE_NORMAL_USER) {
            throw new RuntimeException("只有普通用户可以认领服务");
        }
        Integer certified = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM volunteer_profile WHERE user_id=? AND cert_status=2",
                Integer.class, volunteerId
        );
        if (certified == null || certified <= 0) {
            throw new RuntimeException("请先完成志愿者认证后再接单");
        }
        if (volunteer.getCommunityId() == null) {
            throw new RuntimeException("请先加入社区后再认领服务");
        }
        
        // 查询需求
        ServiceRequest request = serviceRequestMapper.selectById(dto.getRequestId());
        if (request == null || request.getIsDeleted() == 1) {
            throw new RuntimeException("需求不存在");
        }
        
        if (request.getStatus() != Constants.REQUEST_STATUS_PUBLISHED) {
            throw new RuntimeException("只能认领已发布的需求");
        }

        // 网格化隔离：仅允许同社区流转
        SysUser requester = sysUserMapper.selectById(request.getRequesterUserId());
        if (request.getCommunityId() == null) {
            throw new RuntimeException("该需求未绑定社区，无法认领");
        }
        if (!request.getCommunityId().equals(volunteer.getCommunityId())) {
            throw new RuntimeException("仅允许认领同社区需求");
        }
        
        // 检查是否已被认领
        ServiceClaim existingClaim = serviceClaimMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ServiceClaim>()
                        .eq(ServiceClaim::getRequestId, dto.getRequestId())
                        .eq(ServiceClaim::getVolunteerUserId, volunteerId)
                        .eq(ServiceClaim::getIsDeleted, 0)
                        .in(ServiceClaim::getClaimStatus, Constants.CLAIM_STATUS_CLAIMED, Constants.CLAIM_STATUS_COMPLETED, Constants.CLAIM_STATUS_PENDING_CONFIRM)
                        .last("LIMIT 1")
        );
        
        if (existingClaim != null) {
            throw new RuntimeException("您已认领过该需求");
        }
        
        // 检查需求是否已被其他志愿者认领
        ServiceClaim otherClaim = serviceClaimMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ServiceClaim>()
                        .eq(ServiceClaim::getRequestId, dto.getRequestId())
                        .ne(ServiceClaim::getVolunteerUserId, volunteerId)
                        .eq(ServiceClaim::getIsDeleted, 0)
                        .in(ServiceClaim::getClaimStatus, Constants.CLAIM_STATUS_CLAIMED, Constants.CLAIM_STATUS_COMPLETED, Constants.CLAIM_STATUS_PENDING_CONFIRM)
                        .last("LIMIT 1")
        );
        
        if (otherClaim != null) {
            throw new RuntimeException("该需求已被其他志愿者认领");
        }
        
        // 创建认领记录
        ServiceClaim claim = new ServiceClaim();
        claim.setRequestId(dto.getRequestId());
        claim.setVolunteerUserId(volunteerId);
        claim.setClaimAt(LocalDateTime.now());
        claim.setClaimStatus(Constants.CLAIM_STATUS_CLAIMED);  // 已认领
        claim.setCreatedAt(LocalDateTime.now());
        claim.setUpdatedAt(LocalDateTime.now());
        claim.setIsDeleted((byte) 0);
        
        serviceClaimMapper.insert(claim);
        
        // 更新需求状态为已认领
        ServiceRequestStateMachine.assertTransition(request.getStatus(), Constants.REQUEST_STATUS_CLAIMED);
        request.setStatus(Constants.REQUEST_STATUS_CLAIMED);
        request.setClaimedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        serviceRequestMapper.updateById(request);

        userNotificationService.notifyServiceClaimed(request, claim);

        // 创建/更新 service_order（用于时间银行闭环）
        LambdaQueryWrapper<ServiceOrder> ow = new LambdaQueryWrapper<>();
        ow.eq(ServiceOrder::getRequestId, request.getId()).eq(ServiceOrder::getIsDeleted, 0).last("LIMIT 1");
        ServiceOrder order = serviceOrderMapper.selectOne(ow);
        if (order == null) {
            order = new ServiceOrder();
            order.setRequestId(request.getId());
            order.setVolunteerUserId(volunteerId);
            order.setCommunityId(requester != null ? requester.getCommunityId() : null);
            order.setStatus((byte) 1); // 1=已认领
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setIsDeleted((byte) 0);
            serviceOrderMapper.insert(order);
        } else {
            order.setVolunteerUserId(volunteerId);
            order.setStatus((byte) 1);
            order.setUpdatedAt(LocalDateTime.now());
            serviceOrderMapper.updateById(order);
        }
    }
    
    @Override
    @Transactional
    public void completeService(Long volunteerId, ServiceCompleteDTO dto) {
        // 查询认领记录
        ServiceClaim claim = serviceClaimMapper.selectById(dto.getClaimId());
        if (claim == null || claim.getIsDeleted() == 1) {
            throw new RuntimeException("认领记录不存在");
        }
        
        // 验证是否为该志愿者的认领记录
        if (!claim.getVolunteerUserId().equals(volunteerId)) {
            throw new RuntimeException("无权操作该认领记录");
        }
        
        if (claim.getClaimStatus() != Constants.CLAIM_STATUS_CLAIMED) {
            throw new RuntimeException("只能完成已认领的服务");
        }
        
        // 更新认领记录为“待确认”
        claim.setClaimStatus(Constants.CLAIM_STATUS_PENDING_CONFIRM);
        claim.setServiceHours(dto.getServiceHours());
        claim.setHoursSubmittedAt(LocalDateTime.now());
        claim.setCompletionNote(dto.getCompletionNote());
        claim.setUpdatedAt(LocalDateTime.now());
        
        serviceClaimMapper.updateById(claim);
        
        // 需求进入“待确认”，等待需求方确认或超时自动完成
        ServiceRequest request = serviceRequestMapper.selectById(claim.getRequestId());
        if (request != null && request.getIsDeleted() == 0) {
            ServiceRequestStateMachine.assertTransition(request.getStatus(), Constants.REQUEST_STATUS_PENDING_CONFIRM);
            request.setStatus(Constants.REQUEST_STATUS_PENDING_CONFIRM);
            request.setUpdatedAt(LocalDateTime.now());
            serviceRequestMapper.updateById(request);
        }

        // 标记订单进入“已提交时长”状态（待核销）
        if (request != null) {
            LambdaQueryWrapper<ServiceOrder> ow = new LambdaQueryWrapper<>();
            ow.eq(ServiceOrder::getRequestId, request.getId()).eq(ServiceOrder::getIsDeleted, 0).last("LIMIT 1");
            ServiceOrder order = serviceOrderMapper.selectOne(ow);
            if (order != null) {
                order.setStatus((byte) 2); // 2=已提交时长
                order.setUpdatedAt(LocalDateTime.now());
                serviceOrderMapper.updateById(order);
            }
        }
    }

    @Override
    @Transactional
    public void confirmService(Long requesterUserId, com.community.platform.dto.ServiceConfirmDTO dto) {
        ServiceClaim claim = serviceClaimMapper.selectById(dto.getClaimId());
        if (claim == null || claim.getIsDeleted() == 1) {
            throw new RuntimeException("认领记录不存在");
        }
        if (claim.getClaimStatus() == null || !claim.getClaimStatus().equals(Constants.CLAIM_STATUS_PENDING_CONFIRM)) {
            throw new RuntimeException("只能确认待确认的服务");
        }
        if (claim.getServiceHours() == null) {
            throw new RuntimeException("服务时长为空，无法核销");
        }

        ServiceRequest request = serviceRequestMapper.selectById(claim.getRequestId());
        if (request == null || request.getIsDeleted() == 1) {
            throw new RuntimeException("需求不存在");
        }
        if (!request.getRequesterUserId().equals(requesterUserId)) {
            throw new RuntimeException("无权核销该需求");
        }

        settleAndFinalize(request, claim);
    }

    @Override
    @Transactional
    public void disputeService(Long requesterUserId, ServiceDisputeDTO dto) {
        ServiceClaim claim = serviceClaimMapper.selectById(dto.getClaimId());
        if (claim == null || claim.getIsDeleted() == 1) {
            throw new RuntimeException("认领记录不存在");
        }
        if (!Set.of(Constants.CLAIM_STATUS_PENDING_CONFIRM, Constants.CLAIM_STATUS_CLAIMED).contains(claim.getClaimStatus())) {
            throw new RuntimeException("当前状态不可申诉");
        }
        ServiceRequest request = serviceRequestMapper.selectById(claim.getRequestId());
        if (request == null || request.getIsDeleted() == 1) {
            throw new RuntimeException("需求不存在");
        }
        if (!request.getRequesterUserId().equals(requesterUserId)) {
            throw new RuntimeException("无权申诉该需求");
        }
        claim.setClaimStatus(Constants.CLAIM_STATUS_DISPUTED);
        claim.setCompletionNote(String.format("【申诉】%s", dto.getReason().trim()));
        claim.setUpdatedAt(LocalDateTime.now());
        serviceClaimMapper.updateById(claim);
    }

    @Override
    @Transactional
    public void disputeByRequest(Long requesterUserId, Long requestId, String reason) {
        ServiceRequest request = serviceRequestMapper.selectById(requestId);
        if (request == null || request.getIsDeleted() == 1) {
            throw new RuntimeException("需求不存在");
        }
        if (!request.getRequesterUserId().equals(requesterUserId)) {
            throw new RuntimeException("无权申诉该需求");
        }
        ServiceClaim claim = serviceClaimMapper.selectOne(new LambdaQueryWrapper<ServiceClaim>()
                .eq(ServiceClaim::getRequestId, requestId)
                .eq(ServiceClaim::getIsDeleted, 0)
                .orderByDesc(ServiceClaim::getId)
                .last("LIMIT 1"));
        if (claim == null) {
            throw new RuntimeException("未找到关联认领记录");
        }
        ServiceDisputeDTO dto = new ServiceDisputeDTO();
        dto.setClaimId(claim.getId());
        dto.setReason(reason);
        disputeService(requesterUserId, dto);
    }

    @Transactional
    public int autoCompletePendingClaims(int timeoutHours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(timeoutHours);
        List<ServiceClaim> pendingClaims = serviceClaimMapper.selectList(
                new LambdaQueryWrapper<ServiceClaim>()
                        .eq(ServiceClaim::getIsDeleted, 0)
                        .eq(ServiceClaim::getClaimStatus, Constants.CLAIM_STATUS_PENDING_CONFIRM)
                        .le(ServiceClaim::getHoursSubmittedAt, cutoff)
        );
        int success = 0;
        for (ServiceClaim claim : pendingClaims) {
            ServiceRequest request = serviceRequestMapper.selectById(claim.getRequestId());
            if (request == null || request.getIsDeleted() == 1) {
                continue;
            }
            try {
                settleAndFinalize(request, claim);
                success++;
            } catch (Exception ignored) {
                // 失败时保留“待确认”，等待管理员干预
            }
        }
        return success;
    }

    private void settleAndFinalize(ServiceRequest request, ServiceClaim claim) {
        SysUser requester = sysUserMapper.selectById(request.getRequesterUserId());
        SysUser volunteer = sysUserMapper.selectById(claim.getVolunteerUserId());
        if (requester == null || volunteer == null) {
            throw new RuntimeException("用户不存在");
        }

        long coins = calcSettlementCoins(claim.getServiceHours());
        if (coins <= 0) {
            throw new RuntimeException("核销时长无效");
        }

        boolean subsidized = SubsidyUtil.isSubsidized(requester.getIdentityTag());
        long requesterCoins = requester.getTimeCoins() == null ? 0 : requester.getTimeCoins();
        if (!subsidized && requesterCoins < coins) {
            throw new RuntimeException("时间币余额不足，无法核销");
        }

        LambdaQueryWrapper<ServiceOrder> ow = new LambdaQueryWrapper<>();
        ow.eq(ServiceOrder::getRequestId, request.getId()).eq(ServiceOrder::getIsDeleted, 0).last("LIMIT 1");
        ServiceOrder order = serviceOrderMapper.selectOne(ow);
        if (order == null) {
            order = new ServiceOrder();
            order.setRequestId(request.getId());
            order.setVolunteerUserId(volunteer.getId());
            order.setCommunityId(requester.getCommunityId());
            order.setStatus((byte) 2);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setIsDeleted((byte) 0);
            serviceOrderMapper.insert(order);
        }

        volunteer.setTimeCoins((volunteer.getTimeCoins() == null ? 0 : volunteer.getTimeCoins()) + coins);
        sysUserMapper.updateById(volunteer);

        if (!subsidized) {
            requester.setTimeCoins(requesterCoins - coins);
            sysUserMapper.updateById(requester);
        }

        com.community.platform.generated.entity.TimeTransaction tVol = new com.community.platform.generated.entity.TimeTransaction();
        tVol.setUserId(volunteer.getId());
        tVol.setAmount(coins);
        tVol.setType((byte) 1);
        tVol.setOrderId(order.getId());
        tVol.setCreateTime(LocalDateTime.now());
        timeTransactionMapper.insert(tVol);

        com.community.platform.generated.entity.TimeTransaction tReq = new com.community.platform.generated.entity.TimeTransaction();
        tReq.setUserId(requester.getId());
        if (subsidized) {
            tReq.setAmount(coins);
            tReq.setType((byte) 3);
        } else {
            tReq.setAmount(-coins);
            tReq.setType((byte) 2);
        }
        tReq.setOrderId(order.getId());
        tReq.setCreateTime(LocalDateTime.now());
        timeTransactionMapper.insert(tReq);

        order.setStatus((byte) 3);
        order.setUpdatedAt(LocalDateTime.now());
        serviceOrderMapper.updateById(order);

        claim.setClaimStatus(Constants.CLAIM_STATUS_COMPLETED);
        claim.setUpdatedAt(LocalDateTime.now());
        serviceClaimMapper.updateById(claim);

        ServiceRequestStateMachine.assertTransition(request.getStatus(), Constants.REQUEST_STATUS_COMPLETED);
        request.setStatus(Constants.REQUEST_STATUS_COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        serviceRequestMapper.updateById(request);

        volunteerCreditService.onServiceConfirmed(request, claim);
    }

    private long calcSettlementCoins(BigDecimal serviceHours) {
        if (serviceHours == null || serviceHours.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }
        BigDecimal rawCoins = serviceHours.multiply(BigDecimal.valueOf(Constants.TIME_COINS_PER_SERVICE_HOUR));
        long rounded = rawCoins.setScale(0, RoundingMode.HALF_UP).longValue();
        return Math.max(Constants.MIN_TIME_COINS_PER_COMPLETED_SERVICE, rounded);
    }
    
    @Override
    public IPage<ServiceClaimVO> getMyServiceRecords(Long volunteerId, Integer current, Integer size, Byte claimStatus, String sortBy, String sortOrder) {
        // 查询该志愿者的认领记录
        LambdaQueryWrapper<ServiceClaim> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceClaim::getVolunteerUserId, volunteerId)
                .eq(ServiceClaim::getIsDeleted, 0);

        if (claimStatus != null) {
            wrapper.eq(ServiceClaim::getClaimStatus, claimStatus);
        }

        String normalizedSortBy = sortBy == null ? "" : sortBy.trim();
        String normalizedSortOrder = sortOrder == null ? "desc" : sortOrder.trim().toLowerCase();
        boolean asc = "asc".equals(normalizedSortOrder);

        switch (normalizedSortBy) {
            case "claimAt" -> {
                wrapper.orderBy(true, asc, ServiceClaim::getClaimAt);
                wrapper.orderByDesc(ServiceClaim::getCreatedAt);
            }
            case "hoursSubmittedAt" -> {
                wrapper.orderBy(true, asc, ServiceClaim::getHoursSubmittedAt);
                wrapper.orderByDesc(ServiceClaim::getCreatedAt);
            }
            default -> wrapper.orderBy(true, asc, ServiceClaim::getCreatedAt);
        }
        
        Page<ServiceClaim> page = new Page<>(current != null ? current : 1, size != null ? size : 10);
        IPage<ServiceClaim> claimPage = serviceClaimMapper.selectPage(page, wrapper);
        
        // 转换为VO
        List<ServiceClaimVO> voList = claimPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 填充需求信息
        fillRequestInfo(voList);
        
        // 构建分页结果
        Page<ServiceClaimVO> voPage = new Page<>(claimPage.getCurrent(), claimPage.getSize(), claimPage.getTotal());
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    /**
     * 转换为VO
     */
    private ServiceClaimVO convertToVO(ServiceClaim claim) {
        ServiceClaimVO vo = new ServiceClaimVO();
        BeanUtils.copyProperties(claim, vo);
        return vo;
    }
    
    /**
     * 填充需求信息
     */
    private void fillRequestInfo(List<ServiceClaimVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        
        // 收集所有需求ID
        List<Long> requestIds = voList.stream()
                .map(ServiceClaimVO::getRequestId)
                .distinct()
                .collect(Collectors.toList());
        
        if (requestIds.isEmpty()) {
            return;
        }
        
        // 批量查询需求
        List<ServiceRequest> requests = serviceRequestMapper.selectBatchIds(requestIds);
        Map<Long, ServiceRequest> requestMap = requests.stream()
                .collect(Collectors.toMap(ServiceRequest::getId, r -> r));
        
        // 填充需求信息
        for (ServiceClaimVO vo : voList) {
            ServiceRequest request = requestMap.get(vo.getRequestId());
            if (request != null) {
                vo.setRequestTitle(request.getServiceType());
                vo.setRequestAddress(request.getServiceAddress());
                SysUser requester = sysUserMapper.selectById(request.getRequesterUserId());
                if (requester != null) {
                    vo.setRequesterName(requester.getRealName());
                    vo.setRequesterPhone(request.getEmergencyContactPhone());
                }
            }
        }
    }
}
