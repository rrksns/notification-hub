package com.notificationhub.analytics.infrastructure.persistence.adapter;

import com.notificationhub.analytics.domain.model.DeliveryEvent;
import com.notificationhub.analytics.domain.port.out.DeliveryEventRepository;
import com.notificationhub.analytics.infrastructure.persistence.document.DeliveryEventDocument;
import com.notificationhub.analytics.infrastructure.persistence.repository.DeliveryEventMongoRepository;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DeliveryEventRepositoryAdapter implements DeliveryEventRepository {

    private final DeliveryEventMongoRepository mongoRepository;

    public DeliveryEventRepositoryAdapter(DeliveryEventMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public DeliveryEvent save(DeliveryEvent event) {
        return mongoRepository.save(DeliveryEventDocument.from(event)).toDomain();
    }

    @Override
    public List<DeliveryEvent> findByTenantId(String tenantId) {
        return mongoRepository.findByTenantId(tenantId).stream()
                .map(DeliveryEventDocument::toDomain)
                .toList();
    }
}
