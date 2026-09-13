// DLQ 운영 CLI의 기본 실행 표면을 검증하는 테스트
package com.notificationhub.dlqops;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DlqOpsApplicationTest {

    @Test
    @DisplayName("도움말 옵션은 성공 종료한다")
    void run_help_returnsSuccess() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = DlqOpsApplication.run(
                new String[]{"--help"},
                new PrintStream(output),
                System.err
        );

        assertThat(exitCode).isZero();
        assertThat(output.toString(StandardCharsets.UTF_8)).contains("Usage: dlq-ops");
    }

    @Test
    @DisplayName("명령이 없으면 실패 종료한다")
    void run_withoutCommand_returnsFailure() {
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int exitCode = DlqOpsApplication.run(
                new String[]{},
                System.out,
                new PrintStream(error)
        );

        assertThat(exitCode).isEqualTo(1);
        assertThat(error.toString(StandardCharsets.UTF_8)).contains("Missing command");
    }

    @Test
    @DisplayName("지원하지 않는 옵션은 실패 종료한다")
    void run_unknownOption_returnsFailure() {
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int exitCode = DlqOpsApplication.run(
                new String[]{"list", "--unknown"},
                System.out,
                new PrintStream(error)
        );

        assertThat(exitCode).isEqualTo(1);
        assertThat(error.toString(StandardCharsets.UTF_8)).contains("Unknown option");
    }
}
