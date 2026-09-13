// DLQ 운영 CLI가 지원하는 명령을 정의하는 enum
package com.notificationhub.dlqops;

enum DlqCommand {
    LIST,
    EXPORT,
    REPLAY;

    static DlqCommand from(String value) {
        return switch (value) {
            case "list" -> LIST;
            case "export" -> EXPORT;
            case "replay" -> REPLAY;
            default -> throw new IllegalArgumentException("Unknown command: " + value);
        };
    }
}
