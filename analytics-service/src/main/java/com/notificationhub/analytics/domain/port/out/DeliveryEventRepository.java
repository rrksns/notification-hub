package com.notificationhub.analytics.domain.port.out;

import com.notificationhub.analytics.domain.model.DeliveryEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryEventRepository {
    DeliveryEvent save(DeliveryEvent event);
    Page<DeliveryEvent> findByTenantId(String tenantId, Pageable pageable);
}
