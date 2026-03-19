package com.notificationhub.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.notificationhub.common.jwt.JwtProperties;

@SpringBootApplication(scanBasePackages = {"com.notificationhub.analytics", "com.notificationhub.common"})
@EnableConfigurationProperties(JwtProperties.class)
public class AnalyticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
