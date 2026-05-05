package com.community.platform.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security UserDetailsService 实现
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username)
               .eq(SysUser::getIsDeleted, 0)
               .last("LIMIT 1");  // 确保只返回一条记录
        
        SysUser user = sysUserMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        
        return new UserDetailsImpl(user);
    }
}
