// Kafka TLS/SASL 속성 적용 동작을 검증하는 테스트
package com.notificationhub.common.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KafkaSecurityPropertiesTest {

    @Test
    @DisplayName("SASL_SSL 보안 속성을 Kafka client 설정에 적용한다")
    void apply_withSaslSsl_addsSecurityProperties() {
        Map<String, Object> properties = new HashMap<>();

        KafkaSecurityProperties.apply(
                properties,
                "SASL_SSL",
                "SCRAM-SHA-512",
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"user\" password=\"password\";",
                "-----BEGIN CERTIFICATE-----cert-----END CERTIFICATE-----",
                "trust-password"
        );

        assertThat(properties).containsEntry("security.protocol", "SASL_SSL");
        assertThat(properties).containsEntry("sasl.mechanism", "SCRAM-SHA-512");
        assertThat(properties).containsKey("sasl.jaas.config");
        assertThat(properties).containsKey("ssl.truststore.certificates");
        assertThat(properties).containsEntry("ssl.truststore.password", "trust-password");
    }

    @Test
    @DisplayName("로컬 PLAINTEXT 설정에서는 선택적 TLS/SASL 속성을 생략한다")
    void apply_withPlaintext_omitsBlankOptionalProperties() {
        Map<String, Object> properties = new HashMap<>();

        KafkaSecurityProperties.apply(properties, "PLAINTEXT", "", "", "", "");

        assertThat(properties).containsEntry("security.protocol", "PLAINTEXT");
        assertThat(properties).doesNotContainKeys(
                "sasl.mechanism",
                "sasl.jaas.config",
                "ssl.truststore.certificates",
                "ssl.truststore.password"
        );
    }
}
