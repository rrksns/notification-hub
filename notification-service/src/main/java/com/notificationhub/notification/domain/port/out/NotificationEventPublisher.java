package com.notificationhub.notification.domain.port.out;

import com.notificationhub.common.event.NotificationEvent;

public interface NotificationEventPublisher {
    void publish(NotificationEvent event);
}
