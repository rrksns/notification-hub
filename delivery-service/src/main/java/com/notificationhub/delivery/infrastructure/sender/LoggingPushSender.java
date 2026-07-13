// 실제 PUSH Provider 연동 전까지 기존 로그 발송 동작을 유지하는 구현체
package com.notificationhub.delivery.infrastructure.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "push", name = "provider", havingValue = "logging", matchIfMissing = true)
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public void send(String recipient, String content) {
        log.info("[PUSH] → {} : {}", recipient, content);
    }
}
