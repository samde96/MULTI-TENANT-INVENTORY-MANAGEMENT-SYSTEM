package com.keen.erp.repo;

import com.keen.erp.entity.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    List<SystemNotification> findTop100ByOrderByCreatedAtDesc();
}
