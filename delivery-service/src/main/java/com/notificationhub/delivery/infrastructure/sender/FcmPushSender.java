// FCM HTTP v1 API로 PUSH 알림을 발송하는 어댑터
package com.notificationhub.delivery.infrastructure.sender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "push", name = "provider", havingValue = "fcm")
public class FcmPushSender implements PushSender {

    private final FcmProperties properties;
    private final FcmAccessTokenProvider accessTokenProvider;
    private final RestClient restClient;

    public FcmPushSender(FcmProperties properties,
                         FcmAccessTokenProvider accessTokenProvider,
                         RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.accessTokenProvider = accessTokenProvider;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public void send(String recipient, String content) {
        validateRequiredSettings();
        String accessToken = accessTokenProvider.accessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new PushDeliveryException("FCM access token is required");
        }

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(sendUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(recipient, content))
                    .retrieve()
                    .toBodilessEntity();

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new PushDeliveryException("FCM delivery failed: status=" + response.getStatusCode());
            }
        } catch (RestClientResponseException e) {
            throw new PushDeliveryException("FCM delivery failed: status=" + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new PushDeliveryException("FCM delivery failed: " + e.getMessage(), e);
        }
    }

    private void validateRequiredSettings() {
        if (properties.projectId().isBlank()) {
            throw new PushDeliveryException("FCM project id is required");
        }
    }

    private String sendUrl() {
        String baseUrl = properties.apiUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/projects/" + properties.projectId() + "/messages:send";
    }

    private Map<String, Object> requestBody(String recipient, String content) {
        return Map.of(
                "message", Map.of(
                        "token", recipient,
                        "notification", Map.of(
                                "title", properties.title(),
                                "body", content
                        )
                )
        );
    }
}
