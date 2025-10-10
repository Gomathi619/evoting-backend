package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.dto.ApiResponse;
import com.evoting.evoting_backend.model.Election;
import com.evoting.evoting_backend.service.ElectionService;
import com.evoting.evoting_backend.service.TallyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/elections")
public class ElectionController {

    @Autowired
    private ElectionService electionService;

    @Autowired
    private TallyService tallyService;

    /**
     * ✅ EXISTING ENDPOINT - Get all elections
     */
    @GetMapping
    public ApiResponse getAllElections() {
        try {
            List<Election> elections = electionService.getAllElections();
            return new ApiResponse(true, "Elections retrieved successfully", elections);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to retrieve elections: " + e.getMessage());
        }
    }

    /**
     * ✅ EXISTING ENDPOINT - Create election
     */
    @PostMapping
    public ApiResponse createElection(@RequestBody Election election) {
        try {
            Election createdElection = electionService.createElection(election);
            return new ApiResponse(true, "Election created successfully", createdElection);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to create election: " + e.getMessage());
        }
    }

    /**
     * ✅ EXISTING ENDPOINT - Open election
     */
    @PostMapping("/{id}/open")
    public ApiResponse openElection(@PathVariable Long id) {
        try {
            Election election = electionService.openElection(id);
            return new ApiResponse(true, "Election opened successfully", election);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to open election: " + e.getMessage());
        }
    }

    /**
     * ✅ UPDATED ENDPOINT - Close election with automatic detailed tally
     */
    @PostMapping("/{id}/close")
    public ApiResponse closeElection(@PathVariable Long id) {
        try {
            // ✅ EXISTING: Close election first
            Election election = electionService.closeElection(id);
            
            // ✅ NEW: Trigger automatic detailed tally in background
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Wait 2 seconds for election closure to complete
                    Map<String, Object> results = tallyService.tallyVotesWithWinner(id);
                    System.out.println("✅ Automatic detailed tally completed for election: " + id);
                    System.out.println("✅ Results computed for " + ((Map)results.get("candidateResults")).size() + " candidates");
                    System.out.println("✅ Winner: " + results.get("winner"));
                    System.out.println("✅ Final Results: " + results);
                } catch (Exception tallyException) {
                    System.err.println("❌ Automatic tally failed for election " + id + ": " + tallyException.getMessage());
                    // Don't throw error - election closure should still succeed
                }
            }).start();
            
            return new ApiResponse(true, "Election closed successfully. Tally computation started.", election);
            
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to close election: " + e.getMessage());
        }
    }

    /**
     * ✅ EXISTING ENDPOINT - Get election by ID
     */
    @GetMapping("/{id}")
    public ApiResponse getElectionById(@PathVariable Long id) {
        try {
            Election election = electionService.getElectionById(id);
            return new ApiResponse(true, "Election retrieved successfully", election);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to retrieve election: " + e.getMessage());
        }
    }

    /**
     * ✅ EXISTING ENDPOINT - Delete election
     */
    @DeleteMapping("/{id}")
    public ApiResponse deleteElection(@PathVariable Long id) {
        try {
            electionService.deleteElection(id);
            return new ApiResponse(true, "Election deleted successfully");
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to delete election: " + e.getMessage());
        }
    }
}