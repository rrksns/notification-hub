// Google service account 인증 정보로 FCM OAuth access token을 발급하는 구현체
package com.notificationhub.delivery.infrastructure.sender;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "push", name = "provider", havingValue = "fcm")
public class GoogleServiceAccountAccessTokenProvider implements FcmAccessTokenProvider {

    private static final String FIREBASE_MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final FcmProperties properties;
    private GoogleCredentials scopedCredentials;

    public GoogleServiceAccountAccessTokenProvider(FcmProperties properties) {
        this.properties = properties;
    }

    @Override
    public String accessToken() {
        try {
            GoogleCredentials credentials = scopedCredentials();
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new PushDeliveryException("FCM access token creation failed: " + e.getMessage(), e);
        }
    }

    private synchronized GoogleCredentials scopedCredentials() throws IOException {
        if (scopedCredentials == null) {
            scopedCredentials = credentials().createScoped(List.of(FIREBASE_MESSAGING_SCOPE));
        }
        return scopedCredentials;
    }

    private GoogleCredentials credentials() throws IOException {
        if (!properties.credentialsJson().isBlank()) {
            try (InputStream inputStream = new ByteArrayInputStream(
                    properties.credentialsJson().getBytes(StandardCharsets.UTF_8))) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }
        if (!properties.credentialsPath().isBlank()) {
            try (InputStream inputStream = new FileInputStream(properties.credentialsPath())) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }
        return GoogleCredentials.getApplicationDefault();
    }
}
