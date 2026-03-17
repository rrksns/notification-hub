package com.notificationhub.analytics.infrastructure.persistence.adapter;

import com.notificationhub.analytics.domain.model.DailyStats;
import com.notificationhub.analytics.domain.port.out.DailyStatsRepository;
import com.notificationhub.analytics.infrastructure.persistence.document.DailyStatsDocument;
import com.notificationhub.analytics.infrastructure.persistence.repository.DailyStatsMongoRepository;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.Optional;

@Component
public class DailyStatsRepositoryAdapter implements DailyStatsRepository {

    private final DailyStatsMongoRepository mongoRepository;

    public DailyStatsRepositoryAdapter(DailyStatsMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public DailyStats save(DailyStats dailyStats) {
        return mongoRepository.save(DailyStatsDocument.from(dailyStats)).toDomain();
    }

    @Override
    public Optional<DailyStats> findByTenantIdAndDate(String tenantId, LocalDate date) {
        return mongoRepository.findByTenantIdAndDate(tenantId, date)
                .map(DailyStatsDocument::toDomain);
    }
}
