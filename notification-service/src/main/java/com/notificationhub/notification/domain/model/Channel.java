package com.notificationhub.notification.domain.model;

public enum Channel {
    EMAIL, SMS, PUSH;

    public static Channel from(String value) {
        try {
            return Channel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported channel type: " + value);
        }
    }
}
