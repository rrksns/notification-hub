// DLQ 운영 CLI 실행 옵션을 보관하는 불변 값 객체
package com.notificationhub.dlqops;

record DlqOptions(
        DlqCommand command,
        String bootstrapServers,
        String topic,
        String targetTopic,
        String groupId,
        int limit,
        int timeoutSeconds,
        String tenantId,
        String notificationId,
        String channel,
        String input,
        String output,
        boolean execute
) {
}
