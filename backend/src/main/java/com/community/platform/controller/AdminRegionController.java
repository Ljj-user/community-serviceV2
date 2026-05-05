package com.community.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.Result;
import com.community.platform.common.Constants;
import com.community.platform.dto.CommunityOptionVO;
import com.community.platform.generated.entity.SysRegion;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.SysRegionMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/region")
public class AdminRegionController {

    @Autowired
    private SysRegionMapper sysRegionMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    @GetMapping("/community-options")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<List<CommunityOptionVO>> communityOptions() {
        SysUser user = currentUser();
        List<CommunityOptionVO> list = new ArrayList<>();
        if (user.getRole() != null && user.getRole().equals(Constants.ROLE_COMMUNITY_ADMIN)) {
            if (user.getCommunityId() == null) {
                return Result.success(List.of());
            }
            SysRegion region = sysRegionMapper.selectById(user.getCommunityId());
            CommunityOptionVO vo = new CommunityOptionVO();
            vo.setId(user.getCommunityId());
            vo.setName(region == null || region.getName() == null || region.getName().isBlank()
                    ? ("社区#" + user.getCommunityId())
                    : region.getName());
            list.add(vo);
            return Result.success(list);
        }

        // 优先取标准社区层级(level=3)
        LambdaQueryWrapper<SysRegion> communityWrapper = new LambdaQueryWrapper<>();
        communityWrapper.eq(SysRegion::getLevel, 3).orderByAsc(SysRegion::getId);
        List<SysRegion> regions = sysRegionMapper.selectList(communityWrapper);

        // 兼容旧库：若 level=3 数据为空，回退为所有区域，避免筛选下拉空白
        if (regions == null || regions.isEmpty()) {
            LambdaQueryWrapper<SysRegion> fallback = new LambdaQueryWrapper<>();
            fallback.orderByAsc(SysRegion::getId);
            regions = sysRegionMapper.selectList(fallback);
        }

        if (regions != null && !regions.isEmpty()) {
            list.addAll(regions.stream().map(r -> {
                CommunityOptionVO vo = new CommunityOptionVO();
                vo.setId(r.getId());
                vo.setName(r.getName());
                return vo;
            }).toList());
        }

        // 再兜底：如果区域表没数据，尝试从用户绑定社区ID反推，保证超管能选社区号
        if (list.isEmpty()) {
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(SysUser::getIsDeleted, 0)
                    .isNotNull(SysUser::getCommunityId)
                    .select(SysUser::getCommunityId);
            List<SysUser> users = sysUserMapper.selectList(userWrapper);
            Map<Long, Long> ids = users.stream()
                    .map(SysUser::getCommunityId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toMap(x -> x, x -> x, (a, b) -> a));
            for (Long id : ids.keySet().stream().sorted().toList()) {
                CommunityOptionVO vo = new CommunityOptionVO();
                vo.setId(id);
                vo.setName("社区#" + id);
                list.add(vo);
            }
        }
        return Result.success(list);
    }

    private SysUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new RuntimeException("未登录");
        }
        SysUser user = sysUserMapper.selectById(userDetails.getUser().getId());
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }
}
