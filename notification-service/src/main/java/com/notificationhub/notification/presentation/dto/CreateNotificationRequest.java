package com.notificationhub.notification.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNotificationRequest(
        @NotBlank String channel,
        @NotBlank String recipient,
        @NotBlank String content,
        @NotBlank String idempotencyKey
) {}
