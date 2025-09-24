package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.EligibilityToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface EligibilityTokenRepository extends JpaRepository<EligibilityToken, String> {
    Optional<EligibilityToken> findByVoterIdAndElectionId(Long voterId, Long electionId);
    List<EligibilityToken> findByVoterIdAndSpentFalse(Long voterId);
    List<EligibilityToken> findByElectionId(Long electionId);
    List<EligibilityToken> findBySpentTrue();
    List<EligibilityToken> findBySpentFalse();
    
    @Query("SELECT COUNT(e) FROM EligibilityToken e WHERE e.electionId = ?1")
    Long countByElectionId(Long electionId);
    
    @Query("SELECT COUNT(e) FROM EligibilityToken e WHERE e.electionId = ?1 AND e.spent = true")
    Long countSpentTokensByElectionId(Long electionId);
    
    @Query("SELECT COUNT(e) > 0 FROM EligibilityToken e WHERE e.voterId = ?1 AND e.electionId = ?2 AND e.spent = false")
    boolean existsValidTokenForVoter(Long voterId, Long electionId);
}