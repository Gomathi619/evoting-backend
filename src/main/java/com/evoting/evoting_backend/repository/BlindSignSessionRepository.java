package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.BlindSignSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.time.LocalDateTime;

public interface BlindSignSessionRepository extends JpaRepository<BlindSignSession, String> {
    
    Optional<BlindSignSession> findBySessionId(String sessionId);
    
    @Query("SELECT s FROM BlindSignSession s WHERE s.voterIdentityId = :voterIdentityId AND s.electionId = :electionId AND s.expiresAt > :now AND s.used = false")
    Optional<BlindSignSession> findActiveSession(@Param("voterIdentityId") Long voterIdentityId, 
                                               @Param("electionId") Long electionId, 
                                               @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(s) > 0 FROM BlindSignSession s WHERE s.voterIdentityId = :voterIdentityId AND s.electionId = :electionId AND s.used = false AND s.expiresAt > :now")
    boolean hasActiveSession(@Param("voterIdentityId") Long voterIdentityId, 
                           @Param("electionId") Long electionId, 
                           @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM BlindSignSession s WHERE s.expiresAt < :now AND s.used = false")
    java.util.List<BlindSignSession> findExpiredSessions(@Param("now") LocalDateTime now);
}