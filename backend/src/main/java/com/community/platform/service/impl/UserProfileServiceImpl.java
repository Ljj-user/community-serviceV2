package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.dto.UserProfileResponse;
import com.community.platform.dto.UserProfileUpdateRequest;
import com.community.platform.generated.entity.SysRegion;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.SysRegionMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.common.Constants;
import com.community.platform.service.UserProfileService;
import com.community.platform.util.DefaultAvatarUtil;
import com.community.platform.util.IdentityTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRegionMapper sysRegionMapper;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Value("${app.avatar-base-url:http://localhost:8080/static/avatars/}")
    private String avatarBaseUrl;

    private String normalizeAvatarUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String url = raw.trim();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        // 兼容历史数据：DB 内可能存的是 /static/avatars/xxx，移动端需要可直接访问的绝对地址
        if (url.startsWith("/static/avatars/")) {
            String base = (avatarBaseUrl == null || avatarBaseUrl.isBlank())
                    ? "http://localhost:8080/static/avatars/"
                    : avatarBaseUrl.trim();
            if (!base.endsWith("/")) base = base + "/";
            return base + url.substring("/static/avatars/".length());
        }
        return url;
    }

    private SysUser getCurrentSysUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new RuntimeException("未登录");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        SysUser user = userDetails.getUser();
        return sysUserMapper.selectById(user.getId());
    }

    private boolean isAdminRole(Byte role) {
        return role != null && (role.equals(Constants.ROLE_SUPER_ADMIN) || role.equals(Constants.ROLE_COMMUNITY_ADMIN));
    }

    private UserProfileResponse convertToResponse(SysUser user) {
        UserProfileResponse resp = new UserProfileResponse();
        if (user == null) {
            return resp;
        }
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setPhone(user.getPhone());
        resp.setEmail(user.getEmail());
        resp.setAvatarUrl(normalizeAvatarUrl(user.getAvatarUrl()));
        resp.setRole(user.getRole());
        if (user.getRole() != null && user.getRole().equals(Constants.ROLE_NORMAL_USER)) {
            resp.setIdentityType(IdentityTypeUtil.normalize(user.getIdentityType()));
        } else {
            resp.setIdentityType(user.getIdentityType());
        }
        resp.setCommunityId(user.getCommunityId());
        if (user.getCommunityId() != null) {
            SysRegion region = sysRegionMapper.selectById(user.getCommunityId());
            if (region != null) {
                resp.setCommunityName(region.getName());
                resp.setProvince(region.getProvince());
                resp.setCity(region.getCity());
            }
        }
        resp.setTimeCoins(user.getTimeCoins());
        resp.setPoints(user.getPoints());
        resp.setIdentityTag(user.getIdentityTag());
        resp.setAddress(user.getAddress());
        resp.setGender(user.getGender());
        resp.setSkillTags(user.getSkillTags());
        resp.setCreatedAt(user.getCreatedAt());
        return resp;
    }

    @Override
    public UserProfileResponse getCurrentUserProfile() {
        SysUser user = getCurrentSysUser();
        return convertToResponse(user);
    }

    @Override
    public UserProfileResponse updateCurrentUserProfile(UserProfileUpdateRequest request) {
        SysUser user = getCurrentSysUser();
        boolean adminRole = isAdminRole(user.getRole());
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (adminRole) {
                throw new RuntimeException("管理员账号用户名为固定字段，不允许修改");
            }
            String nu = request.getUsername().trim();
            if (!nu.equals(user.getUsername())) {
                if (nu.length() < 2 || nu.length() > 64) {
                    throw new RuntimeException("用户名长度为 2-64 个字符");
                }
                LambdaQueryWrapper<SysUser> uw = new LambdaQueryWrapper<>();
                uw.eq(SysUser::getUsername, nu).eq(SysUser::getIsDeleted, 0);
                if (sysUserMapper.selectOne(uw) != null) {
                    throw new RuntimeException("用户名已存在");
                }
                user.setUsername(nu);
            }
        }
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            String p = request.getPhone().trim();
            if (!p.equals(user.getPhone() == null ? "" : user.getPhone())) {
                if (!p.isEmpty()) {
                    LambdaQueryWrapper<SysUser> pw = new LambdaQueryWrapper<>();
                    pw.eq(SysUser::getPhone, p).eq(SysUser::getIsDeleted, 0).ne(SysUser::getId, user.getId());
                    if (sysUserMapper.selectOne(pw) != null) {
                        throw new RuntimeException("手机号已被使用");
                    }
                }
            }
            user.setPhone(p);
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getCommunityId() != null) {
            if (adminRole) {
                throw new RuntimeException("管理员账号所属社区由系统分配，不允许在个人资料中修改");
            }
            SysRegion region = sysRegionMapper.selectById(request.getCommunityId());
            if (region == null) {
                throw new RuntimeException("社区/区域不存在，请从可用的 sys_region 列表中选择");
            }
            user.setCommunityId(request.getCommunityId());
        }
        if (request.getGender() != null) {
            Byte g = request.getGender();
            if (g < 0 || g > 2) {
                throw new RuntimeException("性别参数无效");
            }
            if (!java.util.Objects.equals(user.getGender(), g)) {
                user.setGender(g);
                if (DefaultAvatarUtil.isBundledDefaultPath(user.getAvatarUrl())) {
                    user.setAvatarUrl(DefaultAvatarUtil.resolve(g));
                }
            }
        }
        if (request.getSkillTags() != null
                && user.getRole() != null
                && user.getRole().equals(Constants.ROLE_NORMAL_USER)) {
            user.setSkillTags(normalizeSkillTagsJson(request.getSkillTags()));
        }
        if (request.getIdentityTag() != null) {
            if (adminRole) {
                throw new RuntimeException("管理员账号身份标签为固定字段，不允许修改");
            }
            user.setIdentityTag(request.getIdentityTag());
        }
        user.setUpdatedAt(java.time.LocalDateTime.now());
        sysUserMapper.updateById(user);
        return convertToResponse(sysUserMapper.selectById(user.getId()));
    }

    private String normalizeSkillTagsJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "[]";
        }
        String text = raw.trim();
        try {
            List<String> parsed = objectMapper.readValue(text, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            return toJsonArray(parsed);
        } catch (Exception ignored) {
            // 兼容旧前端传入 "助老,陪诊" 这类 CSV
            String[] parts = text.split(",");
            List<String> tags = new ArrayList<>();
            for (String p : parts) {
                if (p == null) continue;
                String t = p.trim();
                if (!t.isEmpty()) tags.add(t);
            }
            return toJsonArray(tags);
        }
    }

    private String toJsonArray(List<String> input) {
        if (input == null || input.isEmpty()) {
            return "[]";
        }
        List<String> cleaned = input.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (cleaned.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public UserProfileResponse updateAvatar(String avatarUrl) {
        SysUser user = getCurrentSysUser();
        user.setAvatarUrl(avatarUrl);
        sysUserMapper.updateById(user);
        return convertToResponse(user);
    }
}

