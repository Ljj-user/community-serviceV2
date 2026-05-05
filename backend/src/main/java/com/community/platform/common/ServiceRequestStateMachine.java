package com.community.platform.common;

import java.util.Map;
import java.util.Set;

/**
 * 需求状态机：统一限制状态流转，避免非法回退/跳转。
 */
public final class ServiceRequestStateMachine {

    private ServiceRequestStateMachine() {}

    private static final Map<Byte, Set<Byte>> ALLOWED_TRANSITIONS = Map.of(
            Constants.REQUEST_STATUS_PENDING, Set.of(Constants.REQUEST_STATUS_PUBLISHED, Constants.REQUEST_STATUS_REJECTED),
            Constants.REQUEST_STATUS_PUBLISHED, Set.of(Constants.REQUEST_STATUS_CLAIMED),
            // 兼容历史流程：旧数据可能直接从“已认领”完成（2->3）
            Constants.REQUEST_STATUS_CLAIMED, Set.of(Constants.REQUEST_STATUS_PENDING_CONFIRM, Constants.REQUEST_STATUS_COMPLETED),
            Constants.REQUEST_STATUS_PENDING_CONFIRM, Set.of(Constants.REQUEST_STATUS_COMPLETED),
            Constants.REQUEST_STATUS_COMPLETED, Set.of(),
            Constants.REQUEST_STATUS_REJECTED, Set.of()
    );

    public static void assertTransition(Byte from, Byte to) {
        if (from == null || to == null) {
            throw new RuntimeException("状态不能为空");
        }
        if (from.equals(to)) {
            return;
        }
        Set<Byte> allowed = ALLOWED_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new RuntimeException(String.format("非法状态流转: %s -> %s", from, to));
        }
    }
}
