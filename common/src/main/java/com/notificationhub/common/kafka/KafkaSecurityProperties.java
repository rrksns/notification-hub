// Kafka Producer와 Consumer에 TLS/SASL 보안 속성을 적용하는 공통 도우미
package com.notificationhub.common.kafka;

import java.util.Map;

public final class KafkaSecurityProperties {

    private KafkaSecurityProperties() {
    }

    public static void apply(
            Map<String, Object> properties,
            String securityProtocol,
            String saslMechanism,
            String saslJaasConfig,
            String sslTruststoreCertificates,
            String sslTruststorePassword
    ) {
        properties.put("security.protocol", securityProtocol);
        putIfHasText(properties, "sasl.mechanism", saslMechanism);
        putIfHasText(properties, "sasl.jaas.config", saslJaasConfig);
        putIfHasText(properties, "ssl.truststore.certificates", sslTruststoreCertificates);
        putIfHasText(properties, "ssl.truststore.password", sslTruststorePassword);
    }

    private static void putIfHasText(Map<String, Object> properties, String key, String value) {
        if (value != null && !value.isBlank()) {
            properties.put(key, value);
        }
    }
}
