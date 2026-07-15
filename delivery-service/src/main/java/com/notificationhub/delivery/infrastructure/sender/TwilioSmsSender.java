// Twilio Messages API로 SMS를 발송하는 어댑터
package com.notificationhub.delivery.infrastructure.sender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(prefix = "sms", name = "provider", havingValue = "twilio")
public class TwilioSmsSender implements SmsSender {

    private final TwilioProperties properties;
    private final RestClient restClient;

    public TwilioSmsSender(TwilioProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public void send(String recipient, String content) {
        validateRequiredSettings();

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(messageUrl())
                    .headers(headers -> headers.setBasicAuth(properties.accountSid(), properties.authToken()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(requestBody(recipient, content))
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode() != HttpStatus.CREATED) {
                throw new SmsDeliveryException("Twilio delivery failed: status=" + response.getStatusCode());
            }
        } catch (RestClientResponseException e) {
            throw new SmsDeliveryException("Twilio delivery failed: status=" + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new SmsDeliveryException("Twilio delivery failed: " + e.getMessage(), e);
        }
    }

    private void validateRequiredSettings() {
        if (properties.accountSid().isBlank()) {
            throw new SmsDeliveryException("Twilio account SID is required");
        }
        if (properties.authToken().isBlank()) {
            throw new SmsDeliveryException("Twilio auth token is required");
        }
        if (properties.fromNumber().isBlank() && properties.messagingServiceSid().isBlank()) {
            throw new SmsDeliveryException("Twilio sender is required");
        }
    }

    private String messageUrl() {
        String baseUrl = properties.apiUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/Accounts/" + properties.accountSid() + "/Messages.json";
    }

    private MultiValueMap<String, String> requestBody(String recipient, String content) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", recipient);
        body.add("Body", content);
        if (properties.messagingServiceSid().isBlank()) {
            body.add("From", properties.fromNumber());
        } else {
            body.add("MessagingServiceSid", properties.messagingServiceSid());
        }
        return body;
    }
}
