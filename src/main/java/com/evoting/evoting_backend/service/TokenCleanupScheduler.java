package com.evoting.evoting_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenCleanupScheduler {
    
    @Autowired
    private AtomicTokenService atomicTokenService;
    
    @Autowired
    private MonitoringService monitoringService;
    
    /**
     * ✅ Scheduled cleanup of expired tokens every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredTokens() {
        try {
            int cleanedCount = atomicTokenService.cleanupExpiredTokens();
            if (cleanedCount > 0) {
                monitoringService.recordSecurityEvent("tokens_cleaned_" + cleanedCount);
            }
        } catch (Exception e) {
            monitoringService.recordSecurityEvent("token_cleanup_error");
        }
    }
}