package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.VoterIdentity;
import com.evoting.evoting_backend.model.IdentityStatus;
import com.evoting.evoting_backend.repository.VoterIdentityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class IdentityVerificationService {
    
    @Autowired
    private VoterIdentityRepository voterIdentityRepository;
    
    private final String HMAC_SECRET = "EVoting-Secure-HMAC-Key-2024";
    
    public VoterIdentity verifyIdentity(String governmentId, String name, String email, String phone) {
        try {
            // Hash government ID for privacy
            String governmentIdHash = hashGovernmentId(governmentId);
            
            // Check if already verified
            if (voterIdentityRepository.existsVerifiedByIdHash(governmentIdHash)) {
                throw new RuntimeException("Voter already verified with this ID");
            }
            
            // Simulate government API checks
            boolean isAlive = checkDeathRegistry(governmentId);
            boolean isDuplicate = checkDuplicateRegistration(governmentIdHash);
            
            if (!isAlive) {
                throw new RuntimeException("Voter registration rejected: Identity verification failed");
            }
            
            if (isDuplicate) {
                throw new RuntimeException("Duplicate registration detected");
            }
            
            // Create voter identity record
            VoterIdentity identity = new VoterIdentity();
            identity.setGovernmentIdHash(governmentIdHash);
            identity.setName(name);
            identity.setEmail(email);
            identity.setPhone(phone);
            identity.setStatus(IdentityStatus.VERIFIED);
            identity.setAlive(true);
            identity.setDuplicate(false);
            identity.setVerifiedAt(LocalDateTime.now());
            
            return voterIdentityRepository.save(identity);
            
        } catch (Exception e) {
            throw new RuntimeException("Identity verification failed: " + e.getMessage());
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
            throw new RuntimeException("HMAC calculation failed");
        }
    }
    
    // Mock government API integrations
    private boolean checkDeathRegistry(String governmentId) {
        // Simulate death registry check - always return true in development
        System.out.println("Checking death registry for ID: " + governmentId.substring(0, 4) + "***");
        return true;
    }
    
    private boolean checkDuplicateRegistration(String governmentIdHash) {
        // Check if this government ID is already registered and verified
        return voterIdentityRepository.existsVerifiedByIdHash(governmentIdHash);
    }
    
    public VoterIdentity getVerifiedIdentity(String governmentId) {
        String governmentIdHash = hashGovernmentId(governmentId);
        return voterIdentityRepository.findActiveVerifiedByIdHash(governmentIdHash)
                .orElseThrow(() -> new RuntimeException("No verified identity found"));
    }
    
    public long getVerifiedVoterCount() {
        return voterIdentityRepository.countVerifiedIdentities();
    }
}