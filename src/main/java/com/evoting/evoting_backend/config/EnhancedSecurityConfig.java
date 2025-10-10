package com.evoting.evoting_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableScheduling
@EnableRetry
public class EnhancedSecurityConfig {
    // Services are automatically created with @Service annotation
    // No need for manual bean definitions
}