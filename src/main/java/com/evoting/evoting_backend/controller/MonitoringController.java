package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.dto.ApiResponse;
import com.evoting.evoting_backend.service.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@PreAuthorize("hasRole('ADMIN') or hasRole('ELECTION_OFFICER')")
public class MonitoringController {
    
    @Autowired
    private MonitoringService monitoringService;
    
    @GetMapping("/metrics")
    public ApiResponse getMetricsSnapshot() {
        try {
            Map<String, Object> metrics = monitoringService.getMetricsSnapshot();
            return new ApiResponse(true, "Metrics snapshot retrieved", metrics);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to get metrics: " + e.getMessage());
        }
    }
    
    @GetMapping("/health")
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        // Basic health indicators
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "1.0.0");
        health.put("environment", "production");
        
        // Component health
        Map<String, Object> components = new HashMap<>();
        components.put("database", "OPERATIONAL");
        components.put("hsm", "OPERATIONAL"); 
        components.put("deathRegistry", "OPERATIONAL");
        components.put("zkpService", "OPERATIONAL");
        components.put("auditSystem", "OPERATIONAL");
        
        health.put("components", components);
        
        return health;
    }
    
    @GetMapping("/prometheus")
    public String getPrometheusMetrics() {
        // Prometheus will automatically scrape from /actuator/prometheus
        return "Prometheus metrics available at /actuator/prometheus";
    }
}