package com.community.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.Constants;
import com.community.platform.generated.entity.AuditLog;
import com.community.platform.generated.entity.ServiceRequest;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.AuditLogMapper;
import com.community.platform.generated.mapper.ServiceRequestMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;

/**
 * 超级管理员：数据导出（简化版：先输出 CSV）
 */
@RestController
@RequestMapping("/admin/export")
public class AdminExportController {

    @Autowired
    private ServiceRequestMapper serviceRequestMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/{module}")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public void export(@PathVariable("module") String module,
                       @RequestParam(value = "format", required = false) String format,
                       @RequestParam(value = "startTime", required = false) String startTime,
                       @RequestParam(value = "endTime", required = false) String endTime,
                       HttpServletResponse response) throws IOException {
        SysUser operator = currentOperator();
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        boolean pdf = "pdf".equalsIgnoreCase(format);
        String filename = module + "-" + ts + (pdf ? ".pdf" : ".csv");

        String csv;
        if ("service_request".equalsIgnoreCase(module)) {
            csv = exportServiceRequest(startTime, endTime, operator);
        } else if ("service_monitor".equalsIgnoreCase(module)) {
            csv = exportServiceMonitor(operator);
        } else if ("users".equalsIgnoreCase(module)) {
            csv = exportUsers(startTime, endTime, operator);
        } else if ("volunteers".equalsIgnoreCase(module)) {
            csv = exportVolunteers(startTime, endTime, operator);
        } else if ("audit".equalsIgnoreCase(module)) {
            csv = exportAudit(startTime, endTime, operator);
        } else if ("invite_code".equalsIgnoreCase(module)) {
            csv = exportInviteCode(operator);
        } else if ("banner".equalsIgnoreCase(module)) {
            csv = exportBanner(operator);
        } else if ("announcement".equalsIgnoreCase(module)) {
            csv = exportAnnouncement(operator);
        } else {
            csv = "error,unknown_module\n";
        }

        if (pdf) {
            byte[] bytes = buildPdf(csv, module + " 导出");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + urlEncode(filename) + "\"");
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
            return;
        }
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + urlEncode(filename) + "\"");
        response.getWriter().write("\uFEFF"); // Excel 友好 CSV
        response.getWriter().write(csv);
        response.getWriter().flush();
    }

    private String exportServiceRequest(String startTime, String endTime, SysUser operator) {
        LambdaQueryWrapper<ServiceRequest> w = new LambdaQueryWrapper<>();
        w.eq(ServiceRequest::getIsDeleted, 0);
        if (isCommunityAdmin(operator)) {
            w.eq(ServiceRequest::getCommunityId, operator.getCommunityId());
        }
        applyTimeRange(w, ServiceRequest::getCreatedAt, startTime, endTime);
        w.orderByDesc(ServiceRequest::getCreatedAt);
        List<ServiceRequest> list = serviceRequestMapper.selectList(w);

        StringBuilder sb = new StringBuilder();
        sb.append("id,requesterUserId,serviceType,serviceAddress,urgencyLevel,status,createdAt\n");
        for (ServiceRequest r : list) {
            sb.append(n(r.getId())).append(',')
                    .append(n(r.getRequesterUserId())).append(',')
                    .append(q(r.getServiceType())).append(',')
                    .append(q(r.getServiceAddress())).append(',')
                    .append(n(r.getUrgencyLevel())).append(',')
                    .append(n(r.getStatus())).append(',')
                    .append(q(String.valueOf(r.getCreatedAt())))
                    .append('\n');
        }
        return sb.toString();
    }

    private String exportUsers(String startTime, String endTime, SysUser operator) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getIsDeleted, 0);
        if (isCommunityAdmin(operator)) {
            w.eq(SysUser::getCommunityId, operator.getCommunityId());
        }
        applyTimeRange(w, SysUser::getCreatedAt, startTime, endTime);
        w.orderByDesc(SysUser::getCreatedAt);
        List<SysUser> list = sysUserMapper.selectList(w);

        StringBuilder sb = new StringBuilder();
        sb.append("id,username,role,identityType,realName,phone,email,status,createdAt\n");
        for (SysUser u : list) {
            sb.append(n(u.getId())).append(',')
                    .append(q(u.getUsername())).append(',')
                    .append(n(u.getRole())).append(',')
                    .append(n(u.getIdentityType())).append(',')
                    .append(q(u.getRealName())).append(',')
                    .append(q(u.getPhone())).append(',')
                    .append(q(u.getEmail())).append(',')
                    .append(n(u.getStatus())).append(',')
                    .append(q(String.valueOf(u.getCreatedAt())))
                    .append('\n');
        }
        return sb.toString();
    }

    private String exportVolunteers(String startTime, String endTime, SysUser operator) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getIsDeleted, 0)
                .eq(SysUser::getRole, Constants.ROLE_NORMAL_USER)
                .eq(SysUser::getIdentityType, Constants.IDENTITY_VOLUNTEER);
        if (isCommunityAdmin(operator)) {
            w.eq(SysUser::getCommunityId, operator.getCommunityId());
        }
        applyTimeRange(w, SysUser::getCreatedAt, startTime, endTime);
        w.orderByDesc(SysUser::getCreatedAt);
        List<SysUser> list = sysUserMapper.selectList(w);
        StringBuilder sb = new StringBuilder();
        sb.append("id,username,realName,phone,email,communityId,status,createdAt\n");
        for (SysUser u : list) {
            sb.append(n(u.getId())).append(',')
                    .append(q(u.getUsername())).append(',')
                    .append(q(u.getRealName())).append(',')
                    .append(q(u.getPhone())).append(',')
                    .append(q(u.getEmail())).append(',')
                    .append(n(u.getCommunityId())).append(',')
                    .append(n(u.getStatus())).append(',')
                    .append(q(String.valueOf(u.getCreatedAt())))
                    .append('\n');
        }
        return sb.toString();
    }

    private String exportAudit(String startTime, String endTime, SysUser operator) {
        LambdaQueryWrapper<AuditLog> w = new LambdaQueryWrapper<>();
        if (isCommunityAdmin(operator)) {
            w.inSql(AuditLog::getUserId,
                    "SELECT id FROM sys_user WHERE is_deleted=0 AND community_id=" + operator.getCommunityId());
        }
        applyTimeRange(w, AuditLog::getCreatedAt, startTime, endTime);
        w.orderByDesc(AuditLog::getCreatedAt);
        List<AuditLog> list = auditLogMapper.selectList(w);

        StringBuilder sb = new StringBuilder();
        sb.append("id,username,role,module,action,success,riskLevel,ip,createdAt\n");
        for (AuditLog a : list) {
            sb.append(n(a.getId())).append(',')
                    .append(q(a.getUsername())).append(',')
                    .append(n(a.getRole())).append(',')
                    .append(q(a.getModule())).append(',')
                    .append(q(a.getAction())).append(',')
                    .append(n(a.getSuccess())).append(',')
                    .append(q(a.getRiskLevel())).append(',')
                    .append(q(a.getIp())).append(',')
                    .append(q(String.valueOf(a.getCreatedAt())))
                    .append('\n');
        }
        return sb.toString();
    }

    private String exportInviteCode(SysUser operator) {
        String sql = """
                SELECT c.community_id, r.name community_name, c.code, c.status, c.used_count, c.max_uses, c.expires_at, c.created_at
                FROM community_invite_code c
                LEFT JOIN sys_region r ON r.id = c.community_id
                WHERE (? IS NULL OR c.community_id = ?)
                ORDER BY c.id DESC
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, scopeCommunity(operator), scopeCommunity(operator));
        StringBuilder sb = new StringBuilder();
        sb.append("communityId,communityName,code,status,usedCount,maxUses,expiresAt,createdAt\n");
        for (Map<String, Object> x : rows) {
            sb.append(n(x.get("community_id"))).append(',')
                    .append(q(String.valueOf(x.get("community_name")))).append(',')
                    .append(q(String.valueOf(x.get("code")))).append(',')
                    .append(n(x.get("status"))).append(',')
                    .append(n(x.get("used_count"))).append(',')
                    .append(n(x.get("max_uses"))).append(',')
                    .append(q(String.valueOf(x.get("expires_at")))).append(',')
                    .append(q(String.valueOf(x.get("created_at"))))
                    .append('\n');
        }
        return sb.toString();
    }

    private String exportBanner(SysUser operator) {
        String sql = """
                SELECT b.community_id, r.name community_name, b.title, b.subtitle, b.image_url, b.link_url, b.status, b.created_at
                FROM community_banner b
                LEFT JOIN sys_region r ON r.id = b.community_id
                WHERE (? IS NULL OR b.community_id = ?)
                ORDER BY b.sort_no ASC, b.id DESC
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, scopeCommunity(operator), scopeCommunity(operator));
        StringBuilder sb = new StringBuilder();
        sb.append("communityId,communityName,title,subtitle,imageUrl,linkUrl,status,createdAt\n");
        for (Map<String, Object> x : rows) {
            sb.append(n(x.get("community_id"))).append(',')
                    .append(q(String.valueOf(x.get("community_name")))).append(',')
                    .append(q(String.valueOf(x.get("title")))).append(',')
                    .append(q(String.valueOf(x.get("subtitle")))).append(',')
                    .append(q(String.valueOf(x.get("image_url")))).append(',')
                    .append(q(String.valueOf(x.get("link_url")))).append(',')
                    .append(n(x.get("status"))).append(',')
                    .append(q(String.valueOf(x.get("created_at"))))
                    .append('\n');
        }
        return sb.toString();
    }

    private String exportAnnouncement(SysUser operator) {
        String sql = """
                SELECT a.target_community_id, r.name community_name, a.title, a.target_scope, a.status, a.is_top, a.published_at, a.created_at
                FROM announcement a
                LEFT JOIN sys_region r ON r.id = a.target_community_id
                WHERE a.is_deleted=0
                  AND (? IS NULL OR a.target_community_id = ?)
                ORDER BY a.created_at DESC
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, scopeCommunity(operator), scopeCommunity(operator));
        StringBuilder sb = new StringBuilder();
        sb.append("communityId,communityName,title,targetScope,status,isTop,publishedAt,createdAt\n");
        for (Map<String, Object> x : rows) {
            sb.append(n(x.get("target_community_id"))).append(',')
                    .append(q(String.valueOf(x.get("community_name")))).append(',')
                    .append(q(String.valueOf(x.get("title")))).append(',')
                    .append(n(x.get("target_scope"))).append(',')
                    .append(n(x.get("status"))).append(',')
                    .append(n(x.get("is_top"))).append(',')
                    .append(q(String.valueOf(x.get("published_at")))).append(',')
                    .append(q(String.valueOf(x.get("created_at"))))
                    .append('\n');
        }
        return sb.toString();
    }

    private String exportServiceMonitor(SysUser operator) {
        String sql = """
                SELECT r.id request_id, r.community_id, sr.name community_name, r.service_type, r.service_address, r.expected_time, r.status, r.created_at
                FROM service_request r
                LEFT JOIN sys_region sr ON sr.id = r.community_id
                WHERE r.is_deleted=0
                  AND r.status IN (1,2)
                  AND r.expected_time < NOW()
                  AND (? IS NULL OR r.community_id = ?)
                ORDER BY r.expected_time ASC
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, scopeCommunity(operator), scopeCommunity(operator));
        StringBuilder sb = new StringBuilder();
        sb.append("requestId,communityId,communityName,serviceType,serviceAddress,expectedTime,status,createdAt\n");
        for (Map<String, Object> x : rows) {
            sb.append(n(x.get("request_id"))).append(',')
                    .append(n(x.get("community_id"))).append(',')
                    .append(q(String.valueOf(x.get("community_name")))).append(',')
                    .append(q(String.valueOf(x.get("service_type")))).append(',')
                    .append(q(String.valueOf(x.get("service_address")))).append(',')
                    .append(q(String.valueOf(x.get("expected_time")))).append(',')
                    .append(n(x.get("status"))).append(',')
                    .append(q(String.valueOf(x.get("created_at"))))
                    .append('\n');
        }
        return sb.toString();
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    }

    private static String q(String s) {
        if (s == null || "null".equalsIgnoreCase(s)) return "";
        String v = s.replace("\"", "\"\"");
        return "\"" + v + "\"";
    }

    private static String n(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static <T> void applyTimeRange(LambdaQueryWrapper<T> w,
                                           com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, LocalDateTime> col,
                                           String startTime,
                                           String endTime) {
        LocalDateTime start = parseIso(startTime);
        LocalDateTime end = parseIso(endTime);
        if (start != null) w.ge(col, start);
        if (end != null) w.le(col, end);
    }

    private static LocalDateTime parseIso(String iso) {
        if (iso == null || iso.isBlank()) return null;
        // 兼容前端传入的 toISOString：2026-03-03T12:34:56.789Z
        String s = iso.replace("Z", "");
        if (s.length() >= 19) s = s.substring(0, 19);
        try {
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private SysUser currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new RuntimeException("未登录");
        }
        return sysUserMapper.selectById(userDetails.getUser().getId());
    }

    private boolean isCommunityAdmin(SysUser user) {
        return user != null && user.getRole() != null && user.getRole().equals(Constants.ROLE_COMMUNITY_ADMIN);
    }

    private Long scopeCommunity(SysUser operator) {
        return isCommunityAdmin(operator) ? operator.getCommunityId() : null;
    }

    private byte[] buildPdf(String csv, String title) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph(title, new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph(" "));
        String[] lines = csv.split("\\r?\\n");
        if (lines.length == 0) {
            document.close();
            return out.toByteArray();
        }
        String[] headers = splitCsvLine(lines[0]);
        PdfPTable table = new PdfPTable(Math.max(1, headers.length));
        table.setWidthPercentage(100);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h));
            table.addCell(cell);
        }
        for (int i = 1; i < lines.length; i++) {
            if (lines[i] == null || lines[i].isBlank()) continue;
            String[] cols = splitCsvLine(lines[i]);
            for (String c : cols) {
                table.addCell(new Phrase(c));
            }
            for (int j = cols.length; j < headers.length; j++) {
                table.addCell(new Phrase(""));
            }
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private String[] splitCsvLine(String line) {
        if (line == null) return new String[0];
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                result.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        result.add(cur.toString());
        return result.toArray(new String[0]);
    }
}

