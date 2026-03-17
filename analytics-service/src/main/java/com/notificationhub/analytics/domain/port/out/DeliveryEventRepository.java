package com.notificationhub.analytics.domain.port.out;

import com.notificationhub.analytics.domain.model.DeliveryEvent;
import java.util.List;

public interface DeliveryEventRepository {
    DeliveryEvent save(DeliveryEvent event);
    List<DeliveryEvent> findByTenantId(String tenantId);
}
