package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.KYCVerification;
import com.evoting.evoting_backend.model.IdentityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface KYCVerificationRepository extends JpaRepository<KYCVerification, Long> {
    
    Optional<KYCVerification> findByGovernmentIdHash(String governmentIdHash);
    
    @Query("SELECT COUNT(k) > 0 FROM KYCVerification k WHERE k.governmentIdHash = :governmentIdHash AND k.status = 'VERIFIED'")
    boolean existsVerifiedByIdHash(@Param("governmentIdHash") String governmentIdHash);
    
    @Query("SELECT k FROM KYCVerification k WHERE k.governmentIdHash = :governmentIdHash AND k.status = 'VERIFIED' AND k.isAlive = true")
    Optional<KYCVerification> findActiveVerifiedByIdHash(@Param("governmentIdHash") String governmentIdHash);
    
    @Query("SELECT COUNT(k) FROM KYCVerification k WHERE k.status = 'VERIFIED'")
    long countVerifiedIdentities();
    
    @Query("SELECT COUNT(k) FROM KYCVerification k WHERE k.status = 'VERIFIED' AND k.isAlive = false")
    long countDeceasedVerifications();
}