package com.notificationhub.analytics.infrastructure.persistence.adapter;

import com.notificationhub.analytics.domain.model.DeliveryEvent;
import com.notificationhub.analytics.domain.port.out.DeliveryEventRepository;
import com.notificationhub.analytics.infrastructure.persistence.document.DeliveryEventDocument;
import com.notificationhub.analytics.infrastructure.persistence.repository.DeliveryEventMongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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
    public Page<DeliveryEvent> findByTenantId(String tenantId, Pageable pageable) {
        return mongoRepository.findByTenantId(tenantId, pageable)
                .map(DeliveryEventDocument::toDomain);
    }
}
