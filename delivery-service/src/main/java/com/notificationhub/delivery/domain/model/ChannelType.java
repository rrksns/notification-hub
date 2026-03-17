package com.notificationhub.delivery.domain.model;

public enum ChannelType {
    EMAIL, SMS, PUSH;

    public static ChannelType from(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported channel type: " + value);
        }
    }
}
