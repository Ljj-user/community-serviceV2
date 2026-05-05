package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.UserProfileResponse;
import com.community.platform.dto.UserProfileUpdateRequest;
import com.community.platform.service.UserProfileService;
import com.community.platform.security.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 用户资料 / 头像相关接口
 */
@RestController
@RequestMapping("/user")
public class UserProfileController {
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    @Autowired
    private UserProfileService userProfileService;

    /**
     * 静态资源访问前缀，例如 http://localhost:8080/static/avatars/
     */
    @Value("${app.avatar-base-url:http://localhost:8080/static/avatars/}")
    private String avatarBaseUrl;

    /**
     * 头像存储根目录（本地磁盘路径）
     */
    @Value("${app.avatar-upload-dir:uploads/avatars}")
    private String avatarUploadDir;

    /**
     * 获取当前登录用户资料
     */
    @GetMapping("/profile")
    public Result<UserProfileResponse> getProfile() {
        UserProfileResponse profile = userProfileService.getCurrentUserProfile();
        return Result.success(profile);
    }

    /**
     * 更新当前用户资料（含头像 URL）
     */
    @PutMapping("/profile")
    public Result<UserProfileResponse> updateProfile(
            @Valid @RequestBody UserProfileUpdateRequest request,
            HttpServletRequest httpRequest) {
        UserProfileResponse before = userProfileService.getCurrentUserProfile();
        UserProfileResponse profile = userProfileService.updateCurrentUserProfile(request);
        if (before.getUsername() != null && profile.getUsername() != null
                && !before.getUsername().equals(profile.getUsername())) {
            String auth = httpRequest.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                JwtTokenUtil.updateUsernameForToken(token, profile.getUsername());
            }
        }
        return Result.success("更新成功", profile);
    }

    /**
     * 上传头像文件，并更新 avatar_url
     */
    @PostMapping("/avatar")
    public Result<UserProfileResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件为空");
        }
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalFilename);
        String extLower = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        boolean extAllowed = !extLower.isEmpty() && ALLOWED_EXT.contains(extLower);
        boolean mimeLooksImage = contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
        if (!mimeLooksImage && !extAllowed) {
            return Result.error("仅支持图片类型文件");
        }

        String filename = UUID.randomUUID().toString().replace("-", "");
        if (extAllowed) {
            filename = filename + "." + extLower;
        }

        File uploadRoot = new File(avatarUploadDir).getAbsoluteFile();
        if (!uploadRoot.exists()) {
            boolean ok = uploadRoot.mkdirs();
            if (!ok) {
                return Result.error("上传头像失败: 无法创建目录 " + uploadRoot.getPath());
            }
        }

        File dest = new File(uploadRoot, filename);
        try {
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            file.transferTo(dest);
        } catch (IOException e) {
            return Result.error("上传头像失败: " + e.getMessage());
        }

        String base = avatarBaseUrl == null ? "/static/avatars/" : avatarBaseUrl.trim();
        if (!base.endsWith("/")) base = base + "/";
        String avatarUrl = base + filename;
        UserProfileResponse profile = userProfileService.updateAvatar(avatarUrl);
        return Result.success("头像更新成功", profile);
    }
}

