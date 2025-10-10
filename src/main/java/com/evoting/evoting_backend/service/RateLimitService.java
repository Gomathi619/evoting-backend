package com.evoting.evoting_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {
    
    @Autowired
    private RedisTemplate<String, Long> redisTemplate;
    
    @Autowired
    private MonitoringService monitoringService;
    
    @Autowired
    private ImmutableAuditService auditService;
    
    @Value("${rate.limit.fallback.allow:true}")
    private boolean fallbackAllow;
    
    /**
     * ✅ Rate limiting with sliding window algorithm
     */
    public boolean isAllowed(String key, int maxRequests, int timeWindow) {
        try {
            String redisKey = "rate_limit:" + key;
            Long currentCount = redisTemplate.opsForValue().get(redisKey);
            
            if (currentCount == null) {
                // First request in window
                redisTemplate.opsForValue().set(redisKey, 1L, timeWindow, TimeUnit.SECONDS);
                return true;
            }
            
            if (currentCount >= maxRequests) {
                // Rate limited
                monitoringService.recordSecurityEvent("rate_limit_triggered");
                auditService.logEvent("RATE_LIMIT_TRIGGERED", "RateLimitService", 
                    "isAllowed", "key:" + maskKey(key) + ", count:" + currentCount + ", max:" + maxRequests);
                return false;
            }
            
            // Increment counter
            redisTemplate.opsForValue().increment(redisKey);
            return true;
            
        } catch (Exception e) {
            // Fail open - allow request if Redis fails
            monitoringService.recordSecurityEvent("rate_limit_error");
            auditService.logEvent("RATE_LIMIT_ERROR", "RateLimitService", 
                "isAllowed", "key:" + maskKey(key) + ", error:" + e.getMessage());
            return fallbackAllow;
        }
    }
    
    /**
     * ✅ IP-based rate limiting for anonymous endpoints
     */
    public boolean isIpAllowed(String ipAddress, String endpoint) {
        String key = "ip:" + ipAddress + ":" + endpoint;
        
        // Stricter limits for sensitive endpoints
        if (endpoint.contains("/api/secure-votes") || endpoint.contains("/api/kyc")) {
            return isAllowed(key, 5, 60); // 5 requests per minute for voting
        } else if (endpoint.contains("/api/auth")) {
            return isAllowed(key, 10, 300); // 10 requests per 5 minutes for auth
        } else {
            return isAllowed(key, 100, 3600); // 100 requests per hour for general
        }
    }
    
    /**
     * ✅ User-based rate limiting for authenticated endpoints
     */
    public boolean isUserAllowed(String username, String endpoint) {
        String key = "user:" + username + ":" + endpoint;
        
        if (endpoint.contains("/api/secure-votes")) {
            return isAllowed(key, 3, 300); // 3 votes per 5 minutes per user
        } else if (endpoint.contains("/api/kyc")) {
            return isAllowed(key, 2, 3600); // 2 KYC attempts per hour
        } else {
            return isAllowed(key, 50, 3600); // 50 requests per hour general
        }
    }
    
    /**
     * ✅ Global rate limiting for election endpoints
     */
    public boolean isElectionEndpointAllowed(Long electionId, String endpoint) {
        String key = "election:" + electionId + ":" + endpoint;
        return isAllowed(key, 1000, 60); // 1000 requests per minute per election
    }
    
    /**
     * ✅ Get current rate limit status
     */
    public RateLimitStatus getRateLimitStatus(String key) {
        try {
            Long currentCount = redisTemplate.opsForValue().get("rate_limit:" + key);
            Long ttl = redisTemplate.getExpire("rate_limit:" + key, TimeUnit.SECONDS);
            
            return new RateLimitStatus(
                currentCount != null ? currentCount : 0,
                ttl != null ? ttl : 0
            );
        } catch (Exception e) {
            return new RateLimitStatus(0, 0);
        }
    }
    
    private String maskKey(String key) {
        if (key == null || key.length() < 10) return "***";
        return key.substring(0, 6) + "***" + key.substring(key.length() - 4);
    }
    
    public static class RateLimitStatus {
        private long currentRequests;
        private long remainingTimeSeconds;
        
        public RateLimitStatus(long currentRequests, long remainingTimeSeconds) {
            this.currentRequests = currentRequests;
            this.remainingTimeSeconds = remainingTimeSeconds;
        }
        
        // Getters
        public long getCurrentRequests() { return currentRequests; }
        public long getRemainingTimeSeconds() { return remainingTimeSeconds; }
    }
}