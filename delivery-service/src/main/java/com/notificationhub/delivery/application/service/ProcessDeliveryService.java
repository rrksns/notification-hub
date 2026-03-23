package com.notificationhub.delivery.application.service;

import com.notificationhub.delivery.domain.model.ChannelType;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.port.in.ProcessDeliveryUseCase;
import com.notificationhub.delivery.domain.port.out.ChannelDelivererPort;
import com.notificationhub.delivery.domain.port.out.DeliveryLogRepository;
import com.notificationhub.delivery.domain.port.out.DeliveryResultPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessDeliveryService implements ProcessDeliveryUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessDeliveryService.class);

    private final DeliveryLogRepository deliveryLogRepository;
    private final ChannelDelivererPort channelDelivererPort;
    private final DeliveryResultPublisher deliveryResultPublisher;

    public ProcessDeliveryService(DeliveryLogRepository deliveryLogRepository,
                                  ChannelDelivererPort channelDelivererPort,
                                  DeliveryResultPublisher deliveryResultPublisher) {
        this.deliveryLogRepository = deliveryLogRepository;
        this.channelDelivererPort = channelDelivererPort;
        this.deliveryResultPublisher = deliveryResultPublisher;
    }

    @Override
    public Result process(Command command) {
        List<DeliveryLog> existing = deliveryLogRepository.findByNotificationId(command.notificationId());
        if (!existing.isEmpty()) {
            log.warn("Duplicate delivery skipped: notificationId={}", command.notificationId());
            DeliveryLog first = existing.get(0);
            return new Result(first.getId(), first.getStatus().name());
        }

        ChannelType channelType = ChannelType.from(command.channel());

        DeliveryLog deliveryLog = DeliveryLog.create(
                command.notificationId(),
                command.tenantId(),
                channelType,
                command.recipient()
        );
        deliveryLogRepository.save(deliveryLog);

        try {
            channelDelivererPort.deliver(channelType, command.recipient(), command.content());
            deliveryLog.markSuccess();
            deliveryLogRepository.save(deliveryLog);
            deliveryResultPublisher.publishSuccess(deliveryLog);
        } catch (Exception e) {
            deliveryLog.markFailed(e.getMessage());
            deliveryLogRepository.save(deliveryLog);
            deliveryResultPublisher.publishFailure(deliveryLog);
        }

        return new Result(deliveryLog.getId(), deliveryLog.getStatus().name());
    }
}
