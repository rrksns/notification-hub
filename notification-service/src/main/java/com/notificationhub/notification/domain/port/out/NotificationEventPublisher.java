package com.notificationhub.notification.domain.port.out;

import com.notificationhub.notification.domain.model.Notification;

public interface NotificationEventPublisher {
    void publish(Notification notification);
}
