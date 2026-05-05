package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.Constants;
import com.community.platform.dto.*;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.entity.SysRegion;
import com.community.platform.generated.mapper.SysRegionMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.AdminUserService;
import com.community.platform.util.IdentityTypeUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRegionMapper sysRegionMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private SysUser currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new RuntimeException("未登录");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        SysUser u = userDetails.getUser();
        return sysUserMapper.selectById(u.getId());
    }

    @Override
    public PageResult<AdminUserVO> list(AdminUserListQuery query) {
        SysUser operator = currentOperator();
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : query.getSize();

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getIsDeleted, 0);
        // 网格化隔离：社区管理员只看自己社区用户
        if (operator != null && operator.getRole() != null && operator.getRole().equals(Constants.ROLE_COMMUNITY_ADMIN)) {
            if (operator.getCommunityId() == null) {
                throw new RuntimeException("管理员未绑定社区，无法查看用户列表");
            }
            wrapper.eq(SysUser::getCommunityId, operator.getCommunityId());
        }
        if (query.getUsername() != null && !query.getUsername().isBlank()) {
            wrapper.like(SysUser::getUsername, query.getUsername().trim());
        }
        if (query.getRole() != null) {
            wrapper.eq(SysUser::getRole, query.getRole());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (query.getCommunityId() != null
                && operator != null
                && operator.getRole() != null
                && operator.getRole().equals(Constants.ROLE_SUPER_ADMIN)) {
            wrapper.eq(SysUser::getCommunityId, query.getCommunityId());
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> p = sysUserMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, SysRegion> regionMap = p.getRecords().stream()
                .map(SysUser::getCommunityId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.collectingAndThen(Collectors.toList(), ids -> {
                    if (ids.isEmpty()) {
                        return Map.<Long, SysRegion>of();
                    }
                    return sysRegionMapper.selectBatchIds(ids).stream()
                            .collect(Collectors.toMap(SysRegion::getId, x -> x, (a, b) -> a));
                }));
        List<AdminUserVO> records = p.getRecords().stream().map(u -> toVO(u, regionMap)).collect(Collectors.toList());
        return PageResult.of(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public AdminUserVO create(AdminUserCreateRequest request) {
        SysUser operator = currentOperator();
        if (operator != null && operator.getRole() != null && operator.getRole().equals(Constants.ROLE_COMMUNITY_ADMIN)) {
            // 社区管理员禁止创建超管/管理员账号，避免越权
            if (request.getRole() == null || !request.getRole().equals(Constants.ROLE_NORMAL_USER)) {
                throw new RuntimeException("社区管理员只能创建普通用户账号");
            }
        }
        // username 唯一性（未删除用户）
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getIsDeleted, 0)
                .last("LIMIT 1");
        if (sysUserMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordMd5(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        // role=3 才使用 identityType，否则统一设为居民（1）
        if (request.getRole() != null && request.getRole().equals(Constants.ROLE_NORMAL_USER)) {
            user.setIdentityType(IdentityTypeUtil.normalize(
                    request.getIdentityType() == null ? Constants.IDENTITY_RESIDENT : request.getIdentityType()));
        } else {
            user.setIdentityType(Constants.IDENTITY_RESIDENT);
        }
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setAddress(request.getAddress());
        user.setStatus(request.getStatus() == null ? (byte) 1 : request.getStatus());
        // 网格化隔离：社区管理员创建的账号默认绑定到本社区
        if (operator != null && operator.getRole() != null && operator.getRole().equals(Constants.ROLE_COMMUNITY_ADMIN)) {
            if (operator.getCommunityId() == null) {
                throw new RuntimeException("管理员未绑定社区，无法创建用户");
            }
            user.setCommunityId(operator.getCommunityId());
        }
        user.setTimeCoins(0L);
        user.setPoints(0L);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setIsDeleted((byte) 0);
        sysUserMapper.insert(user);
        return toVO(sysUserMapper.selectById(user.getId()), Map.of());
    }

    @Override
    public AdminUserVO update(AdminUserUpdateRequest request) {
        SysUser operator = currentOperator();
        SysUser user = sysUserMapper.selectById(request.getId());
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new RuntimeException("用户不存在");
        }
        assertOperableByCommunityAdmin(operator, user);

        if (request.getRole() != null) {
            if (operator.getRole() != null
                    && operator.getRole().equals(Constants.ROLE_COMMUNITY_ADMIN)
                    && !request.getRole().equals(Constants.ROLE_NORMAL_USER)) {
                throw new RuntimeException("社区管理员只能将用户设置为普通用户");
            }
            user.setRole(request.getRole());
            if (request.getRole().equals(Constants.ROLE_NORMAL_USER)) {
                if (request.getIdentityType() != null) {
                    user.setIdentityType(IdentityTypeUtil.normalize(request.getIdentityType()));
                }
            } else {
                // 非普通用户，identityType 不参与业务，统一回退为居民
                user.setIdentityType(Constants.IDENTITY_RESIDENT);
            }
        } else {
            // role 不变，只有普通用户允许改 identityType
            if (user.getRole() != null && user.getRole().equals(Constants.ROLE_NORMAL_USER) && request.getIdentityType() != null) {
                user.setIdentityType(IdentityTypeUtil.normalize(request.getIdentityType()));
            }
        }

        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return toVO(user, Map.of());
    }

    @Override
    public void delete(Long id) {
        SysUser operator = currentOperator();
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            return;
        }
        assertOperableByCommunityAdmin(operator, user);
        // 逻辑删除
        user.setIsDeleted((byte) 1);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    @Override
    public AdminUserVO setStatus(Long id, Byte status) {
        SysUser operator = currentOperator();
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new RuntimeException("用户不存在");
        }
        assertOperableByCommunityAdmin(operator, user);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return toVO(user, Map.of());
    }

    private void assertOperableByCommunityAdmin(SysUser operator, SysUser targetUser) {
        if (operator == null || operator.getRole() == null) {
            throw new RuntimeException("无权限操作");
        }
        if (operator.getRole().equals(Constants.ROLE_SUPER_ADMIN)) {
            return;
        }
        if (!operator.getRole().equals(Constants.ROLE_COMMUNITY_ADMIN)) {
            throw new RuntimeException("无权限操作用户");
        }
        if (operator.getCommunityId() == null) {
            throw new RuntimeException("管理员未绑定社区，无法操作用户");
        }
        if (!operator.getCommunityId().equals(targetUser.getCommunityId())) {
            throw new RuntimeException("仅可操作本社区用户");
        }
        if (targetUser.getRole() != null && !targetUser.getRole().equals(Constants.ROLE_NORMAL_USER)) {
            throw new RuntimeException("社区管理员仅可操作普通用户");
        }
    }

    private AdminUserVO toVO(SysUser user, Map<Long, SysRegion> regionMap) {
        AdminUserVO vo = new AdminUserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setCommunityId(user.getCommunityId());
        if (user.getCommunityId() != null && regionMap != null) {
            SysRegion region = regionMap.get(user.getCommunityId());
            if (region != null) {
                vo.setCommunityName(region.getName());
            }
        }
        if (user.getRole() != null && user.getRole().equals(Constants.ROLE_NORMAL_USER)) {
            vo.setIdentityType(IdentityTypeUtil.normalize(user.getIdentityType()));
        }
        return vo;
    }
}

