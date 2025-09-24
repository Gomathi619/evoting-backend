package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.crypto.PaillierUtil;
import com.evoting.evoting_backend.exception.ElectionException;
import com.evoting.evoting_backend.model.Candidate;
import com.evoting.evoting_backend.model.Election;
import com.evoting.evoting_backend.model.ElectionState;
import com.evoting.evoting_backend.model.Vote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TallyService {

    @Autowired private PaillierKeyService paillierKeyService;
    @Autowired private ElectionService electionService;
    @Autowired private CandidateService candidateService;
    @Autowired private VoteService voteService;

    public Map<String, Integer> tallyVotes(Long electionId) {
        Election election = electionService.getElectionById(electionId);
        
        if (election.getState() != ElectionState.CLOSED) {
            throw new ElectionException("Cannot tally votes for an election that is not CLOSED.");
        }
        
        PaillierUtil paillierUtil = paillierKeyService.getPaillierUtil();
        
        List<Candidate> candidates = candidateService.getCandidatesByElection(electionId);
        List<Vote> votes = voteService.getVotesByElection(electionId);
        
        Map<Long, BigInteger> encryptedTally = new HashMap<>();
        for (Candidate candidate : candidates) {
            // Initialize with an encrypted value of 0, which is 1
            encryptedTally.put(candidate.getId(), paillierUtil.getG().modPow(BigInteger.ZERO, paillierUtil.getN().multiply(paillierUtil.getN())));
        }

        for (Vote vote : votes) {
            if (vote.getEncryptedVote() == null || vote.getEncryptedVote().isEmpty()) {
                continue;
            }
            try {
                BigInteger encryptedVoteValue = new BigInteger(vote.getEncryptedVote());
                Long candidateId = vote.getCandidateId();
                
                if (encryptedTally.containsKey(candidateId)) {
                    BigInteger currentTally = encryptedTally.get(candidateId);
                    BigInteger newTally = paillierUtil.add(currentTally, encryptedVoteValue);
                    encryptedTally.put(candidateId, newTally);
                } else {
                    // This case should ideally not happen if voters cast valid votes for existing candidates
                    System.err.println("Vote cast for an invalid candidateId: " + candidateId);
                }
            } catch (Exception e) {
                // Log and skip invalid votes
                System.err.println("Invalid vote data skipped: " + e.getMessage());
            }
        }
        
        Map<String, Integer> finalResults = new HashMap<>();
        for (Candidate candidate : candidates) {
            BigInteger encryptedCount = encryptedTally.get(candidate.getId());
            BigInteger decryptedCount = paillierUtil.decrypt(encryptedCount);
            finalResults.put(candidate.getName(), decryptedCount.intValue());
        }

        return finalResults;
    }
}