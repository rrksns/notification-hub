// 이메일 발송 Provider 선택 설정을 바인딩하는 record
package com.notificationhub.delivery.infrastructure.sender;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

@ConfigurationProperties(prefix = "email")
public record EmailDeliveryProperties(String provider) {

    public EmailDeliveryProperties {
        if (provider == null || provider.isBlank()) {
            provider = "logging";
        }
        provider = provider.toLowerCase(Locale.ROOT);
    }
}
