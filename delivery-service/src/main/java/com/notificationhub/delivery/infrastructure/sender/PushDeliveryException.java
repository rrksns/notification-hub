// PUSH Provider 발송 실패를 delivery 실패 흐름으로 전달하는 예외
package com.notificationhub.delivery.infrastructure.sender;

public class PushDeliveryException extends RuntimeException {
    public PushDeliveryException(String message) {
        super(message);
    }

    public PushDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
