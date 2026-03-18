package com.notificationhub.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.notificationhub.common.jwt.JwtProperties;

@SpringBootApplication(scanBasePackages = {"com.notificationhub.gateway", "com.notificationhub.common"})
@EnableConfigurationProperties(JwtProperties.class)
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
