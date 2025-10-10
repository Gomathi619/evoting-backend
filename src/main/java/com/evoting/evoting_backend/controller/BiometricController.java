package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.dto.ApiResponse;
import com.evoting.evoting_backend.service.BiometricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/biometric")
public class BiometricController {
    
    @Autowired
    private BiometricService biometricService;
    
    @PostMapping("/process-face")
    public ApiResponse<Map<String, Object>> processFacialImage(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("governmentId") String governmentId) {
        try {
            String templateId = biometricService.processFacialImage(imageFile, governmentId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("templateId", templateId);
            responseData.put("message", "Facial image processed successfully. Template will expire in 5 minutes.");
            responseData.put("expiresIn", "5 minutes");
            
            return new ApiResponse<>(true, "Biometric template created", responseData);
            
        } catch (Exception e) {
            return new ApiResponse<>(false, "Biometric processing failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/verify-face")
    public ApiResponse<Map<String, Object>> verifyFacialImage(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("templateId") String templateId,
            @RequestParam("governmentId") String governmentId) {
        try {
            boolean verified = biometricService.verifyAndDeleteBiometric(templateId, imageFile, governmentId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("verified", verified);
            responseData.put("templateId", templateId);
            responseData.put("biometricDeleted", true);
            responseData.put("message", verified ? 
                "Biometric verification successful" : "Biometric verification failed");
            
            return new ApiResponse<>(true, 
                verified ? "Biometric verified successfully" : "Biometric verification failed", 
                responseData);
            
        } catch (Exception e) {
            return new ApiResponse<>(false, "Biometric verification failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/template-stats")
    public ApiResponse<Map<String, Object>> getTemplateStatistics() {
        try {
            Map<String, Object> stats = biometricService.getTemplateStats();
            return new ApiResponse<>(true, "Template statistics retrieved", stats);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Failed to get template statistics: " + e.getMessage());
        }
    }
    
    @PostMapping("/cleanup-templates")
    public ApiResponse<String> cleanupAllTemplates() {
        try {
            biometricService.cleanupAllTemplates();
            return new ApiResponse<>(true, "All biometric templates cleared successfully");
        } catch (Exception e) {
            return new ApiResponse<>(false, "Template cleanup failed: " + e.getMessage());
        }
    }
}