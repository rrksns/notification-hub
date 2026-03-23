package com.notificationhub.analytics.infrastructure.persistence.adapter;

import com.notificationhub.analytics.domain.model.DailyStats;
import com.notificationhub.analytics.domain.port.out.DailyStatsRepository;
import com.notificationhub.analytics.infrastructure.persistence.document.DailyStatsDocument;
import com.notificationhub.analytics.infrastructure.persistence.repository.DailyStatsMongoRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;

@Component
public class DailyStatsRepositoryAdapter implements DailyStatsRepository {

    private final DailyStatsMongoRepository mongoRepository;
    private final MongoTemplate mongoTemplate;

    public DailyStatsRepositoryAdapter(DailyStatsMongoRepository mongoRepository, MongoTemplate mongoTemplate) {
        this.mongoRepository = mongoRepository;
        this.mongoTemplate = mongoTemplate;
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

    @Override
    public void incrementSuccess(String tenantId, LocalDate date, String channel) {
        Query query = new Query(Criteria.where("tenantId").is(tenantId).and("date").is(date));
        Update update = new Update()
                .inc("totalSent", 1)
                .inc("totalSuccess", 1)
                .inc("channelCounts." + channel + ".0", 1)
                .setOnInsert("id", tenantId + ":" + date)
                .setOnInsert("tenantId", tenantId)
                .setOnInsert("date", date);
        mongoTemplate.findAndModify(query, update, options().upsert(true), DailyStatsDocument.class);
    }

    @Override
    public void incrementFailure(String tenantId, LocalDate date, String channel) {
        Query query = new Query(Criteria.where("tenantId").is(tenantId).and("date").is(date));
        Update update = new Update()
                .inc("totalSent", 1)
                .inc("totalFailed", 1)
                .inc("channelCounts." + channel + ".1", 1)
                .setOnInsert("id", tenantId + ":" + date)
                .setOnInsert("tenantId", tenantId)
                .setOnInsert("date", date);
        mongoTemplate.findAndModify(query, update, options().upsert(true), DailyStatsDocument.class);
    }
}
