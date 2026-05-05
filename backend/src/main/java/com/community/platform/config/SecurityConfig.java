package com.community.platform.config;

import com.community.platform.common.Result;
import com.community.platform.security.HybridPasswordEncoder;
import com.community.platform.security.TokenFilter;
import com.community.platform.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * 安全配置
 * 
 * 按需求文档实现：
 * - 基于 sys_user 的登录/注册
 * - 角色：1超级管理员 2社区管理员 3普通用户（身份为居民老人或志愿者，互斥）
 * - 基于角色的接口权限控制
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private TokenFilter tokenFilter;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 密码加密器（新写入使用 BCrypt，登录兼容历史 MD5）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new HybridPasswordEncoder();
    }

    /**
     * 认证提供者
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 开发联调：允许管理端（7000）与移动端 dev server 等来源跨域访问 API（生产环境建议收紧为固定域名）
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:7000",
                "http://127.0.0.1:7000",
                "http://localhost:9000",
                "http://127.0.0.1:9000",
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 未认证异常处理器
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Result<?> result = Result.error(401, "未登录或登录已过期，请重新登录");
            response.getWriter().write(objectMapper.writeValueAsString(result));
        };
    }

    /**
     * 权限不足异常处理器
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            Result<?> result = Result.error(403, "没有权限访问该资源");
            response.getWriter().write(objectMapper.writeValueAsString(result));
        };
    }

    /**
     * 安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 公开接口
                        .requestMatchers("/auth/login", "/auth/register", "/auth/verification/send").permitAll()
                        // 普通用户加入/改绑社区（邀请码/扫码）
                        .requestMatchers("/community/invite/verify", "/community/join").hasAnyRole("USER", "COMMUNITY_ADMIN", "SUPER_ADMIN")
                        // 普通用户：首页轮播图
                        .requestMatchers("/user/banner/**").hasAnyRole("USER", "COMMUNITY_ADMIN", "SUPER_ADMIN")
                        // 静态资源（头像）
                        .requestMatchers("/static/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        
                        // 超级管理员：系统配置、备份与导出等高危操作
                        .requestMatchers("/admin/config/**", "/admin/backup/**", "/admin/export/**").hasRole("SUPER_ADMIN")

                        // 用户与角色管理（按业务再做社区范围约束）
                        .requestMatchers("/admin/users/**").hasAnyRole("COMMUNITY_ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/admin/community-join/**", "/admin/volunteer/**", "/admin/care-subject/**", "/admin/convenience-info/**", "/admin/alerts/**", "/admin/ai-analysis/**").hasAnyRole("COMMUNITY_ADMIN", "SUPER_ADMIN")
                        // 社区邀请码（社区管理员/系统管理员）
                        .requestMatchers("/admin/invite-code/**").hasAnyRole("COMMUNITY_ADMIN", "SUPER_ADMIN")
                        // 轮播图管理（社区管理员/系统管理员）
                        .requestMatchers("/admin/banner/**").hasAnyRole("COMMUNITY_ADMIN", "SUPER_ADMIN")
                        // 审计日志仅系统管理员可见
                        .requestMatchers("/admin/audit/**").hasAnyRole("COMMUNITY_ADMIN", "SUPER_ADMIN")

                        // 社区管理员：审核需求、管理志愿者、数据看板
                        .requestMatchers("/community/**", "/dashboard/**").hasAnyRole("COMMUNITY_ADMIN", "SUPER_ADMIN")
                        
                        // 普通用户：发布需求、认领服务、评价、个人待办、全局点赞
                        .requestMatchers("/app/runtime").hasAnyRole("USER", "COMMUNITY_ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/user/**", "/volunteer/**", "/convenience-info/**", "/request/**", "/service/**", "/service-request/**", "/service-claim/**", "/service-evaluation/**", "/support/**", "/ai/**").hasAnyRole("USER", "COMMUNITY_ADMIN", "SUPER_ADMIN")
                        
                        // 其他接口需要认证
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
