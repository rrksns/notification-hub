package com.notificationhub.analytics.infrastructure.messaging;

import com.notificationhub.common.event.DeliveryResultEvent;
import com.notificationhub.common.kafka.KafkaSecurityProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.kafka.properties.security.protocol:PLAINTEXT}")
    private String securityProtocol;
    @Value("${spring.kafka.properties.sasl.mechanism:}")
    private String saslMechanism;
    @Value("${spring.kafka.properties.sasl.jaas.config:}")
    private String saslJaasConfig;
    @Value("${spring.kafka.properties.ssl.truststore.certificates:}")
    private String sslTruststoreCertificates;
    @Value("${spring.kafka.properties.ssl.truststore.password:}")
    private String sslTruststorePassword;

    @Bean
    public ConsumerFactory<String, DeliveryResultEvent> consumerFactory() {
        JsonDeserializer<DeliveryResultEvent> deserializer = new JsonDeserializer<>(DeliveryResultEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("com.notificationhub.common.event");
        deserializer.setUseTypeMapperForKey(true);
        Map<String, Object> props = new java.util.HashMap<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "analytics-service",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class
        ));
        KafkaSecurityProperties.apply(props, securityProtocol, saslMechanism, saslJaasConfig,
                sslTruststoreCertificates, sslTruststorePassword);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryResultEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeliveryResultEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
