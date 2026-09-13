// DLQ export JSON Lines 형식의 직렬화와 역직렬화를 검증하는 테스트
package com.notificationhub.dlqops;

import com.notificationhub.common.event.NotificationEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DlqRecordCodecTest {

    @TempDir
    Path tempDir;

    private final DlqRecordCodec codec = new DlqRecordCodec();

    @Test
    @DisplayName("DLQ 레코드를 한 줄 JSON으로 직렬화한다")
    void toLine_serializesRecord() {
        String line = codec.toLine(record("notification-1", 12));

        assertThat(line).contains("\"topic\":\"notifications.dlq\"");
        assertThat(line).contains("\"offset\":12");
        assertThat(line).contains("\"notificationId\":\"notification-1\"");
    }

    @Test
    @DisplayName("한 줄 JSON을 DLQ 레코드로 역직렬화한다")
    void fromLine_deserializesRecord() {
        String line = """
                {"topic":"notifications.dlq","partition":0,"offset":12,"timestamp":"2026-08-26T12:00:00Z","key":"notification-1","event":{"notificationId":"notification-1","tenantId":"tenant-1","channel":"EMAIL","recipient":"user@example.com","content":"content","idempotencyKey":"key-1","occurredAt":"2026-08-26T12:00:00Z"}}
                """.trim();

        DlqRecord record = codec.fromLine(line);

        assertThat(record.topic()).isEqualTo("notifications.dlq");
        assertThat(record.offset()).isEqualTo(12);
        assertThat(record.event().notificationId()).isEqualTo("notification-1");
    }

    @Test
    @DisplayName("여러 DLQ 레코드를 JSON Lines 파일로 왕복한다")
    void writeAndRead_roundTripsRecords() {
        Path output = tempDir.resolve("dlq.jsonl");
        List<DlqRecord> records = List.of(
                record("notification-1", 12),
                record("notification-2", 13)
        );

        codec.write(output, records);
        List<DlqRecord> restored = codec.read(output);

        assertThat(restored).hasSize(2);
        assertThat(restored).extracting(DlqRecord::offset).containsExactly(12L, 13L);
        assertThat(restored).extracting(record -> record.event().notificationId())
                .containsExactly("notification-1", "notification-2");
    }

    private DlqRecord record(String notificationId, long offset) {
        NotificationEvent event = new NotificationEvent(
                notificationId,
                "tenant-1",
                "EMAIL",
                "user@example.com",
                "content",
                "key-1",
                Instant.parse("2026-08-26T12:00:00Z")
        );
        return new DlqRecord("notifications.dlq", 0, offset, Instant.parse("2026-08-26T12:00:00Z"), notificationId, event);
    }
}
