package com.keen.erp.service;

import com.keen.erp.entity.AppUser;
import com.keen.erp.entity.NotificationType;
import com.keen.erp.entity.SystemNotification;
import com.keen.erp.repo.SystemNotificationRepository;
import com.keen.erp.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final SystemNotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(SystemNotificationRepository notificationRepository,
                               CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public SystemNotification create(String recipient,
                                     String recipientRole,
                                     NotificationType type,
                                     String channel,
                                     String title,
                                     String message,
                                     String referenceNumber) {
        SystemNotification notification = new SystemNotification();
        notification.setRecipient(recipient);
        notification.setRecipientRole(recipientRole);
        notification.setType(type);
        notification.setChannel(channel);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceNumber(referenceNumber);
        return notificationRepository.save(notification);
    }

    @Transactional
    public SystemNotification notifyRole(String recipientRole,
                                         NotificationType type,
                                         String title,
                                         String message,
                                         String referenceNumber) {
        return create(null, recipientRole, type, "EMAIL", title, message, referenceNumber);
    }

    public List<SystemNotification> recent() {
        currentUserService.requirePermission("notifications:view");
        AppUser user = currentUserService.currentUser();
        return notificationRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(notification -> isVisibleToUser(notification, user))
                .toList();
    }

    @Transactional
    public SystemNotification markRead(Long notificationId) {
        currentUserService.requirePermission("notifications:view");
        AppUser user = currentUserService.currentUser();
        SystemNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Notification not found"));
        if (!isVisibleToUser(notification, user)) {
            throw new AccessDeniedException("Notification access denied");
        }
        notification.setReadFlag(true);
        return notificationRepository.save(notification);
    }

    public long countUnreadVisible() {
        AppUser user = currentUserService.currentUser();
        return notificationRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(notification -> isVisibleToUser(notification, user))
                .filter(notification -> !notification.isReadFlag())
                .count();
    }

    private boolean isVisibleToUser(SystemNotification notification, AppUser user) {
        if (user == null || user.isAdmin()) {
            return true;
        }
        if (notification.getRecipient() != null && notification.getRecipient().equalsIgnoreCase(user.getUsername())) {
            return true;
        }
        if (notification.getRecipientRole() != null) {
            return user.getRoles().stream()
                    .filter(role -> role != null && role.getCode() != null)
                    .anyMatch(role -> role.getCode().equalsIgnoreCase(notification.getRecipientRole()));
        }
        return notification.getRecipient() == null && notification.getRecipientRole() == null;
    }
}
