package com.community.platform.service.impl;

import com.community.platform.common.Constants;
import com.community.platform.dto.ServiceEvaluationDTO;
import com.community.platform.dto.ServiceEvaluationHistoryVO;
import com.community.platform.dto.ServiceEvaluationPendingVO;
import com.community.platform.dto.VolunteerEvaluationVO;
import com.community.platform.generated.entity.ServiceClaim;
import com.community.platform.generated.entity.ServiceEvaluation;
import com.community.platform.generated.entity.ServiceRequest;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.ServiceClaimMapper;
import com.community.platform.generated.mapper.ServiceEvaluationMapper;
import com.community.platform.generated.mapper.ServiceRequestMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.service.ServiceEvaluationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务评价服务实现
 */
@Service
public class ServiceEvaluationServiceImpl implements ServiceEvaluationService {
    
    @Autowired
    private ServiceEvaluationMapper serviceEvaluationMapper;
    
    @Autowired
    private ServiceClaimMapper serviceClaimMapper;
    
    @Autowired
    private ServiceRequestMapper serviceRequestMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Override
    @Transactional
    public void evaluateService(Long userId, ServiceEvaluationDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }

        if (user.getRole() != Constants.ROLE_NORMAL_USER) {
            throw new RuntimeException("只有普通用户可以评价服务");
        }

        // 查询认领记录
        ServiceClaim claim = serviceClaimMapper.selectById(dto.getClaimId());
        if (claim == null || claim.getIsDeleted() == 1) {
            throw new RuntimeException("认领记录不存在");
        }
        
        if (claim.getClaimStatus() != Constants.CLAIM_STATUS_COMPLETED) {
            throw new RuntimeException("只能评价已完成的服务");
        }
        
        // 查询需求（确定双方身份）
        ServiceRequest request = serviceRequestMapper.selectById(claim.getRequestId());
        if (request == null || request.getIsDeleted() == 1) {
            throw new RuntimeException("需求不存在");
        }

        Byte evaluatorRole;
        if (request.getRequesterUserId() != null && request.getRequesterUserId().equals(userId)) {
            evaluatorRole = Constants.EVAL_ROLE_RESIDENT;
        } else if (claim.getVolunteerUserId() != null && claim.getVolunteerUserId().equals(userId)) {
            evaluatorRole = Constants.EVAL_ROLE_VOLUNTEER;
        } else {
            throw new RuntimeException("无权评价该订单");
        }
        
        // 检查是否已评价（同一认领：居民/志愿者各评价一次）
        ServiceEvaluation existingEvaluation = serviceEvaluationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ServiceEvaluation>()
                        .eq(ServiceEvaluation::getClaimId, dto.getClaimId())
                        .eq(ServiceEvaluation::getEvaluatorRole, evaluatorRole)
                        .eq(ServiceEvaluation::getIsDeleted, 0)
                        .last("LIMIT 1")
        );
        
        if (existingEvaluation != null) {
            throw new RuntimeException("该服务已评价，不能重复评价");
        }
        
        // 创建评价记录
        ServiceEvaluation evaluation = new ServiceEvaluation();
        evaluation.setClaimId(dto.getClaimId());
        evaluation.setRequestId(claim.getRequestId());
        evaluation.setResidentUserId(request.getRequesterUserId());
        evaluation.setVolunteerUserId(claim.getVolunteerUserId());
        evaluation.setEvaluatorRole(evaluatorRole);
        evaluation.setRating(dto.getRating());
        evaluation.setContent(dto.getContent());
        evaluation.setCreatedAt(LocalDateTime.now());
        evaluation.setUpdatedAt(LocalDateTime.now());
        evaluation.setIsDeleted((byte) 0);
        
        serviceEvaluationMapper.insert(evaluation);
    }

    @Override
    public IPage<ServiceEvaluationPendingVO> listPending(Long residentId, Integer current, Integer size) {
        // 已完成的认领记录 + 需求属于该居民 + 未评价
        Page<ServiceClaim> page = new Page<>(current, size);
        LambdaQueryWrapper<ServiceClaim> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceClaim::getIsDeleted, 0);
        wrapper.eq(ServiceClaim::getClaimStatus, Constants.CLAIM_STATUS_COMPLETED);
        wrapper.orderByDesc(ServiceClaim::getHoursSubmittedAt);

        IPage<ServiceClaim> claimPage = serviceClaimMapper.selectPage(page, wrapper);
        List<ServiceClaim> claims = claimPage.getRecords();
        if (claims.isEmpty()) {
            Page<ServiceEvaluationPendingVO> voPage = new Page<>(claimPage.getCurrent(), claimPage.getSize(), 0);
            voPage.setRecords(List.of());
            return voPage;
        }

        List<Long> requestIds = claims.stream().map(ServiceClaim::getRequestId).distinct().toList();
        Map<Long, ServiceRequest> requestMap = serviceRequestMapper.selectBatchIds(requestIds).stream()
                .filter(r -> r.getIsDeleted() == 0 && r.getRequesterUserId().equals(residentId))
                .collect(Collectors.toMap(ServiceRequest::getId, r -> r));

        // 已评价的 claimId
        List<Long> claimIds = claims.stream().map(ServiceClaim::getId).toList();
        List<ServiceEvaluation> evaluations = serviceEvaluationMapper.selectList(
                new LambdaQueryWrapper<ServiceEvaluation>()
                        .in(ServiceEvaluation::getClaimId, claimIds)
                        .eq(ServiceEvaluation::getEvaluatorRole, Constants.EVAL_ROLE_RESIDENT)
                        .eq(ServiceEvaluation::getIsDeleted, 0)
        );
        var evaluatedSet = evaluations.stream().map(ServiceEvaluation::getClaimId).collect(Collectors.toSet());

        // 志愿者姓名
        List<Long> volunteerIds = claims.stream().map(ServiceClaim::getVolunteerUserId).distinct().toList();
        Map<Long, SysUser> volunteerMap = sysUserMapper.selectBatchIds(volunteerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<ServiceEvaluationPendingVO> vos = claims.stream()
                .filter(c -> requestMap.containsKey(c.getRequestId()))
                .filter(c -> !evaluatedSet.contains(c.getId()))
                .map(c -> {
                    ServiceRequest req = requestMap.get(c.getRequestId());
                    ServiceEvaluationPendingVO vo = new ServiceEvaluationPendingVO();
                    vo.setClaimId(c.getId());
                    vo.setRequestId(req.getId());
                    vo.setServiceType(req.getServiceType());
                    vo.setServiceAddress(req.getServiceAddress());
                    vo.setCompletedAt(req.getCompletedAt());
                    vo.setServiceHours(c.getServiceHours());
                    vo.setVolunteerUserId(c.getVolunteerUserId());
                    SysUser v = volunteerMap.get(c.getVolunteerUserId());
                    vo.setVolunteerName(v != null ? v.getRealName() : null);
                    return vo;
                })
                .toList();

        Page<ServiceEvaluationPendingVO> voPage = new Page<>(claimPage.getCurrent(), claimPage.getSize(), vos.size());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public IPage<ServiceEvaluationHistoryVO> listHistory(Long residentId, Integer current, Integer size) {
        Page<ServiceEvaluation> page = new Page<>(current, size);
        LambdaQueryWrapper<ServiceEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceEvaluation::getIsDeleted, 0);
        wrapper.and(w -> w.eq(ServiceEvaluation::getEvaluatorRole, Constants.EVAL_ROLE_RESIDENT)
                        .eq(ServiceEvaluation::getResidentUserId, residentId)
                .or(x -> x.eq(ServiceEvaluation::getEvaluatorRole, Constants.EVAL_ROLE_VOLUNTEER)
                        .eq(ServiceEvaluation::getVolunteerUserId, residentId)));
        wrapper.orderByDesc(ServiceEvaluation::getCreatedAt);

        IPage<ServiceEvaluation> evalPage = serviceEvaluationMapper.selectPage(page, wrapper);
        List<ServiceEvaluation> evals = evalPage.getRecords();
        if (evals.isEmpty()) {
            Page<ServiceEvaluationHistoryVO> voPage = new Page<>(evalPage.getCurrent(), evalPage.getSize(), 0);
            voPage.setRecords(List.of());
            return voPage;
        }

        List<Long> requestIds = evals.stream().map(ServiceEvaluation::getRequestId).distinct().toList();
        Map<Long, ServiceRequest> requestMap = serviceRequestMapper.selectBatchIds(requestIds).stream()
                .filter(r -> r.getIsDeleted() == 0)
                .collect(Collectors.toMap(ServiceRequest::getId, r -> r));

        List<Long> volunteerIds = evals.stream().map(ServiceEvaluation::getVolunteerUserId).distinct().toList();
        Map<Long, SysUser> volunteerMap = sysUserMapper.selectBatchIds(volunteerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<ServiceEvaluationHistoryVO> vos = evals.stream()
                .map(e -> {
                    ServiceEvaluationHistoryVO vo = new ServiceEvaluationHistoryVO();
                    vo.setId(e.getId());
                    vo.setClaimId(e.getClaimId());
                    vo.setRequestId(e.getRequestId());
                    ServiceRequest req = requestMap.get(e.getRequestId());
                    if (req != null) {
                        vo.setServiceType(req.getServiceType());
                        vo.setServiceAddress(req.getServiceAddress());
                    }
                    vo.setVolunteerUserId(e.getVolunteerUserId());
                    SysUser v = volunteerMap.get(e.getVolunteerUserId());
                    vo.setVolunteerName(v != null ? v.getRealName() : null);
                    vo.setRating(e.getRating());
                    vo.setContent(e.getContent());
                    vo.setCreatedAt(e.getCreatedAt());
                    return vo;
                })
                .toList();

        Page<ServiceEvaluationHistoryVO> voPage = new Page<>(evalPage.getCurrent(), evalPage.getSize(), evalPage.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public IPage<VolunteerEvaluationVO> listReceived(Long volunteerId, Integer current, Integer size) {
        SysUser user = sysUserMapper.selectById(volunteerId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getRole() != Constants.ROLE_NORMAL_USER) {
            throw new RuntimeException("只有普通用户可以查看评价");
        }

        Page<ServiceEvaluation> page = new Page<>(current, size);
        LambdaQueryWrapper<ServiceEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceEvaluation::getIsDeleted, 0);
        wrapper.eq(ServiceEvaluation::getVolunteerUserId, volunteerId);
        wrapper.eq(ServiceEvaluation::getEvaluatorRole, Constants.EVAL_ROLE_RESIDENT);
        wrapper.orderByDesc(ServiceEvaluation::getCreatedAt);

        IPage<ServiceEvaluation> evalPage = serviceEvaluationMapper.selectPage(page, wrapper);
        List<ServiceEvaluation> evals = evalPage.getRecords();
        if (evals.isEmpty()) {
            Page<VolunteerEvaluationVO> voPage = new Page<>(evalPage.getCurrent(), evalPage.getSize(), 0);
            voPage.setRecords(List.of());
            return voPage;
        }

        List<Long> requestIds = evals.stream().map(ServiceEvaluation::getRequestId).distinct().toList();
        Map<Long, ServiceRequest> requestMap = serviceRequestMapper.selectBatchIds(requestIds).stream()
                .filter(r -> r.getIsDeleted() == 0)
                .collect(Collectors.toMap(ServiceRequest::getId, r -> r));

        List<Long> residentIds = evals.stream().map(ServiceEvaluation::getResidentUserId).distinct().toList();
        Map<Long, SysUser> residentMap = sysUserMapper.selectBatchIds(residentIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<VolunteerEvaluationVO> vos = evals.stream()
                .map(e -> {
                    VolunteerEvaluationVO vo = new VolunteerEvaluationVO();
                    vo.setId(e.getId());
                    vo.setClaimId(e.getClaimId());
                    vo.setRequestId(e.getRequestId());
                    ServiceRequest req = requestMap.get(e.getRequestId());
                    if (req != null) {
                        vo.setServiceType(req.getServiceType());
                        vo.setServiceAddress(req.getServiceAddress());
                    }
                    vo.setResidentUserId(e.getResidentUserId());
                    SysUser r = residentMap.get(e.getResidentUserId());
                    vo.setResidentName(r != null ? r.getRealName() : null);
                    vo.setRating(e.getRating());
                    vo.setContent(e.getContent());
                    vo.setCreatedAt(e.getCreatedAt());
                    return vo;
                })
                .toList();

        Page<VolunteerEvaluationVO> voPage = new Page<>(evalPage.getCurrent(), evalPage.getSize(), evalPage.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }
}
