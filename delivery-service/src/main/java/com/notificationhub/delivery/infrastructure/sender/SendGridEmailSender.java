// SendGrid Mail Send API로 이메일을 발송하는 어댑터
package com.notificationhub.delivery.infrastructure.sender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "email", name = "provider", havingValue = "sendgrid")
public class SendGridEmailSender implements EmailSender {

    private final SendGridProperties properties;
    private final RestClient restClient;

    public SendGridEmailSender(SendGridProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public void send(String recipient, String content) {
        validateRequiredSettings();

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(properties.apiUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(recipient, content))
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode() != HttpStatus.ACCEPTED) {
                throw new EmailDeliveryException("SendGrid delivery failed: status=" + response.getStatusCode());
            }
        } catch (RestClientResponseException e) {
            throw new EmailDeliveryException("SendGrid delivery failed: status=" + e.getStatusCode(), e);
        }
    }

    private void validateRequiredSettings() {
        if (properties.apiKey().isBlank()) {
            throw new EmailDeliveryException("SendGrid API key is required");
        }
        if (properties.fromEmail().isBlank()) {
            throw new EmailDeliveryException("SendGrid from email is required");
        }
    }

    private Map<String, Object> requestBody(String recipient, String content) {
        return Map.of(
                "personalizations", List.of(Map.of(
                        "to", List.of(Map.of("email", recipient))
                )),
                "from", Map.of(
                        "email", properties.fromEmail(),
                        "name", properties.fromName()
                ),
                "subject", properties.subject(),
                "content", List.of(Map.of(
                        "type", "text/plain",
                        "value", content
                ))
        );
    }
}
