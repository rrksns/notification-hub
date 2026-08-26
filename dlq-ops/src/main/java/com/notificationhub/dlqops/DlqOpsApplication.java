// DLQ 메시지 조회와 재처리를 실행하는 운영 CLI 진입점
package com.notificationhub.dlqops;

import java.io.PrintStream;
import java.util.Arrays;

public class DlqOpsApplication {

    private static final String HELP = """
            Usage: dlq-ops <command> [options]

            Commands:
              list      List messages from notifications.dlq
              export    Export messages from notifications.dlq to JSON Lines
              replay    Replay exported messages to notifications

            Options:
              -h, --help    Show this help
            """;

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        return run(args, System.out, System.err);
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            err.println("Missing command. Run with --help.");
            return 1;
        }

        if (hasHelp(args)) {
            out.print(HELP);
            return 0;
        }

        String command = args[0];
        if (isKnownCommand(command)) {
            err.printf("Command '%s' is not implemented yet.%n", command);
            return 2;
        }

        err.printf("Unknown command '%s'. Run with --help.%n", command);
        return 1;
    }

    private static boolean hasHelp(String[] args) {
        return Arrays.stream(args)
                .anyMatch(arg -> "--help".equals(arg) || "-h".equals(arg));
    }

    private static boolean isKnownCommand(String command) {
        return "list".equals(command) || "export".equals(command) || "replay".equals(command);
    }
}
