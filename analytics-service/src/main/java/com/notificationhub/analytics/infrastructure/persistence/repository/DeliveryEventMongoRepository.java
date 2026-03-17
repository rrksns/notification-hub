package com.notificationhub.analytics.infrastructure.persistence.repository;

import com.notificationhub.analytics.infrastructure.persistence.document.DeliveryEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DeliveryEventMongoRepository extends MongoRepository<DeliveryEventDocument, String> {
    List<DeliveryEventDocument> findByTenantId(String tenantId);
}
