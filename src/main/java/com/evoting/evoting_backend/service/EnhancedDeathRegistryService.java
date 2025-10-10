package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.DeathRegistryCache;
import com.evoting.evoting_backend.repository.DeathRegistryCacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class EnhancedDeathRegistryService {
    
    @Autowired
    private DeathRegistryCacheRepository cacheRepository;
    
    @Autowired
    private ImmutableAuditService auditService;
    
    @Value("${death.registry.api.url:https://api.government-death-registry.com/v1/verify}")
    private String deathRegistryApiUrl;
    
    @Value("${death.registry.api.key:demo-key}")
    private String deathRegistryApiKey;
    
    private final String HMAC_SECRET = "Secure-Death-Registry-HMAC-2024";
    
    public boolean isPersonAlive(String governmentId, String fullName, LocalDate dateOfBirth) {
        try {
            String governmentIdHash = hashGovernmentId(governmentId);
            
            // Check cache first
            Optional<DeathRegistryCache> cached = cacheRepository.findById(governmentIdHash);
            if (cached.isPresent() && cached.get().isValid()) {
                auditService.logEvent("DEATH_REGISTRY_CACHE_HIT", "EnhancedDeathRegistryService",
                    "isPersonAlive", "government_id:" + maskGovernmentId(governmentId) + 
                    ", cached_result:" + cached.get().isAlive());
                return cached.get().isAlive();
            }
            
            // Call external API
            boolean isAlive = callDeathRegistryAPI(governmentId, fullName, dateOfBirth);
            
            // Cache result
            DeathRegistryCache cacheEntry = new DeathRegistryCache();
            cacheEntry.setGovernmentIdHash(governmentIdHash);
            cacheEntry.setAlive(isAlive);
            cacheEntry.setExpiresAt(LocalDateTime.now().plusHours(24)); // Cache for 24 hours
            cacheRepository.save(cacheEntry);
            
            // Audit
            auditService.logEvent("DEATH_REGISTRY_CHECK", "EnhancedDeathRegistryService", 
                "isPersonAlive", String.format("gov_id:%s, full_name:%s, is_alive:%b", 
                maskGovernmentId(governmentId), fullName, isAlive));
            
            return isAlive;
            
        } catch (Exception e) {
            auditService.logEvent("DEATH_REGISTRY_ERROR", "EnhancedDeathRegistryService",
                "isPersonAlive", "government_id:" + maskGovernmentId(governmentId) + 
                ", error:" + e.getMessage());
            
            // Fail-open: assume alive if check fails
            return true;
        }
    }
    
    private String hashGovernmentId(String governmentId) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(HMAC_SECRET.getBytes(), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(governmentId.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC calculation failed for death registry");
        }
    }
    
    private boolean callDeathRegistryAPI(String governmentId, String fullName, LocalDate dateOfBirth) {
        try {
            // Simulate API call - in production, this would be a real HTTP call
            // For demo purposes, simulate different responses
            
            // Simulation logic (REMOVE IN PRODUCTION)
            if (isDemoDeceasedPerson(governmentId, fullName)) {
                return false; // Deceased
            }
            
            // Simulate API delay
            Thread.sleep(50);
            
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
        java.util.Map<String, String> demoDeceased = new java.util.HashMap<>();
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
    
    public void clearExpiredCache() {
        try {
            LocalDateTime now = LocalDateTime.now();
            java.util.List<DeathRegistryCache> expiredEntries = cacheRepository.findExpiredEntries(now);
            cacheRepository.deleteAll(expiredEntries);
            
            auditService.logEvent("DEATH_REGISTRY_CACHE_CLEANUP", "EnhancedDeathRegistryService",
                "clearExpiredCache", "cleaned_entries:" + expiredEntries.size());
        } catch (Exception e) {
            auditService.logEvent("DEATH_REGISTRY_CACHE_CLEANUP_ERROR", "EnhancedDeathRegistryService",
                "clearExpiredCache", "error:" + e.getMessage());
        }
    }
    
    public java.util.Map<String, Object> getCacheStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalEntries", cacheRepository.count());
        stats.put("aliveEntries", cacheRepository.countAliveEntries());
        stats.put("deceasedEntries", cacheRepository.countDeceasedEntries());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }
}