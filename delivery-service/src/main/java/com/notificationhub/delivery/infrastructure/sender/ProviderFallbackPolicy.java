// Provider 장애 후 fallback 처리 정책을 정의하는 계약
package com.notificationhub.delivery.infrastructure.sender;

import com.notificationhub.delivery.domain.model.ChannelType;

public interface ProviderFallbackPolicy {
    void recordFailure(ChannelType channel, String recipient, Throwable cause);
}
