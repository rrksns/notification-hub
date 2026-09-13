// DLQ 운영 CLI 인자 파싱 규칙을 검증하는 테스트
package com.notificationhub.dlqops;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DlqOptionsParserTest {

    @Test
    @DisplayName("list 명령은 안전한 기본값을 가진다")
    void parse_list_usesDefaults() {
        DlqOptions options = DlqOptionsParser.parse(new String[]{"list"});

        assertThat(options.command()).isEqualTo(DlqCommand.LIST);
        assertThat(options.bootstrapServers()).isEqualTo("localhost:9092");
        assertThat(options.topic()).isEqualTo("notifications.dlq");
        assertThat(options.targetTopic()).isEqualTo("notifications");
        assertThat(options.groupId()).startsWith("dlq-ops-");
        assertThat(options.limit()).isEqualTo(50);
        assertThat(options.timeoutSeconds()).isEqualTo(5);
        assertThat(options.execute()).isFalse();
    }

    @Test
    @DisplayName("export 명령은 필터와 출력 파일 옵션을 파싱한다")
    void parse_export_withFiltersAndOutput() {
        DlqOptions options = DlqOptionsParser.parse(new String[]{
                "export",
                "--bootstrap-servers", "kafka:9092",
                "--topic", "notifications.dlq",
                "--group-id", "ops-review",
                "--limit", "10",
                "--timeout-seconds", "3",
                "--tenant-id", "tenant-1",
                "--notification-id", "notification-1",
                "--channel", "EMAIL",
                "--output", "dlq.jsonl"
        });

        assertThat(options.command()).isEqualTo(DlqCommand.EXPORT);
        assertThat(options.bootstrapServers()).isEqualTo("kafka:9092");
        assertThat(options.groupId()).isEqualTo("ops-review");
        assertThat(options.limit()).isEqualTo(10);
        assertThat(options.timeoutSeconds()).isEqualTo(3);
        assertThat(options.tenantId()).isEqualTo("tenant-1");
        assertThat(options.notificationId()).isEqualTo("notification-1");
        assertThat(options.channel()).isEqualTo("EMAIL");
        assertThat(options.output()).isEqualTo("dlq.jsonl");
    }

    @Test
    @DisplayName("replay 명령은 입력 파일과 실행 플래그를 파싱한다")
    void parse_replay_withInputAndExecute() {
        DlqOptions options = DlqOptionsParser.parse(new String[]{
                "replay",
                "--input", "dlq.jsonl",
                "--target-topic", "notifications.retry",
                "--execute"
        });

        assertThat(options.command()).isEqualTo(DlqCommand.REPLAY);
        assertThat(options.input()).isEqualTo("dlq.jsonl");
        assertThat(options.targetTopic()).isEqualTo("notifications.retry");
        assertThat(options.execute()).isTrue();
    }

    @Test
    @DisplayName("알 수 없는 명령은 예외가 발생한다")
    void parse_unknownCommand_throws() {
        assertThatThrownBy(() -> DlqOptionsParser.parse(new String[]{"unknown"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown command");
    }

    @Test
    @DisplayName("값이 필요한 옵션에 값이 없으면 예외가 발생한다")
    void parse_missingOptionValue_throws() {
        assertThatThrownBy(() -> DlqOptionsParser.parse(new String[]{"list", "--topic"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing value");
    }
}
