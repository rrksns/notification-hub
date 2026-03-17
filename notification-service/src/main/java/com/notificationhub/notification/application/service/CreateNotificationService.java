package com.notificationhub.notification.application.service;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.common.exception.ErrorCode;
import com.notificationhub.notification.domain.model.Channel;
import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.port.in.CreateNotificationUseCase;
import com.notificationhub.notification.domain.port.out.IdempotencyPort;
import com.notificationhub.notification.domain.port.out.NotificationEventPublisher;
import com.notificationhub.notification.domain.port.out.NotificationRepository;
import com.notificationhub.notification.infrastructure.metrics.NotificationMetrics;
import org.springframework.stereotype.Service;

@Service
public class CreateNotificationService implements CreateNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final IdempotencyPort idempotencyPort;
    private final NotificationEventPublisher eventPublisher;
    private final NotificationMetrics metrics;

    public CreateNotificationService(NotificationRepository notificationRepository,
                                     IdempotencyPort idempotencyPort,
                                     NotificationEventPublisher eventPublisher,
                                     NotificationMetrics metrics) {
        this.notificationRepository = notificationRepository;
        this.idempotencyPort = idempotencyPort;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Override
    public Result create(Command command) {
        if (idempotencyPort.isDuplicate(command.idempotencyKey())) {
            metrics.incrementDuplicate();
            throw new BusinessException(ErrorCode.DUPLICATE_NOTIFICATION);
        }

        Notification notification = Notification.create(
                command.tenantId(),
                Channel.from(command.channel()),
                command.recipient(),
                command.content(),
                command.idempotencyKey()
        );

        notification.publish();
        Notification saved = notificationRepository.save(notification);

        idempotencyPort.save(command.idempotencyKey());
        eventPublisher.publish(saved);
        metrics.incrementSent();

        return new Result(saved.getId(), saved.getStatus().name());
    }
}
