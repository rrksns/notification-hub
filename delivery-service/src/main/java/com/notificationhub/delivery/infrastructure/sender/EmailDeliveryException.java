// 이메일 Provider 발송 실패를 표현하는 인프라 예외
package com.notificationhub.delivery.infrastructure.sender;

public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message) {
        super(message);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
