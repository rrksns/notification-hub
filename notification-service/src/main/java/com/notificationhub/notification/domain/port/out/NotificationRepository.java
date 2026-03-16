package com.notificationhub.notification.domain.port.out;

import com.notificationhub.notification.domain.model.Notification;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(String id);
    Optional<Notification> findByIdAndTenantId(String id, String tenantId);
}
