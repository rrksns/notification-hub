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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CreateNotificationService implements CreateNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateNotificationService.class);

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

        boolean idempotencyMayExist = false;
        try {
            Notification notification = Notification.create(
                    command.tenantId(),
                    Channel.from(command.channel()),
                    command.recipient(),
                    command.content(),
                    command.idempotencyKey()
            );

            Notification published = notification.publish();
            Notification saved = notificationRepository.save(published);

            idempotencyMayExist = true;
            idempotencyPort.save(command.tenantId(), command.idempotencyKey());
            outboxPort.save(saved);
            metrics.incrementSent();

            return new Result(saved.getId(), saved.getStatus().name());
        } catch (RuntimeException e) {
            compensateRedisState(command, idempotencyMayExist, e);
            throw e;
        }
    }

    private void compensateRedisState(Command command, boolean idempotencyMayExist, RuntimeException cause) {
        if (idempotencyMayExist) {
            try {
                idempotencyPort.delete(command.tenantId(), command.idempotencyKey());
            } catch (RuntimeException cleanupFailure) {
                log.error("Failed to compensate notification idempotency key: tenantId={}, idempotencyKey={}",
                        command.tenantId(), command.idempotencyKey(), cleanupFailure);
            }
        }
        try {
            quotaPort.release(command.tenantId(), command.plan());
        } catch (RuntimeException cleanupFailure) {
            log.error("Failed to compensate notification quota: tenantId={}, plan={}",
                    command.tenantId(), command.plan(), cleanupFailure);
        }
        log.warn("Notification transaction failed after Redis quota consumption: tenantId={}", command.tenantId(), cause);
    }
}
