package com.notificationhub.notification.application.service;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.common.exception.ErrorCode;
import com.notificationhub.notification.domain.model.Channel;
import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.port.in.CreateNotificationUseCase;
import com.notificationhub.notification.domain.port.out.IdempotencyPort;
import com.notificationhub.notification.domain.port.out.NotificationApplicationMetrics;
import com.notificationhub.notification.domain.port.out.NotificationOutboxPort;
import com.notificationhub.notification.domain.port.out.NotificationRepository;
import com.notificationhub.notification.domain.port.out.NotificationQuotaPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateNotificationService implements CreateNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final IdempotencyPort idempotencyPort;
    private final NotificationOutboxPort outboxPort;
    private final NotificationApplicationMetrics metrics;
    private final NotificationQuotaPort quotaPort;

    public CreateNotificationService(NotificationRepository notificationRepository,
                                     IdempotencyPort idempotencyPort,
                                     NotificationOutboxPort outboxPort,
                                     NotificationApplicationMetrics metrics,
                                     NotificationQuotaPort quotaPort) {
        this.notificationRepository = notificationRepository;
        this.idempotencyPort = idempotencyPort;
        this.outboxPort = outboxPort;
        this.metrics = metrics;
        this.quotaPort = quotaPort;
    }

    @Override
    @Transactional
    public Result create(Command command) {
        if (idempotencyPort.isDuplicate(command.tenantId(), command.idempotencyKey())) {
            metrics.incrementDuplicate();
            throw new BusinessException(ErrorCode.DUPLICATE_NOTIFICATION);
        }

        if (!quotaPort.tryConsume(command.tenantId(), command.plan())) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED);
        }

        Notification notification = Notification.create(
                command.tenantId(),
                Channel.from(command.channel()),
                command.recipient(),
                command.content(),
                command.idempotencyKey()
        );

        Notification published = notification.publish();
        Notification saved = notificationRepository.save(published);

        idempotencyPort.save(command.tenantId(), command.idempotencyKey());
        outboxPort.save(saved);
        metrics.incrementSent();

        return new Result(saved.getId(), saved.getStatus().name());
    }
}
