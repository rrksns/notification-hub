package com.notificationhub.analytics.infrastructure.persistence.repository;

import com.notificationhub.analytics.infrastructure.persistence.document.DeliveryEventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeliveryEventMongoRepository extends MongoRepository<DeliveryEventDocument, String> {
    Page<DeliveryEventDocument> findByTenantId(String tenantId, Pageable pageable);
}
