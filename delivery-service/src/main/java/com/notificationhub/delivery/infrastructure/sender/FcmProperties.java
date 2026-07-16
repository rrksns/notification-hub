// Firebase Cloud Messaging Provider 설정을 바인딩하는 record
package com.notificationhub.delivery.infrastructure.sender;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fcm")
public record FcmProperties(
        String projectId,
        String credentialsJson,
        String credentialsPath,
        String apiUrl,
        String title
) {

    private static final String DEFAULT_API_URL = "https://fcm.googleapis.com/v1";
    private static final String DEFAULT_TITLE = "Notification Hub";

    public FcmProperties {
        if (projectId == null) {
            projectId = "";
        }
        if (credentialsJson == null) {
            credentialsJson = "";
        }
        if (credentialsPath == null) {
            credentialsPath = "";
        }
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = DEFAULT_API_URL;
        }
        if (title == null || title.isBlank()) {
            title = DEFAULT_TITLE;
        }
    }
}
