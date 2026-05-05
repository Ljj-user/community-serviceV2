package com.community.platform.config;

import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.AdminAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 审计日志自动采集：系统管理/业务管理相关接口自动落日志
 */
@Component
public class AuditLogFilter extends OncePerRequestFilter {

    @Autowired
    private AdminAuditService adminAuditService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.isBlank()) return true;
        if (path.startsWith("/auth/")) return true;
        if (path.startsWith("/error")) return true;
        if (path.startsWith("/actuator")) return true;
        return !(path.startsWith("/admin/")
                || path.startsWith("/service-request/")
                || path.startsWith("/service-claim/")
                || path.startsWith("/community/announcements/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long begin = System.currentTimeMillis();
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        int status = 200;
        String resultMsg = "OK";
        try {
            filterChain.doFilter(request, response);
            status = response.getStatus();
            resultMsg = status < 400 ? "OK" : ("HTTP_" + status);
        } catch (Exception ex) {
            status = 500;
            resultMsg = ex.getMessage() == null ? "ERROR" : ex.getMessage();
            throw ex;
        } finally {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
                return;
            }
            long elapsed = System.currentTimeMillis() - begin;
            String module = mapModule(requestPath);
            String risk = mapRisk(method, requestPath, status);
            String action = method + " " + requestPath;
            adminAuditService.record(
                    userDetails.getUser().getId(),
                    userDetails.getUser().getUsername(),
                    userDetails.getUser().getRole(),
                    module,
                    action,
                    requestPath,
                    method,
                    status < 400,
                    resultMsg,
                    risk,
                    readClientIp(request),
                    request.getHeader("User-Agent"),
                    (int) elapsed
            );
        }
    }

    private String mapModule(String path) {
        if (path.startsWith("/admin/config")) return "SYSTEM_CONFIG";
        if (path.startsWith("/admin/users")) return "USER_MANAGE";
        if (path.startsWith("/admin/audit")) return "AUDIT_LOG";
        if (path.startsWith("/admin/export")) return "DATA_EXPORT";
        if (path.startsWith("/admin/invite-code")) return "INVITE_CODE";
        if (path.startsWith("/admin/banner")) return "BANNER_MANAGE";
        if (path.startsWith("/service-request/audit")) return "REQUEST_AUDIT";
        if (path.startsWith("/service-request/monitor")) return "SERVICE_MONITOR";
        if (path.startsWith("/service-request/")) return "SERVICE_REQUEST";
        if (path.startsWith("/service-claim/")) return "SERVICE_FLOW";
        if (path.startsWith("/community/announcements/")) return "ANNOUNCEMENT";
        return "SYSTEM";
    }

    private String mapRisk(String method, String path, int status) {
        if (status >= 500) return "HIGH";
        if ("DELETE".equalsIgnoreCase(method)) return "HIGH";
        if (path.startsWith("/admin/export")) return "WARN";
        if (path.contains("/status") || path.contains("/disable") || path.contains("/restore")) return "WARN";
        return "NORMAL";
    }

    private String readClientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}
