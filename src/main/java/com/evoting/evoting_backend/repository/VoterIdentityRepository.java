package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.VoterIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface VoterIdentityRepository extends JpaRepository<VoterIdentity, Long> {
    Optional<VoterIdentity> findByGovernmentIdHash(String governmentIdHash);
    
    @Query("SELECT COUNT(v) > 0 FROM VoterIdentity v WHERE v.governmentIdHash = ?1 AND v.status = 'VERIFIED'")
    boolean existsVerifiedByIdHash(String governmentIdHash);
    
    @Query("SELECT v FROM VoterIdentity v WHERE v.governmentIdHash = ?1 AND v.status = 'VERIFIED' AND v.isAlive = true")
    Optional<VoterIdentity> findActiveVerifiedByIdHash(String governmentIdHash);
    
    @Query("SELECT COUNT(v) FROM VoterIdentity v WHERE v.status = 'VERIFIED'")
    long countVerifiedIdentities();
}