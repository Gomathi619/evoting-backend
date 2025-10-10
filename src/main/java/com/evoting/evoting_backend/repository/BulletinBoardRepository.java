package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.BulletinBoardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface BulletinBoardRepository extends JpaRepository<BulletinBoardEntry, Long> {
    
    Optional<BulletinBoardEntry> findByTrackingCode(String trackingCode);
    
    List<BulletinBoardEntry> findByElectionIdOrderByTimestampAsc(Long electionId);
    
    List<BulletinBoardEntry> findAllByOrderByTimestampAsc();
    
    Optional<BulletinBoardEntry> findTopByElectionIdOrderByTimestampDesc(Long electionId);
    
    Optional<BulletinBoardEntry> findTopByOrderByTimestampDesc();
    
    @Query("SELECT COUNT(b) FROM BulletinBoardEntry b WHERE b.electionId = ?1")
    long countByElectionId(Long electionId);
}