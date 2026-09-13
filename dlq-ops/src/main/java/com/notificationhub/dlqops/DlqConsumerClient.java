// DLQ 토픽에서 운영 대상 레코드를 읽는 client 경계
package com.notificationhub.dlqops;

import java.util.List;

interface DlqConsumerClient {

    List<DlqRecord> read(DlqOptions options);
}
