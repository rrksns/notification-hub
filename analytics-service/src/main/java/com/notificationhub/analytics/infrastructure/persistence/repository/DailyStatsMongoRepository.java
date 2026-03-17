package com.notificationhub.analytics.infrastructure.persistence.repository;

import com.notificationhub.analytics.infrastructure.persistence.document.DailyStatsDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface DailyStatsMongoRepository extends MongoRepository<DailyStatsDocument, String> {
    Optional<DailyStatsDocument> findByTenantIdAndDate(String tenantId, LocalDate date);
}
