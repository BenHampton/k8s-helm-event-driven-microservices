package com.k8sdemo.notification.repository;

import com.k8sdemo.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    boolean existsByOrderId(Long orderId);
}
