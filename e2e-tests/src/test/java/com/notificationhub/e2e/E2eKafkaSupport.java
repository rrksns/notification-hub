// E2E 테스트에서 Kafka 토픽 생성과 이벤트 소비를 담당하는 테스트 헬퍼
package com.notificationhub.e2e;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

final class E2eKafkaSupport {

    private E2eKafkaSupport() {
    }

    static void createTopics(String bootstrapServers, String... topicNames) {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        List<NewTopic> topics = Arrays.stream(topicNames)
                .map(name -> new NewTopic(name, 1, (short) 1))
                .toList();

        try (AdminClient adminClient = AdminClient.create(properties)) {
            adminClient.createTopics(topics).all().get();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Kafka topics", e);
        }
    }

    static <T> void publishEvent(String bootstrapServers, String topic, String key, T event) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        try (KafkaProducer<String, T> producer = new KafkaProducer<>(
                properties,
                new StringSerializer(),
                new JsonSerializer<>()
        )) {
            producer.send(new ProducerRecord<>(topic, key, event)).get();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish Kafka event", e);
        }
    }

    static <T> T readSingleEvent(
            String bootstrapServers,
            String groupId,
            String topic,
            Class<T> eventType,
            Duration timeout
    ) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        JsonDeserializer<T> valueDeserializer = new JsonDeserializer<>(eventType);
        valueDeserializer.addTrustedPackages("com.notificationhub.common.event");

        try (KafkaConsumer<String, T> consumer = new KafkaConsumer<>(
                properties,
                new StringDeserializer(),
                valueDeserializer
        )) {
            consumer.subscribe(List.of(topic));
            long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(500))) {
                    return record.value();
                }
            }
        }

        throw new AssertionError("No Kafka event received from topic " + topic);
    }
}
