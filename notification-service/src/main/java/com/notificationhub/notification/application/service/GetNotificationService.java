package com.notificationhub.notification.application.service;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.common.exception.ErrorCode;
import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.port.in.GetNotificationUseCase;
import com.notificationhub.notification.domain.port.out.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class GetNotificationService implements GetNotificationUseCase {

    private final NotificationRepository notificationRepository;

    public GetNotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification getById(String id, String tenantId) {
        return notificationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
