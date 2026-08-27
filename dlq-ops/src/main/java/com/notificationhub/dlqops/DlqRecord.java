// DLQ export 파일에 저장할 Kafka 레코드와 이벤트 내용을 표현하는 값 객체
package com.notificationhub.dlqops;

import com.notificationhub.common.event.NotificationEvent;
import java.time.Instant;

record DlqRecord(
        String topic,
        int partition,
        long offset,
        Instant timestamp,
        String key,
        NotificationEvent event
) {
}
