package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.model.Candidate;
import com.evoting.evoting_backend.repository.CandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {
    @Autowired private CandidateRepository candidateRepository;

    @GetMapping("/election/{electionId}")
    public List<Candidate> getCandidatesByElection(@PathVariable Long electionId) {
        return candidateRepository.findByElectionId(electionId);
    }

    @PostMapping
    public Candidate addCandidate(@RequestBody Candidate candidate) {
        return candidateRepository.save(candidate);
    }
}