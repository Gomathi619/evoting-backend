package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.BulletinBoardEntry;
import com.evoting.evoting_backend.model.Vote;
import com.evoting.evoting_backend.repository.BulletinBoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BulletinBoardService {
    
    @Autowired
    private BulletinBoardRepository bulletinBoardRepository;
    
    /**
     * ✅ Add vote to bulletin board
     */
    public boolean addVoteToBulletinBoard(Vote vote) {
    System.out.println("🔍 === BULLETIN BOARD DEBUG START ===");
    System.out.println("🔍 Processing vote: " + vote.getTrackingCode());
    
    try {
        // 1. Check repository
        System.out.println("🔍 BulletinBoardRepository: " + (bulletinBoardRepository != null ? "NOT NULL" : "NULL"));
        
        // 2. Check if vote data is valid
        System.out.println("🔍 Vote trackingCode: " + vote.getTrackingCode());
        System.out.println("🔍 Vote electionId: " + vote.getElectionId());
        System.out.println("🔍 Vote encryptedVote length: " + (vote.getEncryptedVote() != null ? vote.getEncryptedVote().length() : "NULL"));
        
        // 3. Check if entry already exists
        Optional<BulletinBoardEntry> existing = bulletinBoardRepository.findByTrackingCode(vote.getTrackingCode());
        System.out.println("🔍 Already exists in bulletin board: " + existing.isPresent());
        
        if (existing.isPresent()) {
            System.out.println("🔍 ✅ Already exists - returning true");
            return true;
        }

        // 4. Create new entry
        BulletinBoardEntry entry = new BulletinBoardEntry();
        entry.setTrackingCode(vote.getTrackingCode());
        entry.setEncryptedVote(vote.getEncryptedVote());
        entry.setElectionId(vote.getElectionId());
        entry.setTimestamp(LocalDateTime.now());
        
        // 5. Generate hashes
        String entryHash = generateSimpleHash(vote);
        entry.setEntryHash(entryHash);
        System.out.println("🔍 Generated entryHash: " + entryHash);
        
        String previousHash = getLatestEntryHash(vote.getElectionId());
        entry.setPreviousHash(previousHash);
        System.out.println("🔍 Previous hash: " + previousHash);

        // 6. Try to save
        System.out.println("🔍 Attempting to save to bulletin board...");
        BulletinBoardEntry savedEntry = bulletinBoardRepository.save(entry);
        
        // 7. Check result
        boolean success = savedEntry != null && savedEntry.getId() != null;
        System.out.println("🔍 Save successful: " + success);
        System.out.println("🔍 Saved entry ID: " + (savedEntry != null ? savedEntry.getId() : "NULL"));
        
        System.out.println("🔍 === BULLETIN BOARD DEBUG END ===");
        return success;

    } catch (Exception e) {
        System.out.println("💥 BULLETIN BOARD CRITICAL ERROR: " + e.getMessage());
        e.printStackTrace(); // This will show the exact error
        System.out.println("🔍 === BULLETIN BOARD DEBUG END ===");
        return false;
    }
}
    
    /**
     * ✅ Simple hash generation
     */
    private String generateSimpleHash(Vote vote) {
        String data = vote.getTrackingCode() + vote.getEncryptedVote() + vote.getElectionId();
        return String.valueOf(data.hashCode());
    }
    
    /**
     * ✅ Get latest entry hash
     */
    private String getLatestEntryHash(Long electionId) {
        try {
            Optional<BulletinBoardEntry> latest = bulletinBoardRepository.findTopByElectionIdOrderByTimestampDesc(electionId);
            return latest.map(BulletinBoardEntry::getEntryHash).orElse("0");
        } catch (Exception e) {
            return "0";
        }
    }
    
    /**
     * ✅ Get entry by tracking code
     */
    public Optional<BulletinBoardEntry> getEntryByTrackingCode(String trackingCode) {
        return bulletinBoardRepository.findByTrackingCode(trackingCode);
    }
    
    /**
     * ✅ Get election entries
     */
    public List<BulletinBoardEntry> getElectionEntries(Long electionId) {
        return bulletinBoardRepository.findByElectionIdOrderByTimestampAsc(electionId);
    }
    
    /**
     * ✅ Verify board integrity
     */
    public boolean verifyBoardIntegrity() {
        try {
            List<BulletinBoardEntry> entries = bulletinBoardRepository.findAllByOrderByTimestampAsc();
            return !entries.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}