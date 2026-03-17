package com.notificationhub.delivery.infrastructure.sender;

import com.notificationhub.delivery.domain.model.ChannelType;
import com.notificationhub.delivery.domain.port.out.ChannelDelivererPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 채널별 발송 어댑터.
 * 실제 환경에서는 SendGrid(EMAIL), Twilio(SMS), FCM(PUSH)를 주입받아 사용합니다.
 * 현재는 로그 출력 stub으로 구현되어 있습니다.
 * Circuit Breaker + Retry: application.yml의 channelDelivery 인스턴스 설정 적용.
 */
@Component
public class ChannelDelivererAdapter implements ChannelDelivererPort {

    private static final Logger log = LoggerFactory.getLogger(ChannelDelivererAdapter.class);

    @Override
    @CircuitBreaker(name = "channelDelivery", fallbackMethod = "deliverFallback")
    @Retry(name = "channelDelivery")
    public void deliver(ChannelType channel, String recipient, String content) {
        switch (channel) {
            case EMAIL -> sendEmail(recipient, content);
            case SMS -> sendSms(recipient, content);
            case PUSH -> sendPush(recipient, content);
        }
    }

    void deliverFallback(ChannelType channel, String recipient, String content, Throwable t) {
        log.warn("[FALLBACK] Circuit OPEN — channel={}, recipient={}, reason={}", channel, recipient, t.getMessage());
    }

    private void sendEmail(String recipient, String content) {
        // TODO: SendGrid / AWS SES 연동
        log.info("[EMAIL] → {} : {}", recipient, content);
    }

    private void sendSms(String recipient, String content) {
        // TODO: Twilio 연동
        log.info("[SMS] → {} : {}", recipient, content);
    }

    private void sendPush(String recipient, String content) {
        // TODO: Firebase Cloud Messaging 연동
        log.info("[PUSH] → {} : {}", recipient, content);
    }
}
