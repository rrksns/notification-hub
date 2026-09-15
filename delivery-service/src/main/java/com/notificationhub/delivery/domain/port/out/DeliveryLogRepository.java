package com.notificationhub.delivery.domain.port.out;

import com.notificationhub.delivery.domain.model.DeliveryLog;

import java.util.List;
import java.util.Optional;

public interface DeliveryLogRepository {
    DeliveryLog save(DeliveryLog deliveryLog);
    Optional<DeliveryLog> findByIdAndTenantId(String id, String tenantId);
    List<DeliveryLog> findByNotificationId(String notificationId);
    List<DeliveryLog> findByTenantId(String tenantId);
}
