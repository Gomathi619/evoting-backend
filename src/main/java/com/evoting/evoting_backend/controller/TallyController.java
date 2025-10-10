package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.service.TallyService;
import com.evoting.evoting_backend.service.ElectionService;
import com.evoting.evoting_backend.service.VoteService;
import com.evoting.evoting_backend.model.Election;
import com.evoting.evoting_backend.model.Vote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tally")
public class TallyController {

    @Autowired
    private TallyService tallyService;

    @Autowired
    private ElectionService electionService;

    @Autowired
    private VoteService voteService;

    /**
     * Get tally results for an election - Uses simple tally for immediate results
     */
    @GetMapping("/{electionId}")
    public ResponseEntity<Map<String, Integer>> getTallyResults(@PathVariable Long electionId) {
        try {
            // Use simple tally to get immediate correct results
            Map<String, Integer> results = tallyService.simpleTallyVotes(electionId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            // Return empty map instead of error to maintain compatibility
            System.err.println("Tally error: " + e.getMessage());
            return ResponseEntity.ok(new HashMap<>());
        }
    }

    /**
     * NEW: Get detailed results with winner information
     */
    @GetMapping("/{electionId}/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedResults(@PathVariable Long electionId) {
        try {
            Map<String, Object> results = tallyService.tallyVotesWithWinner(electionId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            System.err.println("Detailed tally error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Manually trigger tally computation
     */
    @PostMapping("/{electionId}/compute")
    public ResponseEntity<Map<String, Object>> computeTally(@PathVariable Long electionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Integer> results = tallyService.simpleTallyVotes(electionId);
            response.put("success", true);
            response.put("message", "Tally computed successfully");
            response.put("results", results);
            response.put("candidatesCount", results.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Tally computation failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Debug endpoint to check election and vote status
     */
    @GetMapping("/{electionId}/debug")
    public ResponseEntity<Map<String, Object>> debugTally(@PathVariable Long electionId) {
        Map<String, Object> debugInfo = new HashMap<>();
        try {
            Election election = electionService.getElectionById(electionId);
            List<Vote> votes = voteService.getVotesByElection(electionId);
            
            debugInfo.put("electionId", electionId);
            debugInfo.put("electionTitle", election.getTitle());
            debugInfo.put("electionState", election.getState().toString());
            debugInfo.put("totalVotes", votes.size());
            debugInfo.put("votesAvailable", !votes.isEmpty());
            
            // Try to compute simple tally
            try {
                Map<String, Integer> results = tallyService.simpleTallyVotes(electionId);
                debugInfo.put("tallySuccess", true);
                debugInfo.put("results", results);
                debugInfo.put("candidatesCount", results.size());
            } catch (Exception tallyException) {
                debugInfo.put("tallySuccess", false);
                debugInfo.put("tallyError", tallyException.getMessage());
            }
            
            debugInfo.put("status", "DEBUG_COMPLETED");
            
        } catch (Exception e) {
            debugInfo.put("status", "ERROR");
            debugInfo.put("error", e.getMessage());
        }
        return ResponseEntity.ok(debugInfo);
    }
}