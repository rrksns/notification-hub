package com.notificationhub.notification.infrastructure.cache;

import com.notificationhub.notification.domain.port.out.IdempotencyPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private static final String KEY_PREFIX = "idempotency:notification:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isDuplicate(String tenantId, String idempotencyKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tenantId + ":" + idempotencyKey));
    }

    @Override
    public void save(String tenantId, String idempotencyKey) {
        redisTemplate.opsForValue().set(KEY_PREFIX + tenantId + ":" + idempotencyKey, "1", TTL);
    }
}
