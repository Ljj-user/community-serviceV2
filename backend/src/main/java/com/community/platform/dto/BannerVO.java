package com.community.platform.dto;

import lombok.Data;

@Data
public class BannerVO {
    private Long id;
    private Long communityId;
    private String communityName;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String linkUrl;
}

