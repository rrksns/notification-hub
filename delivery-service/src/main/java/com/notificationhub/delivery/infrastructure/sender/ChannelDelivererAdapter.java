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
 * 각 채널별 sender 구현체로 발송을 위임합니다.
 * Circuit Breaker + Retry: application.yml의 channelDelivery 인스턴스 설정 적용.
 */
@Component
public class ChannelDelivererAdapter implements ChannelDelivererPort {

    private static final Logger log = LoggerFactory.getLogger(ChannelDelivererAdapter.class);
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final PushSender pushSender;

    public ChannelDelivererAdapter(EmailSender emailSender, SmsSender smsSender, PushSender pushSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.pushSender = pushSender;
    }

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
        throw new RuntimeException("Delivery failed after circuit breaker fallback: " + t.getMessage(), t);
    }

    private void sendEmail(String recipient, String content) {
        emailSender.send(recipient, content);
    }

    private void sendSms(String recipient, String content) {
        smsSender.send(recipient, content);
    }

    private void sendPush(String recipient, String content) {
        pushSender.send(recipient, content);
    }
}
