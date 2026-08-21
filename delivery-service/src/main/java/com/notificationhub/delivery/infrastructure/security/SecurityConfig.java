package com.notificationhub.delivery.infrastructure.security;

import com.notificationhub.common.jwt.JwtTokenProvider;
import com.notificationhub.common.security.JwtHeaderAuthenticationFilter;
import com.notificationhub.common.security.JwtSecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class SecurityConfig {

    @Bean
    public JwtHeaderAuthenticationFilter jwtHeaderAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            JwtSecurityProperties jwtSecurityProperties
    ) {
        return new JwtHeaderAuthenticationFilter(jwtTokenProvider, jwtSecurityProperties);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtHeaderAuthenticationFilter jwtHeaderAuthenticationFilter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
