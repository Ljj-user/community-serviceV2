package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.Constants;
import com.community.platform.dto.HallSummaryVO;
import com.community.platform.generated.entity.ServiceClaim;
import com.community.platform.generated.entity.ServiceEvaluation;
import com.community.platform.generated.entity.ServiceRequest;
import com.community.platform.generated.mapper.ServiceClaimMapper;
import com.community.platform.generated.mapper.ServiceEvaluationMapper;
import com.community.platform.generated.mapper.ServiceRequestMapper;
import com.community.platform.service.HallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class HallServiceImpl implements HallService {

    @Autowired
    private ServiceRequestMapper serviceRequestMapper;

    @Autowired
    private ServiceClaimMapper serviceClaimMapper;

    @Autowired
    private ServiceEvaluationMapper serviceEvaluationMapper;

    @Override
    public HallSummaryVO getSummary(Long userId) {
        HallSummaryVO vo = new HallSummaryVO();

        // 我的发布历史：按 requester_user_id 统计（不区分状态，前端可自行展示）
        Long myCount = serviceRequestMapper.selectCount(new LambdaQueryWrapper<ServiceRequest>()
                .eq(ServiceRequest::getIsDeleted, 0)
                .eq(ServiceRequest::getRequesterUserId, userId));
        vo.setMyPublishedCount(myCount == null ? 0L : myCount);

        // 已完成的任务：我作为志愿者认领且 claim_status=2
        Long completed = serviceClaimMapper.selectCount(new LambdaQueryWrapper<ServiceClaim>()
                .eq(ServiceClaim::getIsDeleted, 0)
                .eq(ServiceClaim::getVolunteerUserId, userId)
                .eq(ServiceClaim::getClaimStatus, Constants.CLAIM_STATUS_COMPLETED));
        vo.setMyCompletedCount(completed == null ? 0L : completed);

        // 进行中的单子：我作为志愿者认领且 claim_status=1
        Long inProgress = serviceClaimMapper.selectCount(new LambdaQueryWrapper<ServiceClaim>()
                .eq(ServiceClaim::getIsDeleted, 0)
                .eq(ServiceClaim::getVolunteerUserId, userId)
                .in(ServiceClaim::getClaimStatus, Constants.CLAIM_STATUS_CLAIMED, Constants.CLAIM_STATUS_PENDING_CONFIRM));
        vo.setInProgressCount(inProgress == null ? 0L : inProgress);

        // 我收到的评价：volunteer_user_id = userId
        Long evalCount = serviceEvaluationMapper.selectCount(new LambdaQueryWrapper<ServiceEvaluation>()
                .eq(ServiceEvaluation::getIsDeleted, 0)
                .eq(ServiceEvaluation::getVolunteerUserId, userId));
        vo.setReceivedEvaluationCount(evalCount == null ? 0L : evalCount);

        // 平均评分（简单取平均）
        List<ServiceEvaluation> evals = serviceEvaluationMapper.selectList(new LambdaQueryWrapper<ServiceEvaluation>()
                .select(ServiceEvaluation::getRating)
                .eq(ServiceEvaluation::getIsDeleted, 0)
                .eq(ServiceEvaluation::getVolunteerUserId, userId));
        if (evals != null && !evals.isEmpty()) {
            int sum = 0;
            int n = 0;
            for (ServiceEvaluation e : evals) {
                if (e.getRating() != null) {
                    sum += e.getRating();
                    n++;
                }
            }
            if (n > 0) {
                vo.setReceivedAvgRating(BigDecimal.valueOf(sum)
                        .divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP));
            }
        }

        return vo;
    }
}

