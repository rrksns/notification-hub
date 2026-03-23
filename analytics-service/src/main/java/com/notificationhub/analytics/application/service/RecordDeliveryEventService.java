package com.notificationhub.analytics.application.service;

import com.notificationhub.analytics.domain.model.DeliveryEvent;
import com.notificationhub.analytics.domain.port.in.RecordDeliveryEventUseCase;
import com.notificationhub.analytics.domain.port.out.DailyStatsRepository;
import com.notificationhub.analytics.domain.port.out.DeliveryEventRepository;
import com.notificationhub.analytics.domain.port.out.RealtimeCounterPort;
import org.springframework.stereotype.Service;

@Service
public class RecordDeliveryEventService implements RecordDeliveryEventUseCase {

    private final DeliveryEventRepository deliveryEventRepository;
    private final DailyStatsRepository dailyStatsRepository;
    private final RealtimeCounterPort realtimeCounterPort;

    public RecordDeliveryEventService(DeliveryEventRepository deliveryEventRepository,
                                      DailyStatsRepository dailyStatsRepository,
                                      RealtimeCounterPort realtimeCounterPort) {
        this.deliveryEventRepository = deliveryEventRepository;
        this.dailyStatsRepository = dailyStatsRepository;
        this.realtimeCounterPort = realtimeCounterPort;
    }

    @Override
    public void record(Command command) {
        DeliveryEvent event = DeliveryEvent.create(
                command.deliveryLogId(), command.notificationId(), command.tenantId(),
                command.channel(), command.status(), command.failureReason(), command.occurredAt()
        );
        deliveryEventRepository.save(event);

        if (event.isSuccess()) {
            dailyStatsRepository.incrementSuccess(command.tenantId(), command.occurredAt().toLocalDate(), command.channel());
            realtimeCounterPort.incrementSuccess(command.tenantId(), command.channel());
        } else {
            dailyStatsRepository.incrementFailure(command.tenantId(), command.occurredAt().toLocalDate(), command.channel());
            realtimeCounterPort.incrementFailure(command.tenantId(), command.channel());
        }
    }
}
