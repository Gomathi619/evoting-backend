package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.model.BulletinBoardEntry;
import com.evoting.evoting_backend.model.Vote;
import com.evoting.evoting_backend.repository.BulletinBoardRepository;
import com.evoting.evoting_backend.repository.VoteRepository;
import com.evoting.evoting_backend.service.BulletinBoardService;
import com.evoting.evoting_backend.service.CandidateService;
import com.evoting.evoting_backend.service.ElectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    @Autowired private VoteRepository voteRepository;
    @Autowired private BulletinBoardRepository bulletinBoardRepository;
    @Autowired private BulletinBoardService bulletinBoardService;
    @Autowired private ElectionService electionService;
    @Autowired private CandidateService candidateService;

    @GetMapping("/vote/{trackingCode}")
    public Map<String, Object> verifyVote(@PathVariable String trackingCode) {
        // ✅ ADDED: Debug input
        System.out.println("=== VERIFICATION CONTROLLER DEBUG ===");
        System.out.println("Verifying tracking code: " + trackingCode);
        System.out.println("Code length: " + trackingCode.length());
        
        Optional<Vote> voteOpt = voteRepository.findByTrackingCode(trackingCode);
        System.out.println("Vote found in database: " + voteOpt.isPresent());
        
        Optional<BulletinBoardEntry> bbEntryOpt = bulletinBoardRepository.findByTrackingCode(trackingCode);
        System.out.println("Entry found in bulletin board: " + bbEntryOpt.isPresent());
        System.out.println("=== END DEBUG ===");

        if (voteOpt.isEmpty()) {
            return Map.of("verified", false, "message", "Vote not found!");
        }

        Vote vote = voteOpt.get();
        if (bbEntryOpt.isEmpty()) {
            return Map.of("verified", false, "message", "Not in bulletin board!");
        }

        boolean voteMatches = vote.getEncryptedVote().equals(bbEntryOpt.get().getEncryptedVote());

        String electionTitle = "Unknown";
        String candidateName = "Unknown";
        
        try {
            electionTitle = electionService.getElectionById(vote.getElectionId()).getTitle();
            candidateName = candidateService.getCandidateById(vote.getCandidateId()).getName();
        } catch (Exception e) {
            System.out.println("Error getting election/candidate details: " + e.getMessage());
        }

        return Map.of(
                "verified", voteMatches,
                "voteExists", true,
                "inBulletinBoard", true,
                "voteMatches", voteMatches,
                "election", electionTitle,
                "candidate", candidateName,
                "trackingCode", trackingCode,
                "verificationTime", java.time.LocalDateTime.now().toString()
        );
    }

    @GetMapping("/bulletin-board/{electionId}")
    public Map<String, Object> getBulletinBoard(@PathVariable Long electionId) {
        List<BulletinBoardEntry> entries = bulletinBoardService.getElectionEntries(electionId);
        boolean integrityValid = bulletinBoardService.verifyBoardIntegrity();

        return Map.of(
                "electionId", electionId,
                "totalEntries", entries.size(),
                "integrityValid", integrityValid,
                "entries", entries.stream().map(this::entryToMap).toList(),
                "verifiedAt", java.time.LocalDateTime.now().toString()
        );
    }

    @GetMapping("/bulletin-board/integrity")
    public Map<String, Object> verifyBulletinBoardIntegrity() {
        boolean integrityValid = bulletinBoardService.verifyBoardIntegrity();
        
        // ✅ FIXED: Use simple stats instead of non-existent methods
        long totalEntries = bulletinBoardRepository.count();
        Optional<BulletinBoardEntry> latestEntry = bulletinBoardRepository.findTopByOrderByTimestampDesc();
        
        return Map.of(
                "integrityValid", integrityValid,
                "totalEntries", totalEntries,
                "latestEntryId", latestEntry.map(BulletinBoardEntry::getId).orElse(null),
                "checkTime", java.time.LocalDateTime.now().toString(),
                "message", integrityValid ? "Bulletin board integrity verified" : "Integrity compromised"
        );
    }

    @GetMapping("/tracking-codes/{electionId}")
    public Map<String, Object> getTrackingCodes(@PathVariable Long electionId) {
        List<BulletinBoardEntry> entries = bulletinBoardService.getElectionEntries(electionId);
        List<String> trackingCodes = entries.stream().map(BulletinBoardEntry::getTrackingCode).toList();

        return Map.of(
                "electionId", electionId,
                "totalVotes", trackingCodes.size(),
                "trackingCodes", trackingCodes,
                "generatedAt", java.time.LocalDateTime.now().toString()
        );
    }

    private Map<String, Object> entryToMap(BulletinBoardEntry entry) {
        return Map.of(
                "entryHash", entry.getEntryHash(),
                "previousHash", entry.getPreviousHash(),
                "trackingCode", entry.getTrackingCode(),
                "electionId", entry.getElectionId(),
                "timestamp", entry.getTimestamp().toString()
        );
    }
}