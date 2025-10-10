package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.dto.ApiResponse;
import com.evoting.evoting_backend.service.BlindSignatureService;
import com.evoting.evoting_backend.service.HSMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hsm")
@PreAuthorize("hasRole('ADMIN')")
public class HSMController {
    
    @Autowired
    private BlindSignatureService blindSignatureService;
    
    @Autowired
    private HSMService hsmService;
    
    @GetMapping("/status")
    public ApiResponse getHSMStatus() {
        try {
            Map<String, Object> status = blindSignatureService.getHSMStatus();
            status.put("service", "Blind Signature Service");
            status.put("hsmHealth", hsmService.healthCheck() ? "HEALTHY" : "DEGRADED");
            
            return new ApiResponse(true, "HSM status retrieved", status);
            
        } catch (Exception e) {
            return new ApiResponse(false, "HSM status check failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/rotate-keys")
    public ApiResponse rotateKeys() {
        try {
            blindSignatureService.rotateHSMKeys();
            return new ApiResponse(true, "HSM keys rotated successfully");
            
        } catch (Exception e) {
            return new ApiResponse(false, "Key rotation failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/health")
    public ApiResponse healthCheck() {
        try {
            // Perform comprehensive health check
            boolean hsmHealthy = blindSignatureService.isHSMOperational();
            boolean hsmServiceHealthy = hsmService.healthCheck();
            
            String overallStatus = (hsmHealthy && hsmServiceHealthy) ? "HEALTHY" : "DEGRADED";
            
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", overallStatus);
            healthInfo.put("hsmIntegrated", true);
            healthInfo.put("blindSignatureHSM", hsmHealthy);
            healthInfo.put("hsmService", hsmServiceHealthy);
            healthInfo.put("timestamp", System.currentTimeMillis());
            healthInfo.put("keysInStore", hsmService.getKeyIds().size());
            
            if (!hsmHealthy || !hsmServiceHealthy) {
                healthInfo.put("recommendation", "Check HSM configuration and restart service if needed");
            } else {
                healthInfo.put("recommendation", "All HSM systems operational");
            }
            
            return new ApiResponse(true, "Health check completed", healthInfo);
            
        } catch (Exception e) {
            return new ApiResponse(false, "Health check failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/keys")
    public ApiResponse listKeys() {
        try {
            Map<String, Object> keyInfo = new HashMap<>();
            keyInfo.put("keyIds", hsmService.getKeyIds());
            keyInfo.put("totalKeys", hsmService.getKeyIds().size());
            keyInfo.put("timestamp", System.currentTimeMillis());
            
            return new ApiResponse(true, "HSM keys retrieved", keyInfo);
            
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to retrieve HSM keys: " + e.getMessage());
        }
    }
}