// Provider 장애를 운영 로그로 기록하는 fail-closed fallback 구현
package com.notificationhub.delivery.infrastructure.sender;

import com.notificationhub.delivery.domain.model.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingProviderFallbackPolicy implements ProviderFallbackPolicy {

    private static final Logger log = LoggerFactory.getLogger(LoggingProviderFallbackPolicy.class);

    @Override
    public void recordFailure(ChannelType channel, String recipient, Throwable cause) {
        log.error("Provider fallback exhausted: channel={}, recipient={}, reason={}",
                channel, recipient, cause.getMessage(), cause);
    }
}
