package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.Constants;
import com.community.platform.dto.LoginRequest;
import com.community.platform.dto.LoginResponse;
import com.community.platform.dto.RegisterRequest;
import com.community.platform.dto.SendVerificationCodeRequest;
import com.community.platform.dto.UserOnboardingProfileVO;
import com.community.platform.dto.UserOnboardingSubmitRequest;
import com.community.platform.dto.UserInfo;
import com.community.platform.dto.ChangePasswordRequest;
import com.community.platform.dto.VerificationCodeTicketVO;
import com.community.platform.generated.entity.SysRegion;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.SysRegionMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.AuthService;
import com.community.platform.util.DefaultAvatarUtil;
import com.community.platform.util.IdentityTypeUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 认证服务实现
 */
@Service
public class AuthServiceImpl implements AuthService {
    
    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRegionMapper sysRegionMapper;
    
    @Value("${app.avatar-base-url:http://localhost:8080/static/avatars/}")
    private String avatarBaseUrl;

    @Value("${app.auth.expose-dev-code:false}")
    private boolean exposeDevCode;

    private String normalizeAvatarUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String url = raw.trim();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (url.startsWith("/static/avatars/")) {
            String base = (avatarBaseUrl == null || avatarBaseUrl.isBlank())
                    ? "http://localhost:8080/static/avatars/"
                    : avatarBaseUrl.trim();
            if (!base.endsWith("/")) base = base + "/";
            return base + url.substring("/static/avatars/".length());
        }
        return url;
    }

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        // Spring Security 认证
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        SysUser user = userDetails.getUser();
        
        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        
        // 生成 token
        String token = com.community.platform.security.JwtTokenUtil.generateToken(user.getUsername());
        
        // 转换为 UserInfo
        UserInfo userInfo = convertToUserInfo(user);
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserInfo(userInfo);
        
        return response;
    }
    
    @Override
    @Transactional
    public UserInfo register(RegisterRequest request) {
        validateVerificationCode(request.getEmail(), request.getVerificationScene(), request.getVerificationCode());
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername())
               .eq(SysUser::getIsDeleted, 0)
               .last("LIMIT 1");  // 确保只返回一条记录
        
        if (sysUserMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }

        LambdaQueryWrapper<SysUser> phoneW = new LambdaQueryWrapper<>();
        phoneW.eq(SysUser::getPhone, request.getPhone())
                .eq(SysUser::getIsDeleted, 0)
                .last("LIMIT 1");
        if (sysUserMapper.selectOne(phoneW) != null) {
            throw new RuntimeException("手机号已被注册");
        }
        
        // 创建新用户
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordMd5(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        // 注册只允许创建“普通用户”，禁止前端/接口参数越权注册管理员
        user.setRole(Constants.ROLE_NORMAL_USER);
        user.setIdentityType(IdentityTypeUtil.normalize(request.getIdentityType()));
        if (request.getCommunityId() != null) {
            SysRegion region = sysRegionMapper.selectById(request.getCommunityId());
            if (region == null) {
                throw new RuntimeException("所选社区不存在，community_id 必须对应 sys_region.id");
            }
            user.setCommunityId(request.getCommunityId());
        } else {
            user.setCommunityId(null);
        }
        user.setTimeCoins(0L);
        user.setPoints(0L);
        Byte g = request.getGender();
        if (g == null || g < 0 || g > 2) {
            g = (byte) 0;
        }
        user.setGender(g);
        user.setAvatarUrl(DefaultAvatarUtil.resolve(g));
        user.setStatus(Constants.USER_STATUS_ENABLED);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setIsDeleted((byte) 0);

        sysUserMapper.insert(user);
        List<String> normalizedSkills = normalizeSkillTags(request.getSkillTags());
        if (!normalizedSkills.isEmpty()) {
            String skillJson = toJsonArray(normalizedSkills);
            user.setSkillTags(skillJson);
            user.setUpdatedAt(LocalDateTime.now());
            sysUserMapper.updateById(user);
            replaceUserSkills(user.getId(), normalizedSkills);
        }
        SysUser persisted = sysUserMapper.selectById(user.getId());
        return convertToUserInfo(persisted != null ? persisted : user);
    }

    @Override
    public VerificationCodeTicketVO sendVerificationCode(SendVerificationCodeRequest request) {
        String scene = (request.getScene() == null || request.getScene().isBlank()) ? "REGISTER" : request.getScene().trim().toUpperCase();
        String code = String.format("%06d", new Random().nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
        jdbcTemplate.update(
                "INSERT INTO verify_code_ticket(scene,target,verify_code,expires_at,is_used,created_at,updated_at) VALUES(?,?,?,?,0,NOW(),NOW())",
                scene, request.getEmail().trim(), code, expiresAt
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        VerificationCodeTicketVO vo = new VerificationCodeTicketVO();
        vo.setTicketId(id);
        vo.setScene(scene);
        vo.setTarget(maskEmail(request.getEmail().trim()));
        vo.setExpiresAt(expiresAt);
        if (exposeDevCode) {
            vo.setDevCode(code);
        }
        return vo;
    }

    @Override
    @Transactional
    public UserOnboardingProfileVO submitOnboarding(Long userId, UserOnboardingSubmitRequest request) {
        List<String> normalizedSkills = normalizeSkillTags(request.getSkillTags());
        String skillJson = toJsonArray(normalizedSkills);
        String featuresJson = toJsonArray(request.getPreferredFeatures());
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_onboarding_profile WHERE user_id=?",
                Integer.class, userId
        );
        if (exists != null && exists > 0) {
            jdbcTemplate.update("UPDATE user_onboarding_profile SET skill_tags_json=?, preferred_features_json=?, intent_note=?, updated_at=NOW() WHERE user_id=?",
                    skillJson, featuresJson, request.getIntentNote(), userId);
        } else {
            jdbcTemplate.update("INSERT INTO user_onboarding_profile(user_id, skill_tags_json, preferred_features_json, intent_note, created_at, updated_at) VALUES(?,?,?,?,NOW(),NOW())",
                    userId, skillJson, featuresJson, request.getIntentNote());
        }
        jdbcTemplate.update("DELETE FROM user_onboarding_answer WHERE user_id=?", userId);
        batchInsertAnswers(userId, "skill_tags", normalizedSkills);
        batchInsertAnswers(userId, "preferred_features", request.getPreferredFeatures());
        if (request.getIntentNote() != null && !request.getIntentNote().isBlank()) {
            jdbcTemplate.update("INSERT INTO user_onboarding_answer(user_id, question_key, answer_value, created_at) VALUES(?,?,?,NOW())",
                    userId, "intent_note", request.getIntentNote().trim());
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null && !normalizedSkills.isEmpty()) {
            user.setSkillTags(skillJson);
            user.setUpdatedAt(LocalDateTime.now());
            sysUserMapper.updateById(user);
        }
        replaceUserSkills(userId, normalizedSkills);
        return getOnboarding(userId);
    }

    @Override
    public UserOnboardingProfileVO getOnboarding(Long userId) {
        List<UserOnboardingProfileVO> list = jdbcTemplate.query(
                "SELECT user_id, skill_tags_json, preferred_features_json, intent_note, updated_at FROM user_onboarding_profile WHERE user_id=?",
                (rs, rowNum) -> {
                    UserOnboardingProfileVO vo = new UserOnboardingProfileVO();
                    vo.setUserId(rs.getLong("user_id"));
                    vo.setSkillTags(parseJsonArray(rs.getString("skill_tags_json")));
                    vo.setPreferredFeatures(parseJsonArray(rs.getString("preferred_features_json")));
                    vo.setIntentNote(rs.getString("intent_note"));
                    vo.setUpdatedAt(rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
                    return vo;
                }, userId
        );
        if (list.isEmpty()) {
            UserOnboardingProfileVO vo = new UserOnboardingProfileVO();
            vo.setUserId(userId);
            vo.setSkillTags(List.of());
            vo.setPreferredFeatures(List.of());
            return vo;
        }
        return list.get(0);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new RuntimeException("未登录");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        SysUser user = sysUserMapper.selectById(userDetails.getUser().getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 校验旧密码（兼容历史 MD5，新增使用 BCrypt）
        boolean match = passwordEncoder.matches(request.getOldPassword(), user.getPasswordMd5());
        if (!match) {
            throw new RuntimeException("旧密码不正确");
        }

        // 更新新密码
        user.setPasswordMd5(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    @Override
    public UserInfo getUserInfoById(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() != 0)) {
            throw new RuntimeException("用户不存在");
        }
        return convertToUserInfo(user);
    }

    /**
     * 转换为 UserInfo DTO（身份字段归一化，避免历史脏数据）
     */
    private UserInfo convertToUserInfo(SysUser user) {
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        userInfo.setAvatarUrl(normalizeAvatarUrl(user.getAvatarUrl()));
        if (user.getRole() != null && user.getRole().equals(Constants.ROLE_NORMAL_USER)) {
            userInfo.setIdentityType(IdentityTypeUtil.normalize(user.getIdentityType()));
        }
        if (user.getCommunityId() != null) {
            SysRegion region = sysRegionMapper.selectById(user.getCommunityId());
            if (region != null) {
                userInfo.setCommunityName(region.getName());
                userInfo.setProvince(region.getProvince());
                userInfo.setCity(region.getCity());
            }
        }
        return userInfo;
    }

    private void validateVerificationCode(String email, String scene, String code) {
        String actualScene = (scene == null || scene.isBlank()) ? "REGISTER" : scene.trim().toUpperCase();
        List<String> records = jdbcTemplate.query(
                "SELECT verify_code FROM verify_code_ticket WHERE target=? AND scene=? AND is_used=0 AND expires_at>=NOW() ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> rs.getString("verify_code"),
                email.trim(), actualScene
        );
        if (records.isEmpty()) {
            throw new RuntimeException("验证码不存在或已过期");
        }
        String latestCode = records.get(0);
        if (!latestCode.equals(code)) {
            throw new RuntimeException("验证码错误");
        }
        jdbcTemplate.update(
                "UPDATE verify_code_ticket SET is_used=1, used_at=NOW(), updated_at=NOW() WHERE target=? AND scene=? AND verify_code=? AND is_used=0",
                email.trim(), actualScene, code
        );
    }

    private void batchInsertAnswers(Long userId, String key, List<String> answers) {
        if (answers == null || answers.isEmpty()) {
            return;
        }
        for (String answer : answers) {
            if (answer == null || answer.isBlank()) {
                continue;
            }
            jdbcTemplate.update("INSERT INTO user_onboarding_answer(user_id, question_key, answer_value, created_at) VALUES(?,?,?,NOW())",
                    userId, key, answer.trim());
        }
    }

    private String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(values);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private List<String> parseJsonArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(raw,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String maskEmail(String email) {
        int idx = email.indexOf("@");
        if (idx <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(idx);
    }

    private List<String> normalizeSkillTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return List.of();
        }
        Set<String> dedup = new LinkedHashSet<>();
        for (String tag : rawTags) {
            if (tag == null) continue;
            String t = tag.trim();
            if (t.isEmpty()) continue;
            if (t.length() > 64) {
                t = t.substring(0, 64);
            }
            dedup.add(t);
        }
        return List.copyOf(dedup);
    }

    private void replaceUserSkills(Long userId, List<String> skills) {
        try {
            List<String> oldTags = jdbcTemplate.query(
                    "SELECT skill_tag FROM sys_user_skill WHERE user_id=?",
                    (rs, rowNum) -> rs.getString("skill_tag"),
                    userId
            );
            jdbcTemplate.update("DELETE FROM sys_user_skill WHERE user_id=?", userId);
            if (skills != null) {
                for (String tag : skills) {
                    if (tag == null || tag.isBlank()) continue;
                    jdbcTemplate.update("INSERT IGNORE INTO sys_user_skill(user_id, skill_tag, created_at) VALUES(?,?,NOW())",
                            userId, tag.trim());
                }
            }
            Set<String> affected = new LinkedHashSet<>();
            if (oldTags != null) affected.addAll(oldTags);
            if (skills != null) affected.addAll(skills);
            refreshSkillTagStats(affected);
        } catch (Exception ignored) {
        }
    }

    private void refreshSkillTagStats(Set<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        for (String raw : tags) {
            if (raw == null || raw.isBlank()) continue;
            String tag = raw.trim();
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM sys_user_skill WHERE skill_tag=?",
                    Integer.class,
                    tag
            );
            int count = cnt == null ? 0 : cnt;
            if (count <= 0) {
                jdbcTemplate.update("DELETE FROM skill_tag_stat WHERE skill_tag=?", tag);
            } else {
                jdbcTemplate.update("""
                        INSERT INTO skill_tag_stat(skill_tag, user_count, updated_at)
                        VALUES(?, ?, NOW())
                        ON DUPLICATE KEY UPDATE user_count=VALUES(user_count), updated_at=VALUES(updated_at)
                        """, tag, count);
            }
        }
    }
}
