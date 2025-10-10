package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.exception.ElectionException;
import com.evoting.evoting_backend.exception.ResourceNotFoundException;
import com.evoting.evoting_backend.model.Candidate;
import com.evoting.evoting_backend.model.Election;
import com.evoting.evoting_backend.model.ElectionState;
import com.evoting.evoting_backend.repository.CandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandidateService {
    
    @Autowired 
    private CandidateRepository candidateRepository;
    
    @Autowired
    private ElectionService electionService;
    
    @Autowired
    private ImmutableAuditService auditService;

    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }
    
    public Candidate createCandidate(Candidate candidate) {
        // Verify the election exists and is in CREATED state
        Election election = electionService.getElectionById(candidate.getElectionId());
        
        if (election.isOpen() || election.isClosed()) {
            throw new ElectionException("Cannot add candidates to an election that is already OPEN or CLOSED");
        }
        
        Candidate savedCandidate = candidateRepository.save(candidate);
        
        // Audit candidate creation
        auditService.logEvent("CANDIDATE_ADDED", "CandidateService",
            "createCandidate", "candidate:" + savedCandidate.getId() +
            ", name:" + savedCandidate.getName() +
            ", election:" + savedCandidate.getElectionId());
            
        return savedCandidate;
    }
    
    public Candidate getCandidateById(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + id));
    }

    public List<Candidate> getCandidatesByElection(Long electionId) {
        return candidateRepository.findByElectionId(electionId);
    }

    public Candidate updateCandidate(Long id, Candidate candidateDetails) {
        Candidate candidate = getCandidateById(id);
        
        // Check if election is still editable
        Election election = electionService.getElectionById(candidate.getElectionId());
        if (election.isOpen() || election.isClosed()) {
            throw new ElectionException("Cannot modify candidates in an election that is OPEN or CLOSED");
        }
        
        candidate.setName(candidateDetails.getName());
        candidate.setParty(candidateDetails.getParty());
        candidate.setElectionId(candidateDetails.getElectionId());
        
        Candidate updatedCandidate = candidateRepository.save(candidate);
        
        // Audit candidate update
        auditService.logEvent("CANDIDATE_UPDATED", "CandidateService",
            "updateCandidate", "candidate:" + updatedCandidate.getId() +
            ", name:" + updatedCandidate.getName());
            
        return updatedCandidate;
    }

    public void deleteCandidate(Long id) {
        Candidate candidate = getCandidateById(id);
        
        // Check if election is still editable
        Election election = electionService.getElectionById(candidate.getElectionId());
        if (election.isOpen() || election.isClosed()) {
            throw new ElectionException("Cannot delete candidates from an election that is OPEN or CLOSED");
        }
        
        candidateRepository.deleteById(id);
        
        // Audit candidate deletion
        auditService.logEvent("CANDIDATE_DELETED", "CandidateService",
            "deleteCandidate", "candidate:" + candidate.getId() +
            ", name:" + candidate.getName());
    }
}