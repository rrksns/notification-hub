// DLQ list와 export 명령 실행을 검증하는 테스트
package com.notificationhub.dlqops;

import com.notificationhub.common.event.NotificationEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DlqOpsListExportTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("list 명령은 필터와 일치하는 DLQ 레코드만 출력한다")
    void run_list_printsFilteredRecords() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DlqConsumerClient consumerClient = options -> List.of(
                record("notification-1", "tenant-1", "EMAIL", 12),
                record("notification-2", "tenant-2", "SMS", 13)
        );

        int exitCode = DlqOpsApplication.run(
                new String[]{"list", "--tenant-id", "tenant-1"},
                new PrintStream(output),
                System.err,
                consumerClient,
                new DlqRecordCodec()
        );

        String printed = output.toString(StandardCharsets.UTF_8);
        assertThat(exitCode).isZero();
        assertThat(printed).contains("notification-1");
        assertThat(printed).doesNotContain("notification-2");
    }

    @Test
    @DisplayName("export 명령은 필터와 일치하는 DLQ 레코드를 JSON Lines 파일에 저장한다")
    void run_export_writesFilteredRecords() {
        Path output = tempDir.resolve("dlq.jsonl");
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        DlqConsumerClient consumerClient = options -> List.of(
                record("notification-1", "tenant-1", "EMAIL", 12),
                record("notification-2", "tenant-2", "SMS", 13)
        );

        int exitCode = DlqOpsApplication.run(
                new String[]{"export", "--tenant-id", "tenant-1", "--output", output.toString()},
                new PrintStream(stdout),
                System.err,
                consumerClient,
                new DlqRecordCodec()
        );

        String exported = readString(output);
        assertThat(exitCode).isZero();
        assertThat(exported).contains("notification-1");
        assertThat(exported).doesNotContain("notification-2");
    }

    @Test
    @DisplayName("export 명령에 output 옵션이 없으면 실패한다")
    void run_exportWithoutOutput_returnsFailure() {
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        DlqConsumerClient consumerClient = options -> List.of(record("notification-1", "tenant-1", "EMAIL", 12));

        int exitCode = DlqOpsApplication.run(
                new String[]{"export"},
                System.out,
                new PrintStream(error),
                consumerClient,
                new DlqRecordCodec()
        );

        assertThat(exitCode).isEqualTo(1);
        assertThat(error.toString(StandardCharsets.UTF_8)).contains("Missing --output");
    }

    private String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    private DlqRecord record(String notificationId, String tenantId, String channel, long offset) {
        NotificationEvent event = new NotificationEvent(
                notificationId,
                tenantId,
                channel,
                "user@example.com",
                "content",
                "key-1",
                Instant.parse("2026-08-26T12:00:00Z")
        );
        return new DlqRecord("notifications.dlq", 0, offset, Instant.parse("2026-08-26T12:00:00Z"), notificationId, event);
    }
}
