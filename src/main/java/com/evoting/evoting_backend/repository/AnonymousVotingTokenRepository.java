package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.AnonymousVotingToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface AnonymousVotingTokenRepository extends JpaRepository<AnonymousVotingToken, String> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM AnonymousVotingToken t WHERE t.token = :token AND t.electionId = :electionId")
    Optional<AnonymousVotingToken> findByTokenAndElectionIdWithLock(@Param("token") String token, 
                                                                   @Param("electionId") Long electionId);

    Optional<AnonymousVotingToken> findByTokenAndElectionId(String token, Long electionId);
    
    // ✅ ADDED: Check if session has active token (replaces voter identity check)
    @Query("SELECT COUNT(t) > 0 FROM AnonymousVotingToken t WHERE t.sessionId = :sessionId AND t.electionId = :electionId AND t.spent = false AND t.active = true")
    boolean hasActiveUnspentToken(@Param("sessionId") String sessionId, @Param("electionId") Long electionId);
    
    @Query("SELECT t FROM AnonymousVotingToken t WHERE t.electionId = :electionId AND t.spent = false AND t.active = true")
    List<AnonymousVotingToken> findUnspentTokensByElection(@Param("electionId") Long electionId);
    
    @Query("SELECT COUNT(t) FROM AnonymousVotingToken t WHERE t.electionId = :electionId AND t.spent = true")
    long countSpentTokensByElection(@Param("electionId") Long electionId);
    
    @Query("SELECT COUNT(t) FROM AnonymousVotingToken t WHERE t.electionId = :electionId")
    long countTokensByElection(@Param("electionId") Long electionId);

    @Query("SELECT t FROM AnonymousVotingToken t WHERE t.expiresAt < :now AND t.active = true")
    List<AnonymousVotingToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    // New query for session-based tokens
    @Query("SELECT t FROM AnonymousVotingToken t WHERE t.sessionId = :sessionId AND t.electionId = :electionId")
    Optional<AnonymousVotingToken> findBySessionAndElection(@Param("sessionId") String sessionId, 
                                                           @Param("electionId") Long electionId);
    
    // ✅ ADDED: For legacy service compatibility
    @Query("SELECT COUNT(t) > 0 FROM AnonymousVotingToken t WHERE t.sessionId = :sessionId AND t.electionId = :electionId")
    boolean existsBySessionAndElection(@Param("sessionId") String sessionId, @Param("electionId") Long electionId);
}