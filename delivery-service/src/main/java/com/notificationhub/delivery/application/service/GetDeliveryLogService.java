package com.notificationhub.delivery.application.service;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.common.exception.ErrorCode;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.port.in.GetDeliveryLogUseCase;
import com.notificationhub.delivery.domain.port.out.DeliveryLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetDeliveryLogService implements GetDeliveryLogUseCase {

    private final DeliveryLogRepository deliveryLogRepository;

    public GetDeliveryLogService(DeliveryLogRepository deliveryLogRepository) {
        this.deliveryLogRepository = deliveryLogRepository;
    }

    @Override
    public DeliveryLog getById(String id) {
        return deliveryLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public List<DeliveryLog> getByTenantId(String tenantId) {
        return deliveryLogRepository.findByTenantId(tenantId);
    }
}
