package com.keen.erp.service;

import com.keen.erp.entity.AppUser;
import com.keen.erp.entity.AuditLog;
import com.keen.erp.repo.AuditLogRepository;
import com.keen.erp.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public AuditService(AuditLogRepository auditLogRepository, CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    public AuditLog record(String action,
                           String entityType,
                           String entityId,
                           String oldValue,
                           String newValue,
                           boolean success) {
        AuditLog log = new AuditLog();
        try {
            AppUser user = currentUserService.currentUser();
            log.setUsername(user.getUsername());
            log.setRole(user.getRoles().stream()
                    .map(role -> role.getCode())
                    .findFirst()
                    .orElse("UNKNOWN"));
        } catch (RuntimeException ex) {
            log.setUsername("SYSTEM");
            log.setRole("SYSTEM");
        }
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setSuccess(success);

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.setIpAddress(resolveIpAddress(request));
            log.setUserAgent(request.getHeader("User-Agent"));
        }
        return auditLogRepository.save(log);
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return Objects.toString(request.getRemoteAddr(), "unknown");
    }
}
