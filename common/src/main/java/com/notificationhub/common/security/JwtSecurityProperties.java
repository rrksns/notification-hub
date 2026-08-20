// 내부 서비스 JWT 보안 제외 경로 설정을 담는 properties
package com.notificationhub.common.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt.security")
public record JwtSecurityProperties(List<String> excludedPaths) {

    public JwtSecurityProperties {
        excludedPaths = excludedPaths == null ? List.of() : List.copyOf(excludedPaths);
    }
}
