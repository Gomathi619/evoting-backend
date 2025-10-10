package com.evoting.evoting_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class DeathRegistryService {
    
    @Autowired
    private ImmutableAuditService auditService;
    
    @Value("${death.registry.api.url:https://api.government-death-registry.com/v1/verify}")
    private String deathRegistryApiUrl;
    
    @Value("${death.registry.api.key:demo-key}")
    private String deathRegistryApiKey;
    
    // Cache to avoid duplicate API calls (in production, use Redis)
    private final Map<String, Boolean> deathRegistryCache = new HashMap<>();
    
    public boolean isPersonAlive(String governmentId, String fullName, LocalDate dateOfBirth) {
        try {
            // Create cache key
            String cacheKey = governmentId + "|" + fullName + "|" + dateOfBirth.toString();
            
            // Check cache first
            if (deathRegistryCache.containsKey(cacheKey)) {
                boolean isAlive = deathRegistryCache.get(cacheKey);
                auditService.logEvent("DEATH_REGISTRY_CHECK", "DeathRegistryService", 
                    "isPersonAlive", "government_id:" + maskGovernmentId(governmentId) + 
                    ", cached_result:" + isAlive);
                return isAlive;
            }
            
            // In production, this would call the real government API
            boolean isAlive = callDeathRegistryAPI(governmentId, fullName, dateOfBirth);
            
            // Cache the result
            deathRegistryCache.put(cacheKey, isAlive);
            
            // Log the check with HSM audit
            String auditData = String.format("government_id:%s, full_name:%s, dob:%s, is_alive:%b",
                maskGovernmentId(governmentId), fullName, dateOfBirth.toString(), isAlive);
            
            auditService.logEvent("DEATH_REGISTRY_CHECK", "DeathRegistryService", 
                "isPersonAlive", auditData);
            
            System.out.println("Death registry check: " + governmentId + " -> " + (isAlive ? "ALIVE" : "DECEASED"));
            
            return isAlive;
            
        } catch (Exception e) {
            // Log the failure but return true (fail-open for availability)
            // In production, you might want a different failure strategy
            auditService.logEvent("DEATH_REGISTRY_ERROR", "DeathRegistryService", 
                "isPersonAlive", "error:" + e.getMessage() + ", government_id:" + maskGovernmentId(governmentId));
            
            System.err.println("Death registry check failed for " + governmentId + ": " + e.getMessage());
            return true; // Fail-open: assume alive if check fails
        }
    }
    
    private boolean callDeathRegistryAPI(String governmentId, String fullName, LocalDate dateOfBirth) {
        try {
            // Simulate API call - in production, this would be a real HTTP call
            
            // For demo purposes, we'll simulate different responses
            // In production, remove this simulation and use real API
            
            // Simulation logic (REMOVE IN PRODUCTION)
            if (isDemoDeceasedPerson(governmentId, fullName)) {
                return false; // Deceased
            }
            
            // Simulate API delay
            Thread.sleep(100);
            
            // Default to alive for demo
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Death registry check interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Death registry API call failed: " + e.getMessage(), e);
        }
    }
    
    // Demo method - REMOVE IN PRODUCTION
    private boolean isDemoDeceasedPerson(String governmentId, String fullName) {
        // Simulate some deceased persons for demo
        Map<String, String> demoDeceased = new HashMap<>();
        demoDeceased.put("123456789012", "John Smith");
        demoDeceased.put("987654321098", "Jane Doe");
        demoDeceased.put("555555555555", "Test Deceased");
        
        return demoDeceased.containsKey(governmentId) && 
               demoDeceased.get(governmentId).equalsIgnoreCase(fullName);
    }
    
    public String maskGovernmentId(String governmentId) {
        if (governmentId == null || governmentId.length() < 8) {
            return "***";
        }
        return governmentId.substring(0, 4) + "***" + governmentId.substring(governmentId.length() - 4);
    }
    
    // Batch verification for efficiency
    public Map<String, Boolean> batchDeathRegistryCheck(Map<String, Map<String, Object>> persons) {
        Map<String, Boolean> results = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : persons.entrySet()) {
            String governmentId = entry.getKey();
            Map<String, Object> personData = entry.getValue();
            
            String fullName = (String) personData.get("fullName");
            LocalDate dateOfBirth = (LocalDate) personData.get("dateOfBirth");
            
            boolean isAlive = isPersonAlive(governmentId, fullName, dateOfBirth);
            results.put(governmentId, isAlive);
        }
        
        return results;
    }
    
    // Health check for death registry service
    public boolean healthCheck() {
        try {
            // Test with a known demo person
            boolean result = isPersonAlive("000000000000", "Health Check", LocalDate.of(2000, 1, 1));
            
            auditService.logEvent("DEATH_REGISTRY_HEALTH_CHECK", "DeathRegistryService", 
                "healthCheck", "status:" + (result ? "HEALTHY" : "DEGRADED"));
            
            return true; // Service is operational
            
        } catch (Exception e) {
            auditService.logEvent("DEATH_REGISTRY_HEALTH_FAIL", "DeathRegistryService", 
                "healthCheck", "error:" + e.getMessage());
            return false;
        }
    }
    
    // Clear cache (useful for testing)
    public void clearCache() {
        deathRegistryCache.clear();
        auditService.logEvent("DEATH_REGISTRY_CACHE_CLEAR", "DeathRegistryService", 
            "clearCache", "cache_cleared:true");
    }
    
    // Get cache statistics
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", deathRegistryCache.size());
        stats.put("cacheHits", deathRegistryCache.values().stream().filter(v -> v).count());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }
}