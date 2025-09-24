package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.Election;
import com.evoting.evoting_backend.model.Voter;
import com.evoting.evoting_backend.repository.ElectionRepository;
import com.evoting.evoting_backend.repository.VoterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class VoterService {
    @Autowired private VoterRepository voterRepository;
    @Autowired private ElectionRepository electionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public Voter createVoter(Voter voter) {
        voter.setPassword(passwordEncoder.encode(voter.getPassword()));
        if (voter.getElection() != null && voter.getElection().getId() != null) {
            Election election = electionRepository.findById(voter.getElection().getId())
                    .orElseThrow(() -> new RuntimeException("Election not found with id: " + voter.getElection().getId()));
            voter.setElection(election);
        }
        return voterRepository.save(voter);
    }

    public List<Voter> getAllVoters() {
        return voterRepository.findAll();
    }
    
    public Optional<Voter> getVoterById(Long id) {
        return voterRepository.findById(id);
    }
    
    public Optional<Voter> getVoterByEmail(String email) {
        return voterRepository.findByEmail(email);
    }
}