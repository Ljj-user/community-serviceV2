package com.community.platform.dto;

import lombok.Data;

@Data
public class AppRuntimeVO {
    private Boolean demoModeEnabled;
    private String demoModeLabel;
    private String demoDataHint;
}
