package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.Election;
import com.evoting.evoting_backend.model.ElectionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ElectionRepository extends JpaRepository<Election, Long> {
    
    List<Election> findByState(ElectionState state);
    
    Optional<Election> findByTitle(String title);
    
    @Query("SELECT e FROM Election e WHERE e.state = 'OPEN' AND CURRENT_TIMESTAMP BETWEEN e.votingStart AND e.votingEnd")
    List<Election> findActiveElectionsWithTimeRange();
    
    @Query("SELECT COUNT(e) FROM Election e WHERE e.state = :state")
    long countByState(@Param("state") ElectionState state);
    
    @Query("SELECT e FROM Election e WHERE e.votingEnd < CURRENT_TIMESTAMP AND e.state = 'OPEN'")
    List<Election> findElectionsThatShouldBeClosed();
    
    boolean existsByTitleAndState(String title, ElectionState state);
}