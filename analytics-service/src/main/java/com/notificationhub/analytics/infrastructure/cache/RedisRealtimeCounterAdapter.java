package com.notificationhub.analytics.infrastructure.cache;

import com.notificationhub.analytics.domain.port.out.RealtimeCounterPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRealtimeCounterAdapter implements RealtimeCounterPort {

    private static final String SUCCESS_KEY = "realtime:%s:success:%s";
    private static final String FAILURE_KEY = "realtime:%s:failure:%s";
    private static final String TOTAL_SUCCESS_KEY = "realtime:%s:success:total";
    private static final String TOTAL_FAILURE_KEY = "realtime:%s:failure:total";

    private final StringRedisTemplate redisTemplate;

    public RedisRealtimeCounterAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
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
        String val = redisTemplate.opsForValue().get(String.format(TOTAL_SUCCESS_KEY, tenantId));
        return val == null ? 0L : Long.parseLong(val);
    }

    @Override
    public long getTotalFailed(String tenantId) {
        String val = redisTemplate.opsForValue().get(String.format(TOTAL_FAILURE_KEY, tenantId));
        return val == null ? 0L : Long.parseLong(val);
    }
}
