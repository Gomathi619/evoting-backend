package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.dto.ApiResponse;
import com.evoting.evoting_backend.service.EnhancedDeathRegistryService; // Use Enhanced service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/death-registry")
@PreAuthorize("hasRole('ADMIN') or hasRole('ELECTION_OFFICER')")
public class DeathRegistryController {
    
    @Autowired
    private EnhancedDeathRegistryService deathRegistryService; // Use Enhanced service
    
    @PostMapping("/verify")
    public ApiResponse verifyPerson(@RequestBody DeathVerificationRequest request) {
        try {
            boolean isAlive = deathRegistryService.isPersonAlive(
                request.getGovernmentId(), 
                request.getFullName(), 
                request.getDateOfBirth()
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("governmentId", deathRegistryService.maskGovernmentId(request.getGovernmentId()));
            result.put("isAlive", isAlive);
            result.put("timestamp", System.currentTimeMillis());
            result.put("status", isAlive ? "ALIVE" : "DECEASED");
            
            return new ApiResponse(true, 
                isAlive ? "Person is alive according to death registry" : "Person is deceased according to death registry", 
                result);
            
        } catch (Exception e) {
            return new ApiResponse(false, "Death registry verification failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/health")
    public ApiResponse healthCheck() {
        try {
            Map<String, Object> stats = deathRegistryService.getCacheStats();
            
            Map<String, Object> result = new HashMap<>();
            result.put("service", "EnhancedDeathRegistryService");
            result.put("status", "HEALTHY");
            result.put("timestamp", System.currentTimeMillis());
            result.put("cacheStats", stats);
            
            return new ApiResponse(true, "Death registry service health check completed", result);
            
        } catch (Exception e) {
            return new ApiResponse(false, "Death registry health check failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/cache/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse clearCache() {
        try {
            deathRegistryService.clearExpiredCache();
            return new ApiResponse(true, "Death registry cache cleared successfully");
            
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to clear death registry cache: " + e.getMessage());
        }
    }
    
    @GetMapping("/cache/stats")
    public ApiResponse getCacheStats() {
        try {
            Map<String, Object> stats = deathRegistryService.getCacheStats();
            return new ApiResponse(true, "Death registry cache statistics", stats);
            
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to get cache statistics: " + e.getMessage());
        }
    }
    
    // Request DTO
    public static class DeathVerificationRequest {
        private String governmentId;
        private String fullName;
        private LocalDate dateOfBirth;
        
        public String getGovernmentId() { return governmentId; }
        public void setGovernmentId(String governmentId) { this.governmentId = governmentId; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public LocalDate getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    }
}