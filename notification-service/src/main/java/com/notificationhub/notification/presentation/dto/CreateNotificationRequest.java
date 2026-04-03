package com.notificationhub.notification.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotBlank String channel,
        @NotBlank String recipient,
        @NotBlank @Size(max = 2000, message = "content must not exceed 2000 characters") String content,
        @NotBlank String idempotencyKey
) {}
