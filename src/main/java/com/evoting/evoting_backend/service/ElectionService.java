package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.exception.ElectionException;
import com.evoting.evoting_backend.exception.ResourceNotFoundException;
import com.evoting.evoting_backend.model.Election;
import com.evoting.evoting_backend.model.ElectionState;
import com.evoting.evoting_backend.repository.ElectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ElectionService {

    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private ImmutableAuditService auditService;

    public List<Election> getAllElections() {
        return electionRepository.findAll();
    }
    
    public Election createElection(Election election) {
        // Set initial state
        election.setState(ElectionState.CREATED);
        election.setCreatedAt(LocalDateTime.now());
        
        Election savedElection = electionRepository.save(election);
        
        // Audit the creation
        auditService.logEvent("ELECTION_CREATED", "ElectionService", 
            "createElection", "election:" + savedElection.getId() + 
            ", title:" + savedElection.getTitle());
            
        return savedElection;
    }

    public Election getElectionById(Long id) {
        return electionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Election not found with id: " + id));
    }

    public Election openElection(Long id) {
        Election election = getElectionById(id);
        
        if (election.getState() != ElectionState.CREATED) {
            throw new ElectionException("Election must be in CREATED state to be opened. Current state: " + election.getState());
        }
        
        election.setState(ElectionState.OPEN);
        election.setOpenedAt(LocalDateTime.now());
        Election updatedElection = electionRepository.save(election);
        
        // Audit the opening
        auditService.logEvent("ELECTION_OPENED", "ElectionService",
            "openElection", "election:" + updatedElection.getId() +
            ", title:" + updatedElection.getTitle());
            
        return updatedElection;
    }

    public Election closeElection(Long id) {
        Election election = getElectionById(id);
        
        if (election.getState() != ElectionState.OPEN) {
            throw new ElectionException("Election must be in OPEN state to be closed. Current state: " + election.getState());
        }
        
        election.setState(ElectionState.CLOSED);
        election.setClosedAt(LocalDateTime.now());
        Election updatedElection = electionRepository.save(election);
        
        // Audit the closing
        auditService.logEvent("ELECTION_CLOSED", "ElectionService",
            "closeElection", "election:" + updatedElection.getId() +
            ", title:" + updatedElection.getTitle());
            
        return updatedElection;
    }

    public void deleteElection(Long id) {
        Election election = getElectionById(id);
        
        // Only allow deletion of elections in CREATED state
        if (election.getState() != ElectionState.CREATED) {
            throw new ElectionException("Only elections in CREATED state can be deleted. Current state: " + election.getState());
        }
        
        electionRepository.delete(election);
        
        // Audit the deletion
        auditService.logEvent("ELECTION_DELETED", "ElectionService",
            "deleteElection", "election:" + election.getId() +
            ", title:" + election.getTitle());
    }

    public List<Election> getActiveElections() {
        return electionRepository.findByState(ElectionState.OPEN);
    }

    public List<Election> getClosedElections() {
        return electionRepository.findByState(ElectionState.CLOSED);
    }
}