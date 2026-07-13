// 실제 SMS Provider 연동 전까지 기존 로그 발송 동작을 유지하는 구현체
package com.notificationhub.delivery.infrastructure.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sms", name = "provider", havingValue = "logging", matchIfMissing = true)
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String recipient, String content) {
        log.info("[SMS] → {} : {}", recipient, content);
    }
}
