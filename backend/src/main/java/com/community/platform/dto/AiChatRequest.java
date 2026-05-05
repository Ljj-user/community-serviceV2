package com.community.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {
    @NotBlank(message = "消息不能为空")
    private String message;

    /**
     * 可选：最近若干轮对话上下文（移动端透传）
     */
    private List<HistoryMessage> history;

    @Data
    public static class HistoryMessage {
        /**
         * user / assistant / ai
         */
        private String role;
        private String text;
    }
}
