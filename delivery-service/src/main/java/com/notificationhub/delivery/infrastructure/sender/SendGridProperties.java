// SendGrid 이메일 Provider 설정을 바인딩하는 record
package com.notificationhub.delivery.infrastructure.sender;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sendgrid")
public record SendGridProperties(
        String apiKey,
        String fromEmail,
        String fromName,
        String apiUrl
) {

    private static final String DEFAULT_API_URL = "https://api.sendgrid.com/v3/mail/send";
    private static final String DEFAULT_FROM_NAME = "Notification Hub";

    public SendGridProperties {
        if (apiKey == null) {
            apiKey = "";
        }
        if (fromEmail == null) {
            fromEmail = "";
        }
        if (fromName == null || fromName.isBlank()) {
            fromName = DEFAULT_FROM_NAME;
        }
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = DEFAULT_API_URL;
        }
    }
}
