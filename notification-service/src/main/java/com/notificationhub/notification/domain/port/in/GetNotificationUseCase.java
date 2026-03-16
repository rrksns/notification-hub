package com.notificationhub.notification.domain.port.in;

import com.notificationhub.notification.domain.model.Notification;

public interface GetNotificationUseCase {
    Notification getById(String id, String tenantId);
}
