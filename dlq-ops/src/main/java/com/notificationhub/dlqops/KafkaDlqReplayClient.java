// Kafka로 DLQ 레코드 이벤트를 재발행하는 운영 CLI producer adapter
package com.notificationhub.dlqops;

import com.notificationhub.common.event.NotificationEvent;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

class KafkaDlqReplayClient implements DlqReplayClient {

    @Override
    public void publish(DlqOptions options, List<DlqRecord> records) {
        try (KafkaProducer<String, NotificationEvent> producer = new KafkaProducer<>(properties(options))) {
            for (DlqRecord record : records) {
                producer.send(new ProducerRecord<>(options.targetTopic(), key(record), record.event())).get();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while replaying DLQ record", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Failed to replay DLQ record", exception);
        }
    }

    private Properties properties(DlqOptions options) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, options.bootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return properties;
    }

    private String key(DlqRecord record) {
        if (record.key() != null && !record.key().isBlank()) {
            return record.key();
        }
        return record.event().notificationId();
    }
}
