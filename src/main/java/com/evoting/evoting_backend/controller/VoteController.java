package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.dto.ApiResponse;
import com.evoting.evoting_backend.model.Vote;
import com.evoting.evoting_backend.service.VoteService;
import com.evoting.evoting_backend.service.AnonymousTokenService;
import com.evoting.evoting_backend.service.BulletinBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/secure-votes")
public class VoteController {
    
    @Autowired
    private VoteService voteService;
    
    @Autowired
    private AnonymousTokenService anonymousTokenService;
    
    @Autowired
    private BulletinBoardService bulletinBoardService;
    
    /**
     * ✅ Cast Secure Vote
     */
    @PostMapping
    public ApiResponse castSecureVote(@RequestBody Map<String, Object> request) {
        try {
            String anonymousToken = (String) request.get("anonymousToken");
            Long electionId = Long.valueOf(request.get("electionId").toString());
            Long candidateId = Long.valueOf(request.get("candidateId").toString());
            String encryptedVote = (String) request.get("encryptedVote");
            
            // Validate and consume token
            boolean tokenValid = anonymousTokenService.validateAndConsumeToken(anonymousToken, electionId);
            
            if (!tokenValid) {
                return new ApiResponse(false, "Invalid or used token");
            }
            
            // Create vote
            Vote vote = new Vote();
            vote.setEncryptedVote(encryptedVote);
            vote.setElectionId(electionId);
            vote.setCandidateId(candidateId);
            
            Vote savedVote = voteService.saveVote(vote);
            
            // ✅ CRITICAL: Add to bulletin board
            boolean addedToBulletinBoard = bulletinBoardService.addVoteToBulletinBoard(savedVote);
            
            if (!addedToBulletinBoard) {
                System.out.println("WARNING: Vote saved but failed to add to bulletin board!");
            }
            
            // ✅ ADDED: Response debugging
            System.out.println("=== VOTE CONTROLLER DEBUG ===");
            System.out.println("Generated tracking code: " + savedVote.getTrackingCode());
            System.out.println("Added to bulletin board: " + addedToBulletinBoard);
            System.out.println("=== END DEBUG ===");
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("trackingCode", savedVote.getTrackingCode());
            responseData.put("electionId", electionId);
            responseData.put("candidateId", candidateId);
            responseData.put("timestamp", java.time.LocalDateTime.now());
            responseData.put("addedToBulletinBoard", addedToBulletinBoard);
            
            return new ApiResponse(true, "Secure vote cast successfully", responseData);
            
        } catch (Exception e) {
            return new ApiResponse(false, "Vote casting failed: " + e.getMessage());
        }
    }
    
    /**
     * ✅ Token Statistics
     */
    @GetMapping("/token-stats/{electionId}")
    public ApiResponse getTokenStatistics(@PathVariable Long electionId) {
        try {
            Map<String, Object> stats = anonymousTokenService.getTokenStatistics(electionId);
            return new ApiResponse(true, "Token statistics retrieved", stats);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to get token statistics: " + e.getMessage());
        }
    }
}