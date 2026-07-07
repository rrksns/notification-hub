// 실제 이메일 Provider 연동 전까지 기존 로그 발송 동작을 유지하는 구현체
package com.notificationhub.delivery.infrastructure.sender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "email", name = "provider", havingValue = "logging", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String recipient, String content) {
        log.info("[EMAIL] → {} : {}", recipient, content);
    }
}
