// DLQ export 레코드를 대상 Kafka 토픽으로 재발행하는 client 경계
package com.notificationhub.dlqops;

import java.util.List;

interface DlqReplayClient {

    void publish(DlqOptions options, List<DlqRecord> records);
}
