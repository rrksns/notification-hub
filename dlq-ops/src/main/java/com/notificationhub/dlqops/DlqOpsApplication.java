// DLQ 메시지 조회와 재처리를 실행하는 운영 CLI 진입점
package com.notificationhub.dlqops;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

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

    private final DlqConsumerClient consumerClient;
    private final DlqReplayClient replayClient;
    private final DlqRecordCodec recordCodec;

    DlqOpsApplication(DlqConsumerClient consumerClient, DlqReplayClient replayClient, DlqRecordCodec recordCodec) {
        this.consumerClient = consumerClient;
        this.replayClient = replayClient;
        this.recordCodec = recordCodec;
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        return run(args, System.out, System.err);
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        return run(args, out, err, new KafkaDlqConsumerClient(), new DlqRecordCodec(), new KafkaDlqReplayClient());
    }

    static int run(
            String[] args,
            PrintStream out,
            PrintStream err,
            DlqConsumerClient consumerClient,
            DlqRecordCodec recordCodec
    ) {
        return run(args, out, err, consumerClient, recordCodec, new KafkaDlqReplayClient());
    }

    static int run(
            String[] args,
            PrintStream out,
            PrintStream err,
            DlqConsumerClient consumerClient,
            DlqRecordCodec recordCodec,
            DlqReplayClient replayClient
    ) {
        return new DlqOpsApplication(consumerClient, replayClient, recordCodec).execute(args, out, err);
    }

    private int execute(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            err.println("Missing command. Run with --help.");
            return 1;
        }

        if (hasHelp(args)) {
            out.print(HELP);
            return 0;
        }

        try {
            DlqOptions options = DlqOptionsParser.parse(args);
            return switch (options.command()) {
                case LIST -> list(options, out);
                case EXPORT -> export(options, out, err);
                case REPLAY -> replay(options, out, err);
            };
        } catch (IllegalArgumentException exception) {
            err.println(exception.getMessage() + ". Run with --help.");
            return 1;
        }
    }

    private int list(DlqOptions options, PrintStream out) {
        List<DlqRecord> records = filteredRecords(options);
        out.println("offset\tpartition\tkey\tnotificationId\ttenantId\tchannel\trecipient");
        records.forEach(record -> out.printf(
                "%d\t%d\t%s\t%s\t%s\t%s\t%s%n",
                record.offset(),
                record.partition(),
                value(record.key()),
                record.event().notificationId(),
                record.event().tenantId(),
                record.event().channel(),
                record.event().recipient()
        ));
        out.printf("Listed %d record(s).%n", records.size());
        return 0;
    }

    private int export(DlqOptions options, PrintStream out, PrintStream err) {
        if (options.output() == null) {
            err.println("Missing --output for export. Run with --help.");
            return 1;
        }

        List<DlqRecord> records = filteredRecords(options);
        recordCodec.write(Path.of(options.output()), records);
        out.printf("Exported %d record(s) to %s.%n", records.size(), options.output());
        return 0;
    }

    private int replay(DlqOptions options, PrintStream out, PrintStream err) {
        if (options.input() == null) {
            err.println("Missing --input for replay. Run with --help.");
            return 1;
        }

        List<DlqRecord> records = filteredRecords(recordCodec.read(Path.of(options.input())), options);
        if (!options.execute()) {
            out.printf("Dry run: %d record(s) would be replayed to %s.%n", records.size(), options.targetTopic());
            return 0;
        }

        replayClient.publish(options, records);
        out.printf("Replayed %d record(s) to %s.%n", records.size(), options.targetTopic());
        return 0;
    }

    private List<DlqRecord> filteredRecords(DlqOptions options) {
        return filteredRecords(consumerClient.read(options), options);
    }

    private List<DlqRecord> filteredRecords(List<DlqRecord> records, DlqOptions options) {
        DlqEventFilter filter = DlqEventFilter.from(options);
        return records.stream()
                .filter(record -> filter.matches(record.event()))
                .limit(options.limit())
                .toList();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasHelp(String[] args) {
        return Arrays.stream(args)
                .anyMatch(arg -> "--help".equals(arg) || "-h".equals(arg));
    }

}
