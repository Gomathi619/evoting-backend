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
import java.util.*;

@Service
public class TallyService {

    @Autowired 
    private PaillierKeyService paillierKeyService;
    
    @Autowired 
    private ElectionService electionService;
    
    @Autowired 
    private CandidateService candidateService;
    
    @Autowired 
    private VoteService voteService;
    
    @Autowired 
    private EnhancedThresholdPaillierService thresholdPaillierService;
    
    @Autowired 
    private ImmutableAuditService auditService;

    public Map<String, Object> tallyVotesWithWinner(Long electionId) {
        System.out.println("=== TALLY WITH WINNER STARTED ===");
        
        Election election = electionService.getElectionById(electionId);
        
        if (election.getState() != ElectionState.CLOSED) {
            throw new ElectionException("Cannot tally votes for an election that is not CLOSED.");
        }
        
        List<Candidate> candidates = candidateService.getCandidatesByElection(electionId);
        List<Vote> votes = voteService.getVotesByElection(electionId);
        
        System.out.println("Election: " + election.getTitle());
        System.out.println("Candidates: " + candidates.size());
        System.out.println("Total Votes: " + votes.size());
        
        // Simple count - just count votes per candidate
        Map<Long, Integer> candidateVotes = new HashMap<>();
        for (Candidate candidate : candidates) {
            candidateVotes.put(candidate.getId(), 0);
            System.out.println("Candidate: " + candidate.getName() + " (ID: " + candidate.getId() + ")");
        }
        
        for (Vote vote : votes) {
            Long candidateId = vote.getCandidateId();
            if (candidateVotes.containsKey(candidateId)) {
                candidateVotes.put(candidateId, candidateVotes.get(candidateId) + 1);
                System.out.println("✓ Vote counted for candidate ID: " + candidateId);
            } else {
                System.out.println("✗ Vote for unknown candidate ID: " + candidateId);
            }
        }
        
        Map<String, Integer> candidateResults = new HashMap<>();
        for (Candidate candidate : candidates) {
            int voteCount = candidateVotes.get(candidate.getId());
            candidateResults.put(candidate.getName(), voteCount);
            System.out.println("Result - " + candidate.getName() + ": " + voteCount + " votes");
        }
        
        // Calculate winner
        Map<String, Object> finalResults = new HashMap<>();
        finalResults.put("electionTitle", election.getTitle());
        finalResults.put("totalVotes", votes.size());
        finalResults.put("candidateResults", candidateResults);
        
        // Find winner(s) - handle ties
        List<String> winners = findWinners(candidateResults);
        finalResults.put("winners", winners);
        finalResults.put("isTie", winners.size() > 1);
        
        if (winners.size() == 1) {
            finalResults.put("winner", winners.get(0));
            System.out.println("🏆 Winner: " + winners.get(0));
        } else {
            finalResults.put("winner", "Tie between " + String.join(", ", winners));
            System.out.println("🤝 Tie between: " + String.join(", ", winners));
        }
        
        System.out.println("=== TALLY WITH WINNER COMPLETED ===");
        System.out.println("Final Results: " + finalResults);
        
        auditService.logEvent("ELECTION_TALLIED_WITH_WINNER", "TallyService",
            "tallyVotesWithWinner", "election:" + electionId + 
            ", candidates:" + candidateResults.size() + 
            ", total_votes:" + votes.size() +
            ", winner:" + finalResults.get("winner"));

        return finalResults;
    }

    private List<String> findWinners(Map<String, Integer> candidateResults) {
        List<String> winners = new ArrayList<>();
        int maxVotes = -1;
        
        for (Map.Entry<String, Integer> entry : candidateResults.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winners.clear();
                winners.add(entry.getKey());
            } else if (entry.getValue() == maxVotes) {
                winners.add(entry.getKey());
            }
        }
        
        return winners;
    }

    public Map<String, Integer> tallyVotes(Long electionId) {
        System.out.println("=== TALLY DEBUG START ===");
        Election election = electionService.getElectionById(electionId);
        
        if (election.getState() != ElectionState.CLOSED) {
            throw new ElectionException("Cannot tally votes for an election that is not CLOSED.");
        }
        
        PaillierUtil paillierUtil = paillierKeyService.getPaillierUtil();
        
        List<Candidate> candidates = candidateService.getCandidatesByElection(electionId);
        List<Vote> votes = voteService.getVotesByElection(electionId);
        
        System.out.println("Total candidates: " + candidates.size());
        System.out.println("Total votes: " + votes.size());
        
        Map<Long, BigInteger> encryptedTally = new HashMap<>();
        for (Candidate candidate : candidates) {
            encryptedTally.put(candidate.getId(), 
                paillierUtil.getG().modPow(BigInteger.ZERO, 
                paillierUtil.getN().multiply(paillierUtil.getN())));
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
                }
            } catch (Exception e) {
                System.err.println("Invalid vote data skipped: " + e.getMessage());
            }
        }
        
        Map<String, Integer> finalResults = new HashMap<>();
        for (Candidate candidate : candidates) {
            BigInteger encryptedCount = encryptedTally.get(candidate.getId());
            BigInteger decryptedCount = paillierUtil.decrypt(encryptedCount);
            finalResults.put(candidate.getName(), decryptedCount.intValue());
        }

        System.out.println("=== TALLY DEBUG END ===");

        auditService.logEvent("ELECTION_TALLIED", "TallyService",
            "tallyVotes", "election:" + electionId + 
            ", candidates:" + finalResults.size() + 
            ", total_votes:" + votes.size());

        return finalResults;
    }

    /**
     * Simple tally without Paillier - for immediate results
     */
    public Map<String, Integer> simpleTallyVotes(Long electionId) {
        System.out.println("=== SIMPLE TALLY STARTED ===");
        
        Election election = electionService.getElectionById(electionId);
        
        if (election.getState() != ElectionState.CLOSED) {
            throw new ElectionException("Cannot tally votes for an election that is not CLOSED.");
        }
        
        List<Candidate> candidates = candidateService.getCandidatesByElection(electionId);
        List<Vote> votes = voteService.getVotesByElection(electionId);
        
        // Simple count - just count votes per candidate
        Map<Long, Integer> candidateVotes = new HashMap<>();
        for (Candidate candidate : candidates) {
            candidateVotes.put(candidate.getId(), 0);
        }
        
        for (Vote vote : votes) {
            Long candidateId = vote.getCandidateId();
            if (candidateVotes.containsKey(candidateId)) {
                candidateVotes.put(candidateId, candidateVotes.get(candidateId) + 1);
            }
        }
        
        Map<String, Integer> finalResults = new HashMap<>();
        for (Candidate candidate : candidates) {
            int voteCount = candidateVotes.get(candidate.getId());
            finalResults.put(candidate.getName(), voteCount);
        }
        
        auditService.logEvent("ELECTION_TALLIED_SIMPLE", "TallyService",
            "simpleTallyVotes", "election:" + electionId + 
            ", candidates:" + finalResults.size() + 
            ", total_votes:" + votes.size());

        return finalResults;
    }

    public Map<String, Object> tallyVotesWithThreshold(Long electionId, Map<String, BigInteger> trusteeShares) {
        Election election = electionService.getElectionById(electionId);
        
        if (election.getState() != ElectionState.CLOSED) {
            throw new ElectionException("Cannot tally votes for an election that is not CLOSED.");
        }
        
        Map<String, BigInteger> encryptedTally = computeEncryptedTally(electionId);
        
        Map<String, BigInteger> decryptedResults = new HashMap<>();
        for (Map.Entry<String, BigInteger> entry : encryptedTally.entrySet()) {
            BigInteger decryptedCount = thresholdPaillierService.thresholdDecrypt(
                entry.getValue(), trusteeShares);
            decryptedResults.put(entry.getKey(), decryptedCount);
        }
        
        Map<String, Integer> finalResults = new HashMap<>();
        for (Map.Entry<String, BigInteger> entry : decryptedResults.entrySet()) {
            finalResults.put(entry.getKey(), entry.getValue().intValue());
        }
        
        auditService.logEvent("ELECTION_TALLIED_THRESHOLD", "TallyService",
            "tallyVotesWithThreshold", "election:" + electionId + 
            ", shares_used:" + trusteeShares.size() + 
            ", candidates:" + finalResults.size());
            
        Map<String, Object> result = new HashMap<>();
        result.put("electionId", electionId);
        result.put("results", finalResults);
        result.put("thresholdUsed", true);
        result.put("trusteeSharesCount", trusteeShares.size());
        result.put("timestamp", java.time.LocalDateTime.now());
        
        return result;
    }
    
    private Map<String, BigInteger> computeEncryptedTally(Long electionId) {
        PaillierUtil paillierUtil = paillierKeyService.getPaillierUtil();
        List<Candidate> candidates = candidateService.getCandidatesByElection(electionId);
        List<Vote> votes = voteService.getVotesByElection(electionId);
        
        Map<String, BigInteger> encryptedTally = new HashMap<>();
        
        for (Candidate candidate : candidates) {
            encryptedTally.put(candidate.getName(), 
                paillierUtil.getG().modPow(BigInteger.ZERO, 
                paillierUtil.getN().multiply(paillierUtil.getN())));
        }

        for (Vote vote : votes) {
            if (vote.getEncryptedVote() == null || vote.getEncryptedVote().isEmpty()) {
                continue;
            }
            try {
                BigInteger encryptedVoteValue = new BigInteger(vote.getEncryptedVote());
                Candidate candidate = candidateService.getCandidateById(vote.getCandidateId());
                
                if (candidate != null && encryptedTally.containsKey(candidate.getName())) {
                    BigInteger currentTally = encryptedTally.get(candidate.getName());
                    BigInteger newTally = paillierUtil.add(currentTally, encryptedVoteValue);
                    encryptedTally.put(candidate.getName(), newTally);
                }
            } catch (Exception e) {
                System.err.println("Invalid vote data skipped: " + e.getMessage());
            }
        }
        
        return encryptedTally;
    }
    
    public EnhancedThresholdPaillierService.DecryptionStatus getTallyReadiness(Long electionId) {
        return thresholdPaillierService.getTallyReadiness(electionId);
    }
}