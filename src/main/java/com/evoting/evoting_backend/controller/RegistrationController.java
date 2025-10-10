package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.dto.ApiResponse;
import com.evoting.evoting_backend.model.AnonymousVotingToken;
import com.evoting.evoting_backend.service.AnonymousTokenService;
import com.evoting.evoting_backend.service.AtomicTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RegistrationController {
    
    @Autowired
    private AnonymousTokenService anonymousTokenService;
    
    @Autowired
    private AtomicTokenService atomicTokenService;
    
    /**
     * ✅ KYC Session Start
     */
    @PostMapping("/kyc/start-verification")
    public ApiResponse startKYCVerification(@RequestBody Map<String, Object> request) {
        try {
            Long electionId = Long.valueOf(request.get("electionId").toString());
            
            AnonymousTokenService.BlindSignSessionResponse session = 
                anonymousTokenService.startBlindSignSession(electionId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("sessionId", session.getSessionId());
            responseData.put("blindingFactor", session.getBlindingFactor().toString());
            responseData.put("electionId", electionId);
            
            return new ApiResponse(true, "KYC session started", responseData);
            
        } catch (Exception e) {
            return new ApiResponse(false, "KYC verification failed: " + e.getMessage());
        }
    }
    
    /**
     * ✅ Blind Signing
     */
    @PostMapping("/kyc/blind-sign")
    public ApiResponse performBlindSigning(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            BigInteger blindedMessage = new BigInteger(request.get("blindedMessage").toString());
            
            BigInteger blindedSignature = anonymousTokenService.signBlindedMessage(blindedMessage, sessionId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("blindedSignature", blindedSignature.toString());
            responseData.put("sessionId", sessionId);
            
            return new ApiResponse(true, "Blind signing completed", responseData);
            
        } catch (Exception e) {
            return new ApiResponse(false, "Blind signing failed: " + e.getMessage());
        }
    }
    
    /**
     * ✅ Token Issuance
     */
    @PostMapping("/kyc/issue-token")
    public ApiResponse issueAnonymousToken(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            Long electionId = Long.valueOf(request.get("electionId").toString());
            BigInteger unblindedSignature = new BigInteger(request.get("unblindedSignature").toString());
            String blindingFactor = (String) request.get("blindingFactor");
            
            AnonymousVotingToken token = anonymousTokenService.issueAnonymousToken(
                sessionId, electionId, unblindedSignature, blindingFactor);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("anonymousToken", token.getToken());
            responseData.put("electionId", token.getElectionId());
            responseData.put("expiresAt", token.getExpiresAt());
            
            return new ApiResponse(true, "Anonymous token issued successfully", responseData);
            
        } catch (Exception e) {
            return new ApiResponse(false, "Token issuance failed: " + e.getMessage());
        }
    }
    
    /**
     * ✅ Token Validation
     */
    @PostMapping("/secure-votes/validate-token")
    public ApiResponse validateAnonymousToken(@RequestBody Map<String, Object> request) {
        try {
            String anonymousToken = (String) request.get("anonymousToken");
            Long electionId = Long.valueOf(request.get("electionId").toString());
            
            boolean isValid = anonymousTokenService.validateTokenQuick(anonymousToken, electionId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("valid", isValid);
            responseData.put("electionId", electionId);
            responseData.put("token", anonymousTokenService.maskToken(anonymousToken));
            
            return new ApiResponse(true, "Token validation completed", responseData);
            
        } catch (Exception e) {
            return new ApiResponse(false, "Token validation failed: " + e.getMessage());
        }
    }
}