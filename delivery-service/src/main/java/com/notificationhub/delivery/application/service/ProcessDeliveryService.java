package com.notificationhub.delivery.application.service;

import com.notificationhub.delivery.domain.model.ChannelType;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.port.in.ProcessDeliveryUseCase;
import com.notificationhub.delivery.domain.port.out.ChannelDelivererPort;
import com.notificationhub.delivery.domain.port.out.DeliveryLogRepository;
import com.notificationhub.delivery.domain.port.out.DeliveryResultPublisher;
import org.springframework.stereotype.Service;

@Service
public class ProcessDeliveryService implements ProcessDeliveryUseCase {

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
        ChannelType channelType = ChannelType.from(command.channel());

        DeliveryLog log = DeliveryLog.create(
                command.notificationId(),
                command.tenantId(),
                channelType,
                command.recipient()
        );
        deliveryLogRepository.save(log);

        try {
            channelDelivererPort.deliver(channelType, command.recipient(), command.content());
            log.markSuccess();
            deliveryLogRepository.save(log);
            deliveryResultPublisher.publishSuccess(log);
        } catch (Exception e) {
            log.markFailed(e.getMessage());
            deliveryLogRepository.save(log);
            deliveryResultPublisher.publishFailure(log);
        }

        return new Result(log.getId(), log.getStatus().name());
    }
}
