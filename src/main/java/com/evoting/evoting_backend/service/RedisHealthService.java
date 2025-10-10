package com.evoting.evoting_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisHealthService {
    
    @Autowired
    private RedisTemplate<String, Long> redisTemplate;
    
    @Autowired
    private MonitoringService monitoringService;
    
    public boolean isRedisHealthy() {
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            boolean isHealthy = "PONG".equals(connection.ping());
            connection.close();
            
            monitoringService.setGauge("health.redis", isHealthy ? 1 : 0);
            return isHealthy;
            
        } catch (Exception e) {
            monitoringService.setGauge("health.redis", 0);
            return false;
        }
    }
}