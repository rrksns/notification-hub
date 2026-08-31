// Redis Lua script으로 알림 월간 쿼터를 원자적으로 소비하는 어댑터
package com.notificationhub.notification.infrastructure.cache;

import com.notificationhub.notification.domain.quota.NotificationQuotaPolicy;
import com.notificationhub.notification.domain.port.out.NotificationQuotaPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

@Component
public class RedisNotificationQuotaAdapter implements NotificationQuotaPort {

    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            current = tonumber(current) or 0
            local limit = tonumber(ARGV[1])
            if current + 1 > limit then
                return 0
            end
            local next = redis.call('INCR', KEYS[1])
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            return next
            """, Long.class);
    private static final long MONTH_SECONDS = 2_678_400L;

    private final StringRedisTemplate redisTemplate;
    private final NotificationQuotaPolicy policy;

    public RedisNotificationQuotaAdapter(StringRedisTemplate redisTemplate, NotificationQuotaPolicy policy) {
        this.redisTemplate = redisTemplate;
        this.policy = policy;
    }

    @Override
    public boolean tryConsume(String tenantId, String plan) {
        String key = policy.key(tenantId, plan, YearMonth.now());
        Long result = redisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(key),
                String.valueOf(policy.limitFor(plan)),
                String.valueOf(MONTH_SECONDS)
        );
        return result != null && result > 0;
    }
}
