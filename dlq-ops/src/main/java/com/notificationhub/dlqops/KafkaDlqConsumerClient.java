// Kafka에서 DLQ 레코드를 읽어오는 운영 CLI consumer adapter
package com.notificationhub.dlqops;

import com.notificationhub.common.event.NotificationEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

class KafkaDlqConsumerClient implements DlqConsumerClient {

    @Override
    public List<DlqRecord> read(DlqOptions options) {
        List<DlqRecord> records = new ArrayList<>();
        try (KafkaConsumer<String, NotificationEvent> consumer = new KafkaConsumer<>(properties(options))) {
            consumer.subscribe(List.of(options.topic()));
            Instant deadline = Instant.now().plusSeconds(options.timeoutSeconds());

            while (records.size() < options.limit() && Instant.now().isBefore(deadline)) {
                Duration remaining = Duration.between(Instant.now(), deadline);
                Duration pollTimeout = remaining.compareTo(Duration.ofMillis(250)) < 0 ? remaining : Duration.ofMillis(250);
                if (pollTimeout.isNegative() || pollTimeout.isZero()) {
                    break;
                }

                for (ConsumerRecord<String, NotificationEvent> record : consumer.poll(pollTimeout)) {
                    records.add(toDlqRecord(record));
                    if (records.size() >= options.limit()) {
                        break;
                    }
                }
            }
        }
        return records;
    }

    private Properties properties(DlqOptions options) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, options.bootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, options.groupId());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, options.limit());
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, NotificationEvent.class.getName());
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.notificationhub.common.event");
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return properties;
    }

    private DlqRecord toDlqRecord(ConsumerRecord<String, NotificationEvent> record) {
        return new DlqRecord(
                record.topic(),
                record.partition(),
                record.offset(),
                Instant.ofEpochMilli(record.timestamp()),
                record.key(),
                record.value()
        );
    }
}
