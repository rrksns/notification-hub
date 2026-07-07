// 이메일 채널 발송 구현체의 공통 계약을 정의하는 인터페이스
package com.notificationhub.delivery.infrastructure.sender;

public interface EmailSender {
    void send(String recipient, String content);
}
