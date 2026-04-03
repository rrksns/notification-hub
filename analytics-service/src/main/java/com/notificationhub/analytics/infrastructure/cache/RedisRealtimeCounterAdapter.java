package com.notificationhub.analytics.infrastructure.cache;

import com.notificationhub.analytics.domain.port.out.RealtimeCounterPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRealtimeCounterAdapter implements RealtimeCounterPort {

    private static final Logger log = LoggerFactory.getLogger(RedisRealtimeCounterAdapter.class);

    private static final String SUCCESS_KEY = "realtime:%s:success:%s";
    private static final String FAILURE_KEY = "realtime:%s:failure:%s";
    private static final String TOTAL_SUCCESS_KEY = "realtime:%s:success:total";
    private static final String TOTAL_FAILURE_KEY = "realtime:%s:failure:total";

    private final StringRedisTemplate redisTemplate;

    public RedisRealtimeCounterAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private long parseLongSafe(String val, String key) {
        if (val == null) return 0L;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            log.warn("Invalid Redis counter value for key={}: '{}'", key, val);
            return 0L;
        }
    }

    @Override
    public void incrementSuccess(String tenantId, String channel) {
        redisTemplate.opsForValue().increment(String.format(SUCCESS_KEY, tenantId, channel));
        redisTemplate.opsForValue().increment(String.format(TOTAL_SUCCESS_KEY, tenantId));
    }

    @Override
    public void incrementFailure(String tenantId, String channel) {
        redisTemplate.opsForValue().increment(String.format(FAILURE_KEY, tenantId, channel));
        redisTemplate.opsForValue().increment(String.format(TOTAL_FAILURE_KEY, tenantId));
    }

    @Override
    public long getTotalSuccess(String tenantId) {
        String key = String.format(TOTAL_SUCCESS_KEY, tenantId);
        return parseLongSafe(redisTemplate.opsForValue().get(key), key);
    }

    @Override
    public long getTotalFailed(String tenantId) {
        String key = String.format(TOTAL_FAILURE_KEY, tenantId);
        return parseLongSafe(redisTemplate.opsForValue().get(key), key);
    }
}
