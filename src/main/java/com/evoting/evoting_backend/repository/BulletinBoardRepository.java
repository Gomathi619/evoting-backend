package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.BulletinBoardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BulletinBoardRepository extends JpaRepository<BulletinBoardEntry, Long> {
    Optional<BulletinBoardEntry> findByTrackingCode(String trackingCode);
    List<BulletinBoardEntry> findByElectionId(Long electionId);
}