package com.community.platform.dto;

import lombok.Data;

@Data
public class AiAnalysisRecordVO {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private Long communityId;
    private String communityName;
    private String scene;
    private String inputText;
    private String resultMode;
    private String resultJson;
    private Integer appliedToForm;
    private Integer submittedSuccess;
    private String createdAt;
}
