// DLQ replay 명령의 dry-run과 실제 발행 조건을 검증하는 테스트
package com.notificationhub.dlqops;

import com.notificationhub.common.event.NotificationEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DlqOpsReplayTest {

    @TempDir
    Path tempDir;

    private final DlqRecordCodec codec = new DlqRecordCodec();

    @Test
    @DisplayName("replay는 기본적으로 dry-run으로 동작하고 메시지를 발행하지 않는다")
    void run_replayDefaultsToDryRun_doesNotPublish() {
        Path input = writeInputFile(record("notification-1", 12));
        RecordingReplayClient replayClient = new RecordingReplayClient();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = DlqOpsApplication.run(
                new String[]{"replay", "--input", input.toString()},
                new PrintStream(output),
                System.err,
                options -> List.of(),
                codec,
                replayClient
        );

        assertThat(exitCode).isZero();
        assertThat(replayClient.published).isEmpty();
        assertThat(output.toString(StandardCharsets.UTF_8)).contains("Dry run");
    }

    @Test
    @DisplayName("replay에 execute 옵션이 있으면 export 파일의 이벤트를 대상 토픽으로 발행한다")
    void run_replayWithExecute_publishesRecords() {
        Path input = writeInputFile(record("notification-1", 12), record("notification-2", 13));
        RecordingReplayClient replayClient = new RecordingReplayClient();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = DlqOpsApplication.run(
                new String[]{"replay", "--input", input.toString(), "--target-topic", "notifications.retry", "--execute"},
                new PrintStream(output),
                System.err,
                options -> List.of(),
                codec,
                replayClient
        );

        assertThat(exitCode).isZero();
        assertThat(replayClient.published).hasSize(2);
        assertThat(replayClient.targetTopics).containsExactly("notifications.retry");
        assertThat(output.toString(StandardCharsets.UTF_8)).contains("Replayed 2 record(s)");
    }

    @Test
    @DisplayName("replay에 input 옵션이 없으면 실패한다")
    void run_replayWithoutInput_returnsFailure() {
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int exitCode = DlqOpsApplication.run(
                new String[]{"replay"},
                System.out,
                new PrintStream(error),
                options -> List.of(),
                codec,
                new RecordingReplayClient()
        );

        assertThat(exitCode).isEqualTo(1);
        assertThat(error.toString(StandardCharsets.UTF_8)).contains("Missing --input");
    }

    private Path writeInputFile(DlqRecord... records) {
        Path input = tempDir.resolve("dlq.jsonl");
        codec.write(input, List.of(records));
        return input;
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

    private static class RecordingReplayClient implements DlqReplayClient {

        private final List<DlqRecord> published = new ArrayList<>();
        private final List<String> targetTopics = new ArrayList<>();

        @Override
        public void publish(DlqOptions options, List<DlqRecord> records) {
            published.addAll(records);
            targetTopics.add(options.targetTopic());
        }
    }
}
