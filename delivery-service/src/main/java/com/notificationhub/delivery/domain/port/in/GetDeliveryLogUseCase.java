package com.notificationhub.delivery.domain.port.in;

import com.notificationhub.delivery.domain.model.DeliveryLog;

import java.util.List;

public interface GetDeliveryLogUseCase {
    DeliveryLog getById(String id);
    List<DeliveryLog> getByTenantId(String tenantId);
}
