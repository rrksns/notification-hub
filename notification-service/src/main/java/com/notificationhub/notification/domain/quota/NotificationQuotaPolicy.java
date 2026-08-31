// 요금제별 알림 쿼터와 월별 Redis 키를 계산하는 정책
package com.notificationhub.notification.domain.quota;

import java.time.YearMonth;
import org.springframework.stereotype.Component;

@Component
public class NotificationQuotaPolicy {

    public long limitFor(String plan) {
        return switch (plan) {
            case "FREE" -> 100L;
            case "BASIC" -> 1_000L;
            case "PREMIUM" -> 10_000L;
            case "ENTERPRISE" -> 100_000L;
            default -> throw new IllegalArgumentException("Unknown subscription plan: " + plan);
        };
    }

    public String key(String tenantId, String plan, YearMonth month) {
        limitFor(plan);
        return "quota:notification:" + tenantId + ":" + month;
    }
}
