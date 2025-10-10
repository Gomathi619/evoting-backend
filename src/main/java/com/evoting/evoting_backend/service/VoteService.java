package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.exception.ElectionException;
import com.evoting.evoting_backend.model.Election;
import com.evoting.evoting_backend.model.ElectionState;
import com.evoting.evoting_backend.model.Vote;
import com.evoting.evoting_backend.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
public class VoteService {
    @Autowired private VoteRepository voteRepository;
    @Autowired private ElectionService electionService;

    public Vote saveVote(Vote vote) {
        // Ensure the election is open before allowing a vote
        Election election = electionService.getElectionById(vote.getElectionId());
        if (election.getState() != ElectionState.OPEN) {
            throw new ElectionException("Cannot cast vote, election is not open.");
        }

        // Generate UUID with debugging
        String trackingCode = UUID.randomUUID().toString();
        
        // ✅ ADDED: Debug logging
        System.out.println("=== VOTE SERVICE DEBUG ===");
        System.out.println("Generated UUID: " + trackingCode);
        System.out.println("UUID Length: " + trackingCode.length());
        System.out.println("UUID Pattern: " + trackingCode.matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"));
        System.out.println("=== END DEBUG ===");
        
        vote.setTrackingCode(trackingCode);
        vote.setTimestamp(LocalDateTime.now());
        
        Vote savedVote = voteRepository.save(vote);
        
        // ✅ ADDED: Verify saved UUID
        System.out.println("Saved vote tracking code: " + savedVote.getTrackingCode());
        
        return savedVote;
    }

    public Optional<Vote> getVoteByTrackingCode(String trackingCode) {
        return voteRepository.findByTrackingCode(trackingCode);
    }

    public List<Vote> getVotesByElection(Long electionId) {
        return voteRepository.findByElectionId(electionId);
    }
}