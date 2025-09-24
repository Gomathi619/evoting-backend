package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.Candidate;
import com.evoting.evoting_backend.repository.CandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CandidateService {
    @Autowired private CandidateRepository candidateRepository;
    
    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }
    
    public Candidate createCandidate(Candidate candidate) {
        return candidateRepository.save(candidate);
    }
    
    public Candidate getCandidateById(Long id) {
        return candidateRepository.findById(id).orElse(null);
    }

    public List<Candidate> getCandidatesByElection(Long electionId) {
        return candidateRepository.findByElectionId(electionId);
    }

    public Candidate updateCandidate(Long id, Candidate candidateDetails) {
        Candidate candidate = candidateRepository.findById(id).orElse(null);
        if (candidate != null) {
            candidate.setName(candidateDetails.getName());
            candidate.setParty(candidateDetails.getParty());
            candidate.setElection(candidateDetails.getElection());
            return candidateRepository.save(candidate);
        }
        return null;
    }

    public void deleteCandidate(Long id) {
        candidateRepository.deleteById(id);
    }
}