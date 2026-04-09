package com.notificationhub.notification.application.service;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.common.exception.ErrorCode;
import com.notificationhub.notification.domain.model.Channel;
import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.port.in.CreateNotificationUseCase;
import com.notificationhub.notification.domain.port.out.IdempotencyPort;
import com.notificationhub.notification.domain.port.out.NotificationApplicationMetrics;
import com.notificationhub.notification.domain.port.out.NotificationEventPublisher;
import com.notificationhub.notification.domain.port.out.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateNotificationService implements CreateNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final IdempotencyPort idempotencyPort;
    private final NotificationEventPublisher eventPublisher;
    private final NotificationApplicationMetrics metrics;

    public CreateNotificationService(NotificationRepository notificationRepository,
                                     IdempotencyPort idempotencyPort,
                                     NotificationEventPublisher eventPublisher,
                                     NotificationApplicationMetrics metrics) {
        this.notificationRepository = notificationRepository;
        this.idempotencyPort = idempotencyPort;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Override
    @Transactional
    public Result create(Command command) {
        if (idempotencyPort.isDuplicate(command.tenantId(), command.idempotencyKey())) {
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

        Notification published = notification.publish();
        Notification saved = notificationRepository.save(published);

        // NOTE: idempotencyPort(Redis)와 eventPublisher(Kafka) 모두 JPA 트랜잭션 밖에서 실행됩니다.
        // idempotencyPort.save() 성공 후 eventPublisher.publish()가 실패하면
        // DB는 롤백되지만 Redis 키는 남아 재시도가 불가능해집니다.
        // 프로덕션에서는 Transactional Outbox Pattern 적용이 필요합니다.
        idempotencyPort.save(command.tenantId(), command.idempotencyKey());
        eventPublisher.publish(saved);
        metrics.incrementSent();

        return new Result(saved.getId(), saved.getStatus().name());
    }
}
