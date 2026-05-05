package com.community.platform.security;

import com.community.platform.generated.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security UserDetails 实现
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
    
    private SysUser user;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 根据角色返回权限
        String roleName = getRoleName(user.getRole());
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
    }
    
    @Override
    public String getPassword() {
        return user.getPasswordMd5();
    }
    
    @Override
    public String getUsername() {
        return user.getUsername();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return user.getStatus() != null && user.getStatus() == 1;
    }
    
    /**
     * 获取角色名称
     */
    private String getRoleName(Byte role) {
        if (role == null) {
            return "USER";
        }
        switch (role) {
            case 1:
                return "SUPER_ADMIN";
            case 2:
                return "COMMUNITY_ADMIN";
            case 3:
                return "USER";
            default:
                return "USER";
        }
    }
}
