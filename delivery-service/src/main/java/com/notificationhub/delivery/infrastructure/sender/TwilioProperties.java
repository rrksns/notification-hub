// Twilio SMS Provider 설정을 바인딩하는 record
package com.notificationhub.delivery.infrastructure.sender;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "twilio")
public record TwilioProperties(
        String accountSid,
        String authToken,
        String fromNumber,
        String messagingServiceSid,
        String apiUrl
) {

    private static final String DEFAULT_API_URL = "https://api.twilio.com/2010-04-01";

    public TwilioProperties {
        if (accountSid == null) {
            accountSid = "";
        }
        if (authToken == null) {
            authToken = "";
        }
        if (fromNumber == null) {
            fromNumber = "";
        }
        if (messagingServiceSid == null) {
            messagingServiceSid = "";
        }
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = DEFAULT_API_URL;
        }
    }
}
