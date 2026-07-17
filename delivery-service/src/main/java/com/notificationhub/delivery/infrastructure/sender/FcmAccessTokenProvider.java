// FCM HTTP v1 요청에 사용할 OAuth access token을 제공하는 계약
package com.notificationhub.delivery.infrastructure.sender;

public interface FcmAccessTokenProvider {
    String accessToken();
}
