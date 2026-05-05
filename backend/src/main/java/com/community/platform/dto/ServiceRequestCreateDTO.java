package com.community.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求发布请求 DTO
 */
@Data
public class ServiceRequestCreateDTO {
    @NotBlank(message = "服务类型不能为空")
    private String serviceType;
    
    @NotBlank(message = "服务地址不能为空")
    private String serviceAddress;
    
    private String description;
    
    private LocalDateTime expectedTime;
    
    @NotNull(message = "紧急程度不能为空")
    private Byte urgencyLevel;  // 1低 2中 3高 4紧急

    /**
     * 紧急联系人姓名
     */
    private String emergencyContactName;

    /**
     * 紧急联系人电话
     */
    private String emergencyContactPhone;

    /**
     * 与服务对象关系（子女/邻居等）
     */
    private String emergencyContactRelation;
    
    /**
     * 特殊人群/需求标签（如独居老人、残障等）
     */
    private List<String> specialTags;

    /**
     * AI 分析记录ID（可选）
     */
    private Long aiAnalysisRecordId;
}
