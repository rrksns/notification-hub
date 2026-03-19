package com.notificationhub.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.notificationhub.common.jwt.JwtProperties;

@SpringBootApplication(scanBasePackages = {"com.notificationhub.notification", "com.notificationhub.common"})
@EnableConfigurationProperties(JwtProperties.class)
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
