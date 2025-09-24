package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.exception.ElectionException;
import com.evoting.evoting_backend.exception.ResourceNotFoundException;
import com.evoting.evoting_backend.model.Election;
import com.evoting.evoting_backend.model.ElectionState;
import com.evoting.evoting_backend.repository.ElectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ElectionService {

    @Autowired
    private ElectionRepository electionRepository;
    
    public List<Election> getAllElections() {
        return electionRepository.findAll();
    }
    
    public Election createElection(Election election) {
        election.setState(ElectionState.CREATED);
        return electionRepository.save(election);
    }

    public Election getElectionById(Long id) {
        return electionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Election not found with id: " + id));
    }

    public Election openElection(Long id) {
        Election election = getElectionById(id);
        if (election.getState() != ElectionState.CREATED) {
            throw new ElectionException("Election must be in CREATED state to be opened.");
        }
        election.setState(ElectionState.OPEN);
        return electionRepository.save(election);
    }

    public Election closeElection(Long id) {
        Election election = getElectionById(id);
        if (election.getState() != ElectionState.OPEN) {
            throw new ElectionException("Election must be in OPEN state to be closed.");
        }
        election.setState(ElectionState.CLOSED);
        return electionRepository.save(election);
    }
}