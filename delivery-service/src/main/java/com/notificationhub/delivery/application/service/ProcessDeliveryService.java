package com.notificationhub.delivery.application.service;

import com.notificationhub.delivery.domain.model.ChannelType;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.model.DeliveryResultOutbox;
import com.notificationhub.delivery.domain.port.in.ProcessDeliveryUseCase;
import com.notificationhub.delivery.domain.port.out.ChannelDelivererPort;
import com.notificationhub.delivery.domain.port.out.DeliveryLogRepository;
import com.notificationhub.delivery.domain.port.out.DeliveryResultOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProcessDeliveryService implements ProcessDeliveryUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessDeliveryService.class);

    private final DeliveryLogRepository deliveryLogRepository;
    private final ChannelDelivererPort channelDelivererPort;
    private final DeliveryResultOutboxRepository deliveryResultOutboxRepository;

    public ProcessDeliveryService(DeliveryLogRepository deliveryLogRepository,
                                  ChannelDelivererPort channelDelivererPort,
                                  DeliveryResultOutboxRepository deliveryResultOutboxRepository) {
        this.deliveryLogRepository = deliveryLogRepository;
        this.channelDelivererPort = channelDelivererPort;
        this.deliveryResultOutboxRepository = deliveryResultOutboxRepository;
    }

    @Override
    @Transactional
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

        DeliveryLog finalLog;
        try {
            channelDelivererPort.deliver(channelType, command.recipient(), command.content());
            finalLog = deliveryLog.markSuccess();
            deliveryLogRepository.save(finalLog);
        } catch (Exception e) {
            finalLog = deliveryLog.markFailed(e.getMessage());
            deliveryLogRepository.save(finalLog);
        }

        deliveryResultOutboxRepository.save(DeliveryResultOutbox.from(finalLog));

        return new Result(finalLog.getId(), finalLog.getStatus().name());
    }
}
