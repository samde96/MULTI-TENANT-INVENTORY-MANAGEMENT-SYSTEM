package com.keen.erp.controller;

import com.keen.erp.entity.AuditLog;
import com.keen.erp.entity.SystemNotification;
import com.keen.erp.repo.AuditLogRepository;
import com.keen.erp.service.NotificationService;
import com.keen.erp.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public AuditController(AuditLogRepository auditLogRepository,
                           NotificationService notificationService,
                           CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> auditLogs() {
        currentUserService.requirePermission("audit:view");
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @GetMapping("/notifications")
    public List<SystemNotification> notifications() {
        currentUserService.requirePermission("notifications:view");
        return notificationService.recent();
    }

    @PatchMapping("/notifications/{id}/read")
    public SystemNotification markRead(@PathVariable Long id) {
        currentUserService.requirePermission("notifications:view");
        return notificationService.markRead(id);
    }
}
