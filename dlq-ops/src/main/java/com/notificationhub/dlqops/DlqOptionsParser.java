// DLQ 운영 CLI 문자열 인자를 실행 옵션으로 변환하는 파서
package com.notificationhub.dlqops;

import java.util.UUID;

final class DlqOptionsParser {

    private DlqOptionsParser() {
    }

    static DlqOptions parse(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing command");
        }

        DlqCommand command = DlqCommand.from(args[0]);
        String bootstrapServers = "localhost:9092";
        String topic = "notifications.dlq";
        String targetTopic = "notifications";
        String groupId = "dlq-ops-" + UUID.randomUUID();
        int limit = 50;
        int timeoutSeconds = 5;
        String tenantId = null;
        String notificationId = null;
        String channel = null;
        String input = null;
        String output = null;
        boolean execute = false;

        for (int index = 1; index < args.length; index++) {
            String option = args[index];
            switch (option) {
                case "--bootstrap-servers" -> bootstrapServers = value(args, ++index, option);
                case "--topic" -> topic = value(args, ++index, option);
                case "--target-topic" -> targetTopic = value(args, ++index, option);
                case "--group-id" -> groupId = value(args, ++index, option);
                case "--limit" -> limit = positiveInt(value(args, ++index, option), option);
                case "--timeout-seconds" -> timeoutSeconds = positiveInt(value(args, ++index, option), option);
                case "--tenant-id" -> tenantId = value(args, ++index, option);
                case "--notification-id" -> notificationId = value(args, ++index, option);
                case "--channel" -> channel = value(args, ++index, option);
                case "--input" -> input = value(args, ++index, option);
                case "--output" -> output = value(args, ++index, option);
                case "--execute" -> execute = true;
                default -> throw new IllegalArgumentException("Unknown option: " + option);
            }
        }

        return new DlqOptions(
                command,
                bootstrapServers,
                topic,
                targetTopic,
                groupId,
                limit,
                timeoutSeconds,
                tenantId,
                notificationId,
                channel,
                input,
                output,
                execute
        );
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }

    private static int positiveInt(String value, String option) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw new IllegalArgumentException(option + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid integer for " + option, exception);
        }
    }
}
