package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.service.EnhancedThresholdPaillierService;
import com.evoting.evoting_backend.service.TallyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/threshold")
@PreAuthorize("hasRole('ADMIN') or hasRole('ELECTION_OFFICER')")
public class ThresholdDecryptionController {
    
    @Autowired
    private EnhancedThresholdPaillierService thresholdPaillierService;
    
    @Autowired
    private TallyService tallyService;
    
    @PostMapping("/election/{electionId}/tally")
    public Map<String, Object> performThresholdTally(
            @PathVariable Long electionId,
            @RequestBody Map<String, String> trusteeShares) {
        
        try {
            // Convert string shares to BigInteger
            Map<String, BigInteger> shares = new HashMap<>();
            for (Map.Entry<String, String> entry : trusteeShares.entrySet()) {
                shares.put(entry.getKey(), new BigInteger(entry.getValue()));
            }
            
            return tallyService.tallyVotesWithThreshold(electionId, shares);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Threshold tally failed: " + e.getMessage());
            error.put("success", false);
            return error;
        }
    }
    
    @GetMapping("/election/{electionId}/readiness")
    public EnhancedThresholdPaillierService.DecryptionStatus getTallyReadiness(@PathVariable Long electionId) {
        return tallyService.getTallyReadiness(electionId);
    }
    
    @PostMapping("/verify-share")
    public Map<String, Object> verifyTrusteeShare(
            @RequestBody ShareVerificationRequest request) {
        
        boolean isValid = thresholdPaillierService.verifyTrusteeShare(
            request.getTrusteeId(), 
            new BigInteger(request.getShare())
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("trusteeId", request.getTrusteeId());
        response.put("valid", isValid);
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return response;
    }
    
    @GetMapping("/status")
    public EnhancedThresholdPaillierService.DecryptionStatus getDecryptionStatus() {
        return thresholdPaillierService.getDecryptionStatus();
    }
    
    public static class ShareVerificationRequest {
        private String trusteeId;
        private String share;
        
        public String getTrusteeId() { return trusteeId; }
        public void setTrusteeId(String trusteeId) { this.trusteeId = trusteeId; }
        public String getShare() { return share; }
        public void setShare(String share) { this.share = share; }
    }
}