package com.notificationhub.delivery;

import com.notificationhub.delivery.infrastructure.sender.EmailDeliveryProperties;
import com.notificationhub.delivery.infrastructure.sender.SendGridProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import com.notificationhub.common.jwt.JwtProperties;

@SpringBootApplication(scanBasePackages = {"com.notificationhub.delivery", "com.notificationhub.common"})
@EnableRetry
@EnableConfigurationProperties({
        JwtProperties.class,
        EmailDeliveryProperties.class,
        SendGridProperties.class
})
public class DeliveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
