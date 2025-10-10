package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.dto.ApiResponse;
import com.evoting.evoting_backend.model.Candidate;
import com.evoting.evoting_backend.repository.CandidateRepository;
import com.evoting.evoting_backend.service.CandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {
    
    @Autowired 
    private CandidateRepository candidateRepository;
    
    @Autowired
    private CandidateService candidateService;

    // ✅ PUBLIC: Get candidates for a specific election
    @GetMapping("/election/{electionId}")
    public ResponseEntity<ApiResponse<List<Candidate>>> getCandidatesByElection(@PathVariable Long electionId) {
        try {
            List<Candidate> candidates = candidateRepository.findByElectionId(electionId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Candidates retrieved successfully", candidates));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to retrieve candidates: " + e.getMessage()));
        }
    }

    // ✅ ELECTION OFFICER & ADMIN: Add candidate to election
    @PostMapping
    @PreAuthorize("hasRole('ELECTION_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Candidate>> addCandidate(@RequestBody Candidate candidate) {
        try {
            Candidate createdCandidate = candidateService.createCandidate(candidate);
            return ResponseEntity.ok(new ApiResponse<>(true, "Candidate added successfully", createdCandidate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to add candidate: " + e.getMessage()));
        }
    }

    // ✅ ELECTION OFFICER & ADMIN: Update candidate
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ELECTION_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Candidate>> updateCandidate(@PathVariable Long id, @RequestBody Candidate candidateDetails) {
        try {
            Candidate updatedCandidate = candidateService.updateCandidate(id, candidateDetails);
            return ResponseEntity.ok(new ApiResponse<>(true, "Candidate updated successfully", updatedCandidate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to update candidate: " + e.getMessage()));
        }
    }

    // ✅ ADMIN: Delete candidate (only if election not open)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteCandidate(@PathVariable Long id) {
        try {
            candidateService.deleteCandidate(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Candidate deleted successfully", "CANDIDATE_DELETED"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to delete candidate: " + e.getMessage()));
        }
    }

    // ✅ PUBLIC: Get all candidates
    @GetMapping
    public ResponseEntity<ApiResponse<List<Candidate>>> getAllCandidates() {
        try {
            List<Candidate> candidates = candidateRepository.findAll();
            return ResponseEntity.ok(new ApiResponse<>(true, "All candidates retrieved successfully", candidates));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to retrieve candidates: " + e.getMessage()));
        }
    }

    // ✅ PUBLIC: Get specific candidate
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Candidate>> getCandidate(@PathVariable Long id) {
        try {
            Candidate candidate = candidateService.getCandidateById(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Candidate retrieved successfully", candidate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to retrieve candidate: " + e.getMessage()));
        }
    }

    // ✅ ELECTION OFFICER & ADMIN: Get candidates for management
    @GetMapping("/election/{electionId}/manage")
    @PreAuthorize("hasRole('ELECTION_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Candidate>>> getCandidatesForManagement(@PathVariable Long electionId) {
        try {
            List<Candidate> candidates = candidateRepository.findByElectionId(electionId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Candidates for management retrieved successfully", candidates));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to retrieve candidates for management: " + e.getMessage()));
        }
    }
}