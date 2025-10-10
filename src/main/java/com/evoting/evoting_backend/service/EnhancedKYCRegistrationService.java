package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.KYCVerification;
import com.evoting.evoting_backend.model.IdentityStatus;
import com.evoting.evoting_backend.repository.KYCVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Timer;

@Service
public class EnhancedKYCRegistrationService {
    
    @Autowired
    private KYCVerificationRepository kycRepository;
    
    @Autowired
    private EnhancedDeathRegistryService deathRegistryService;
    
    @Autowired
    private ImmutableAuditService auditService;
    
    @Autowired
    private MonitoringService monitoringService;
    
    private final String HMAC_SECRET = "Secure-KYC-HMAC-Key-2024";
    
    public KYCVerification verifyIdentityAndCreateSession(String governmentId, String fullName, 
                                                         String email, String phone, Long electionId) {
        Timer.Sample timer = monitoringService.startTimer();
        
        try {
            // Step 1: Death registry check
            boolean isAlive = deathRegistryService.isPersonAlive(governmentId, fullName, 
                extractDateOfBirthFromGovernmentId(governmentId));
            
            if (!isAlive) {
                auditService.logEvent("KYC_REJECTED_DECEASED", "EnhancedKYCRegistrationService",
                    "verifyIdentity", "government_id:" + deathRegistryService.maskGovernmentId(governmentId));
                throw new RuntimeException("Identity verification failed: Person is deceased");
            }
            
            // Step 2: Hash government ID for privacy
            String governmentIdHash = hashGovernmentId(governmentId);
            
            // Step 3: Check for duplicates
            if (kycRepository.existsVerifiedByIdHash(governmentIdHash)) {
                auditService.logEvent("KYC_REJECTED_DUPLICATE", "EnhancedKYCRegistrationService",
                    "verifyIdentity", "government_id:" + deathRegistryService.maskGovernmentId(governmentId));
                throw new RuntimeException("Voter already verified with this ID");
            }
            
            // Step 4: Create KYC record (STORED SEPARATELY FROM TOKENS)
            KYCVerification kyc = new KYCVerification();
            kyc.setGovernmentIdHash(governmentIdHash);
            kyc.setFullName(fullName);
            kyc.setEmail(email);
            kyc.setPhone(phone);
            kyc.setStatus(IdentityStatus.VERIFIED);
            kyc.setAlive(true);
            kyc.setDuplicate(false);
            kyc.setDeathRegistryChecked(true);
            kyc.setVerifiedAt(LocalDateTime.now());
            
            KYCVerification savedKYC = kycRepository.save(kyc);
            
            // ✅ CRITICAL: KYC database ONLY stores identity verification
            // Token issuance happens separately with NO LINKAGE
            
            // Record metrics
            long durationMs = TimeUnit.NANOSECONDS.toMillis(timer.stop(monitoringService.getKycTimer()));
            monitoringService.recordKYCVerification(true, durationMs);
            
            auditService.logKYCVerification(savedKYC.getId(), governmentIdHash, true);
            
            return savedKYC;
            
        } catch (Exception e) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(timer.stop(monitoringService.getKycTimer()));
            monitoringService.recordKYCVerification(false, durationMs);
            
            auditService.logEvent("KYC_VERIFICATION_FAILED", "EnhancedKYCRegistrationService",
                "verifyIdentity", "government_id:" + deathRegistryService.maskGovernmentId(governmentId) + 
                ", error:" + e.getMessage());
                
            throw new RuntimeException("KYC verification failed: " + e.getMessage(), e);
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
            throw new RuntimeException("HMAC calculation failed for KYC");
        }
    }
    
    private LocalDate extractDateOfBirthFromGovernmentId(String governmentId) {
        try {
            if (governmentId.length() >= 6) {
                String dobPart = governmentId.substring(0, 6);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
                return LocalDate.parse(dobPart, formatter);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse DOB from government ID: " + governmentId);
        }
        return LocalDate.of(1980, 1, 1); // default fallback
    }
    
    public KYCVerification getVerifiedIdentity(String governmentId) {
        String governmentIdHash = hashGovernmentId(governmentId);
        return kycRepository.findActiveVerifiedByIdHash(governmentIdHash)
                .orElseThrow(() -> new RuntimeException("No verified identity found"));
    }
    
    public long getVerifiedVoterCount() {
        return kycRepository.countVerifiedIdentities();
    }
    
    public Map<String, Object> getKYCStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalVerified", kycRepository.countVerifiedIdentities());
        stats.put("deceasedCount", kycRepository.countDeceasedVerifications());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }
}