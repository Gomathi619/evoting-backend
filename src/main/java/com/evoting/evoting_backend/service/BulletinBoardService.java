package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.BulletinBoardEntry;
import com.evoting.evoting_backend.repository.BulletinBoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BulletinBoardService {
    @Autowired private BulletinBoardRepository repository;

    public List<BulletinBoardEntry> getElectionEntries(Long electionId) {
        return repository.findByElectionId(electionId);
    }

    public boolean verifyBoardIntegrity() {
        List<BulletinBoardEntry> entries = repository.findAll();
        String previousHash = "0";
        for (BulletinBoardEntry entry : entries) {
            if (!entry.getPreviousHash().equals(previousHash)) {
                return false;
            }
            previousHash = entry.getEntryHash();
        }
        return true;
    }

    public BulletinBoardStats getBoardStatistics() {
        List<BulletinBoardEntry> entries = repository.findAll();
        Long latestId = entries.isEmpty() ? 0L : entries.get(entries.size()-1).getId();
        return new BulletinBoardStats(entries.size(), latestId, LocalDateTime.now());
    }

    public void logVote(BulletinBoardEntry entry) {
        repository.save(entry);
    }

    public static class BulletinBoardStats {
        private int totalEntries;
        private Long latestEntryId;
        private LocalDateTime checkTime;

        public BulletinBoardStats(int totalEntries, Long latestEntryId, LocalDateTime checkTime) {
            this.totalEntries = totalEntries;
            this.latestEntryId = latestEntryId;
            this.checkTime = checkTime;
        }

        public int getTotalEntries() { return totalEntries; }
        public Long getLatestEntryId() { return latestEntryId; }
        public LocalDateTime getCheckTime() { return checkTime; }
    }
}