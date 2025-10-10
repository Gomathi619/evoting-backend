package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.crypto.MerkleTree;
import com.evoting.evoting_backend.model.ImmutableAuditLog;
import com.evoting.evoting_backend.repository.ImmutableAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ImmutableAuditService {
    
    @Autowired
    private ImmutableAuditLogRepository auditLogRepository;
    
    @Autowired
    private HSMService hsmService;
    
    private String currentMerkleRoot = MerkleTree.calculateHash("initial_root");
    
    public ImmutableAuditLog logEvent(String eventType, String service, String operation, String eventData) {
        try {
            // Get the previous log entry for chain linking
            ImmutableAuditLog previousLog = auditLogRepository.findTopByOrderByIdDesc()
                    .orElse(null);
            
            String previousHash = (previousLog != null) ? previousLog.getLogHash() : "0";
            
            // Create the log entry
            String logId = UUID.randomUUID().toString();
            String timestamp = LocalDateTime.now().toString();
            
            // Create the data to be hashed
            String dataToHash = eventType + "|" + service + "|" + operation + "|" + 
                              eventData + "|" + timestamp + "|" + previousHash + "|" + logId;
            
            // Calculate hash
            String logHash = MerkleTree.calculateHash(dataToHash);
            
            // Sign the hash with HSM
            byte[] signature = hsmService.signWithHSM("audit_log_key", logHash.getBytes());
            String signatureBase64 = java.util.Base64.getEncoder().encodeToString(signature);
            
            // Update Merkle tree
            List<String> recentLogs = getRecentLogHashes();
            recentLogs.add(logHash);
            currentMerkleRoot = MerkleTree.calculateMerkleRoot(recentLogs);
            
            // Create and save the audit log
            ImmutableAuditLog auditLog = new ImmutableAuditLog();
            auditLog.setLogHash(logHash);
            auditLog.setEventType(eventType);
            auditLog.setService(service);
            auditLog.setOperation(operation);
            auditLog.setEventData(eventData);
            auditLog.setPreviousHash(previousHash);
            auditLog.setSignature(signatureBase64);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setMerkleRoot(currentMerkleRoot);
            
            ImmutableAuditLog savedLog = auditLogRepository.save(auditLog);
            
            System.out.println("Immutable audit log created: " + eventType + " | Hash: " + logHash.substring(0, 16) + "...");
            
            return savedLog;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create immutable audit log: " + e.getMessage(), e);
        }
    }
    
    public boolean verifyLogIntegrity(Long logId) {
        try {
            ImmutableAuditLog log = auditLogRepository.findById(logId)
                    .orElseThrow(() -> new RuntimeException("Log not found"));
            
            // Recreate the data that was hashed
            String dataToHash = log.getEventType() + "|" + log.getService() + "|" + 
                              log.getOperation() + "|" + log.getEventData() + "|" + 
                              log.getTimestamp().toString() + "|" + log.getPreviousHash() + "|" + 
                              log.getLogHash();
            
            // Verify hash
            String computedHash = MerkleTree.calculateHash(dataToHash);
            if (!computedHash.equals(log.getLogHash())) {
                return false;
            }
            
            // Verify HSM signature
            byte[] signature = java.util.Base64.getDecoder().decode(log.getSignature());
            boolean signatureValid = hsmService.verifyHSMSignature(
                "audit_log_key", log.getLogHash().getBytes(), signature);
            
            if (!signatureValid) {
                return false;
            }
            
            // Verify Merkle proof
            List<ImmutableAuditLog> allLogs = auditLogRepository.findAllByOrderByIdAsc();
            List<String> allHashes = new ArrayList<>();
            for (ImmutableAuditLog auditLog : allLogs) {
                allHashes.add(auditLog.getLogHash());
            }
            
            int logIndex = allLogs.indexOf(log);
            if (logIndex >= 0) {
                List<String> merkleProof = MerkleTree.generateMerkleProof(allHashes, logIndex);
                boolean merkleValid = MerkleTree.verifyMerkleProof(
                    log.getLogHash(), merkleProof, log.getMerkleRoot());
                
                return merkleValid;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("Log integrity verification failed: " + e.getMessage());
            return false;
        }
    }
    
    public boolean verifyAllLogsIntegrity() {
        try {
            List<ImmutableAuditLog> allLogs = auditLogRepository.findAllByOrderByIdAsc();
            
            for (ImmutableAuditLog log : allLogs) {
                if (!verifyLogIntegrity(log.getId())) {
                    System.err.println("Log integrity check failed for ID: " + log.getId());
                    return false;
                }
            }
            
            System.out.println("All " + allLogs.size() + " audit logs verified successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Complete log verification failed: " + e.getMessage());
            return false;
        }
    }
    
    private List<String> getRecentLogHashes() {
        // Get last 100 logs for Merkle tree (or all if less than 100)
        List<ImmutableAuditLog> recentLogs = auditLogRepository.findTop100ByOrderByIdDesc();
        List<String> hashes = new ArrayList<>();
        
        for (ImmutableAuditLog log : recentLogs) {
            hashes.add(0, log.getLogHash()); // Reverse to maintain order
        }
        
        return hashes;
    }
    
    public String getCurrentMerkleRoot() {
        return currentMerkleRoot;
    }
    
    public long getTotalLogCount() {
        return auditLogRepository.count();
    }
    
    // Critical event logging methods
   // Add to ImmutableAuditService.java if not already there
    public void logKYCVerification(Long voterIdentityId, String governmentIdHash, boolean success) {
    String eventData = String.format("voter_identity_id:%d, government_id_hash:%s, success:%b", 
        voterIdentityId, governmentIdHash, success);
    logEvent("KYC_VERIFICATION", "KYCRegistrationService", "verifyIdentity", eventData);
    }
    
    public void logTokenIssuance(Long voterIdentityId, Long electionId, String tokenHash) {
        String eventData = String.format("voter_identity_id:%d, election_id:%d, token_hash:%s", 
            voterIdentityId, electionId, tokenHash.substring(0, 16) + "...");
        logEvent("TOKEN_ISSUANCE", "AnonymousTokenService", "issueAnonymousToken", eventData);
    }
    
    public void logVoteCast(Long electionId, String trackingCode, String tokenHash) {
        String eventData = String.format("election_id:%d, tracking_code:%s, token_hash:%s", 
            electionId, trackingCode, tokenHash.substring(0, 16) + "...");
        logEvent("VOTE_CAST", "EnhancedVoteController", "castSecureVote", eventData);
    }
    
    public void logElectionStateChange(Long electionId, String oldState, String newState) {
        String eventData = String.format("election_id:%d, old_state:%s, new_state:%s", 
            electionId, oldState, newState);
        logEvent("ELECTION_STATE_CHANGE", "ElectionService", "updateElectionState", eventData);
    }
}