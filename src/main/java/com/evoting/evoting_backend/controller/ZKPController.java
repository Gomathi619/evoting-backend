package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.dto.ApiResponse;
import com.evoting.evoting_backend.service.ZeroKnowledgeProofService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/zkp")
public class ZKPController {
    
    @Autowired
    private ZeroKnowledgeProofService zkpService;
    
    @PostMapping("/verify-vote-validity")
    public ApiResponse<Map<String, Object>> verifyVoteValidity(@RequestBody ZKPVerificationRequest request) {
        try {
            BigInteger encryptedVote = new BigInteger(request.getEncryptedVote());
            Long[] validCandidateIds = request.getValidCandidateIds().toArray(new Long[0]);
            BigInteger publicKey = new BigInteger(request.getPublicKey());
            
            Map<String, Object> proof = zkpService.generateVoteValidityProof(
                encryptedVote, validCandidateIds, publicKey);
            
            boolean isValid = zkpService.verifyVoteValidityProof(
                encryptedVote, proof, validCandidateIds, publicKey);
            
            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            result.put("proofGenerated", true);
            result.put("proofType", proof.get("proofType"));
            result.put("timestamp", System.currentTimeMillis());
            
            return new ApiResponse<>(true, 
                isValid ? "Vote validity ZKP verified successfully" : "Vote validity ZKP verification failed", 
                result);
            
        } catch (Exception e) {
            return new ApiResponse<>(false, "ZKP verification failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/verify-token-ownership")
    public ApiResponse<Map<String, Object>> verifyTokenOwnership(@RequestBody TokenOwnershipRequest request) {
        try {
            BigInteger publicModulus = new BigInteger(request.getPublicModulus());
            
            Map<String, Object> proof = zkpService.generateTokenOwnershipProof(
                request.getAnonymousToken(), publicModulus);
            
            boolean isValid = zkpService.verifyTokenOwnershipProof(
                request.getAnonymousToken(), proof, publicModulus);
            
            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            result.put("proofGenerated", true);
            result.put("proofType", proof.get("proofType"));
            result.put("timestamp", System.currentTimeMillis());
            
            return new ApiResponse<>(true, 
                isValid ? "Token ownership ZKP verified successfully" : "Token ownership ZKP verification failed", 
                result);
            
        } catch (Exception e) {
            return new ApiResponse<>(false, "Token ownership ZKP failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> healthCheck() {
        try {
            boolean isHealthy = zkpService.healthCheck();
            
            Map<String, Object> result = new HashMap<>();
            result.put("service", "ZeroKnowledgeProofService");
            result.put("status", isHealthy ? "HEALTHY" : "DEGRADED");
            result.put("timestamp", System.currentTimeMillis());
            
            return new ApiResponse<>(true, "ZKP service health check completed", result);
            
        } catch (Exception e) {
            return new ApiResponse<>(false, "ZKP health check failed: " + e.getMessage());
        }
    }
    
    // Request DTOs
    public static class ZKPVerificationRequest {
        private String encryptedVote;
        private java.util.List<Long> validCandidateIds;
        private String publicKey;
        
        public String getEncryptedVote() { return encryptedVote; }
        public void setEncryptedVote(String encryptedVote) { this.encryptedVote = encryptedVote; }
        
        public java.util.List<Long> getValidCandidateIds() { return validCandidateIds; }
        public void setValidCandidateIds(java.util.List<Long> validCandidateIds) { this.validCandidateIds = validCandidateIds; }
        
        public String getPublicKey() { return publicKey; }
        public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    }
    
    public static class TokenOwnershipRequest {
        private String anonymousToken;
        private String publicModulus;
        
        public String getAnonymousToken() { return anonymousToken; }
        public void setAnonymousToken(String anonymousToken) { this.anonymousToken = anonymousToken; }
        
        public String getPublicModulus() { return publicModulus; }
        public void setPublicModulus(String publicModulus) { this.publicModulus = publicModulus; }
    }
}