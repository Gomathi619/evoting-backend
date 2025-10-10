package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.VoterIdentity;
import com.evoting.evoting_backend.model.IdentityStatus;
import com.evoting.evoting_backend.repository.VoterIdentityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Timer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class KYCRegistrationService {

    @Autowired
    private VoterIdentityRepository voterIdentityRepository;

    @Autowired
    private DeathRegistryService deathRegistryService;

    @Autowired
    private ImmutableAuditService auditService;

    @Autowired
    private MonitoringService monitoringService;

    private final String HMAC_SECRET = "Secure-KYC-HMAC-Key-2024";

    public VoterIdentity verifyIdentityAndCreateSession(String governmentId, String name,
                                                        String email, String phone, Long electionId) {
        Timer.Sample timer = monitoringService.startTimer();

        try {
            // Step 1: Death registry check
            boolean isAlive = performDeathRegistryCheck(governmentId, name);
            if (!isAlive) {
                auditService.logEvent("KYC_REJECTED_DECEASED", "KYCRegistrationService",
                        "verifyIdentity", "government_id:" + deathRegistryService.maskGovernmentId(governmentId) +
                                ", reason:deceased_person");
                throw new RuntimeException("Identity verification failed: Person is deceased");
            }

            // Step 2: Hash government ID for privacy
            String governmentIdHash = hashGovernmentId(governmentId);

            // Step 3: Check for duplicates
            if (voterIdentityRepository.existsVerifiedByIdHash(governmentIdHash)) {
                auditService.logEvent("KYC_REJECTED_DUPLICATE", "KYCRegistrationService",
                        "verifyIdentity", "government_id:" + deathRegistryService.maskGovernmentId(governmentId) +
                                ", reason:duplicate_registration");
                throw new RuntimeException("Voter already verified with this ID");
            }

            // Step 4: Simulate duplicate detection from government API
            if (checkDuplicateRegistration(governmentIdHash)) {
                auditService.logEvent("KYC_REJECTED_DUPLICATE", "KYCRegistrationService",
                        "verifyIdentity", "government_id:" + deathRegistryService.maskGovernmentId(governmentId) +
                                ", reason:duplicate_detected");
                throw new RuntimeException("Duplicate registration detected");
            }

            // Step 5: Create and save voter identity
            VoterIdentity identity = new VoterIdentity();
            identity.setGovernmentIdHash(governmentIdHash);
            identity.setName(name);
            identity.setEmail(email);
            identity.setPhone(phone);
            identity.setStatus(IdentityStatus.VERIFIED);
            identity.setAlive(true);
            identity.setDuplicate(false);
            identity.setVerifiedAt(LocalDateTime.now());

            VoterIdentity savedIdentity = voterIdentityRepository.save(identity);

            // Step 6: Log success in immutable audit log
            auditService.logKYCVerification(savedIdentity.getId(), governmentIdHash, true);

            // Step 7: Record success metric
            long durationMs = TimeUnit.NANOSECONDS.toMillis(timer.stop(monitoringService.getKycTimer()));
            monitoringService.recordKYCVerification(true, durationMs);

            return savedIdentity;

        } catch (Exception e) {
            // Record failure metric
            long durationMs = TimeUnit.NANOSECONDS.toMillis(timer.stop(monitoringService.getKycTimer()));
            monitoringService.recordKYCVerification(false, durationMs);

            // Log failure
            auditService.logEvent("KYC_VERIFICATION_FAILED", "KYCRegistrationService",
                    "verifyIdentity", "government_id:" + deathRegistryService.maskGovernmentId(governmentId) +
                            ", error:" + e.getMessage());

            throw new RuntimeException("KYC verification failed: " + e.getMessage(), e);
        }
    }

    private boolean performDeathRegistryCheck(String governmentId, String fullName) {
        try {
            LocalDate dateOfBirth = extractDateOfBirthFromGovernmentId(governmentId);
            return deathRegistryService.isPersonAlive(governmentId, fullName, dateOfBirth);
        } catch (Exception e) {
            System.err.println("Death registry check failed: " + e.getMessage());
            auditService.logEvent("DEATH_REGISTRY_CHECK_FAILED", "KYCRegistrationService",
                    "performDeathRegistryCheck", "government_id:" + deathRegistryService.maskGovernmentId(governmentId) +
                            ", error:" + e.getMessage());
            return true; // fail-open for availability
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

    private boolean checkDuplicateRegistration(String governmentIdHash) {
        return voterIdentityRepository.existsVerifiedByIdHash(governmentIdHash);
    }
}