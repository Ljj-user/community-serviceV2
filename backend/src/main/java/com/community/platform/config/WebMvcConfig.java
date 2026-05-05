package com.community.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 静态资源映射配置，用于访问上传的头像文件
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.avatar-upload-dir:uploads/avatars}")
    private String avatarUploadDir;
    @Value("${app.banner-upload-dir:uploads/banner}")
    private String bannerUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = normalizeDir(avatarUploadDir);
        registry.addResourceHandler("/static/avatars/**")
                .addResourceLocations("file:" + location);

        String bannerLocation = normalizeDir(bannerUploadDir);
        registry.addResourceHandler("/static/banner/**")
                .addResourceLocations("file:" + bannerLocation);
    }

    private String normalizeDir(String path) {
        File dir = new File(path).getAbsoluteFile();
        if (!dir.exists()) {
            // best effort; failures will surface on upload
            dir.mkdirs();
        }
        String location = dir.getPath().replace("\\", "/");
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        return location;
    }
}

