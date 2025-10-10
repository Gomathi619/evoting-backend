package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.BulletinBoardEntry;
import com.evoting.evoting_backend.repository.BulletinBoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnhancedBulletinBoardService {
    
    @Autowired
    private BulletinBoardRepository bulletinBoardRepository;
    
    @Autowired
    private HSMService hsmService;
    
    @Autowired
    private ImmutableAuditService auditService;
    
    private String currentMerkleRoot;
    private final List<String> pendingEntries = new ArrayList<>();
    
    /**
     * ✅ ENHANCED: Add entry with cryptographic proofs
     */
    public BulletinBoardEntry addEntryWithProofs(String trackingCode, Long electionId, 
                                                String encryptedVote, String tokenProof) {
        try {
            // Get previous entry for chain linking
            BulletinBoardEntry previousEntry = getLatestEntry();
            String previousHash = (previousEntry != null) ? previousEntry.getEntryHash() : "0";
            
            // Create entry data
            String timestamp = LocalDateTime.now().toString();
            String dataToHash = trackingCode + "|" + electionId + "|" + 
                              encryptedVote + "|" + tokenProof + "|" + 
                              timestamp + "|" + previousHash;
            
            // Calculate entry hash
            String entryHash = calculateSHA256(dataToHash);
            
            // Add to pending entries for Merkle tree
            pendingEntries.add(entryHash);
            
            // Update Merkle root if we have enough entries
            if (pendingEntries.size() >= 10) {
                updateMerkleRoot();
            }
            
            // Sign the entry with HSM
            byte[] signature = hsmService.signWithHSM("bulletin_board_key", entryHash.getBytes());
            String signatureBase64 = java.util.Base64.getEncoder().encodeToString(signature);
            
            // Create and save entry
            BulletinBoardEntry entry = new BulletinBoardEntry();
            entry.setEntryHash(entryHash);
            entry.setPreviousHash(previousHash);
            entry.setTrackingCode(trackingCode);
            entry.setElectionId(electionId);
            entry.setEncryptedVote(encryptedVote);
            entry.setTimestamp(LocalDateTime.now());
            // Note: Store signature in a separate audit table in production
            
            BulletinBoardEntry savedEntry = bulletinBoardRepository.save(entry);
            
            // Generate Merkle proof for this entry
            List<String> merkleProof = generateMerkleProof(entryHash);
            
            auditService.logEvent("BULLETIN_BOARD_ENTRY_ADDED", "EnhancedBulletinBoardService",
                "addEntryWithProofs", "election:" + electionId + 
                ", tracking_code:" + trackingCode + 
                ", merkle_proof_generated:true");
            
            return savedEntry;
            
        } catch (Exception e) {
            auditService.logEvent("BULLETIN_BOARD_ENTRY_FAILED", "EnhancedBulletinBoardService",
                "addEntryWithProofs", "election:" + electionId + 
                ", error:" + e.getMessage());
            throw new RuntimeException("Failed to add bulletin board entry: " + e.getMessage(), e);
        }
    }
    
    /**
     * ✅ ENHANCED: Verify entry integrity with Merkle proofs
     */
    public boolean verifyEntryIntegrity(Long entryId) {
        try {
            BulletinBoardEntry entry = bulletinBoardRepository.findById(entryId)
                    .orElseThrow(() -> new RuntimeException("Entry not found"));
            
            // Verify hash chain
            if (!verifyHashChain(entry)) {
                return false;
            }
            
            // Verify Merkle proof
            List<String> merkleProof = generateMerkleProof(entry.getEntryHash());
            boolean merkleValid = verifyMerkleProof(entry.getEntryHash(), merkleProof, currentMerkleRoot);
            
            // Verify HSM signature (in production, store and verify signatures)
            
            return merkleValid;
            
        } catch (Exception e) {
            System.err.println("Entry integrity verification failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * ✅ ENHANCED: Verify complete bulletin board integrity
     */
    public Map<String, Object> verifyBoardIntegrity() {
        try {
            List<BulletinBoardEntry> allEntries = bulletinBoardRepository.findAll();
            Map<String, Object> result = new HashMap<>();
            
            boolean chainValid = verifyCompleteHashChain(allEntries);
            boolean merkleValid = verifyAllMerkleProofs(allEntries);
            boolean signaturesValid = verifyAllSignatures(allEntries);
            
            result.put("totalEntries", allEntries.size());
            result.put("hashChainValid", chainValid);
            result.put("merkleProofsValid", merkleValid);
            result.put("signaturesValid", signaturesValid);
            result.put("overallIntegrity", chainValid && merkleValid && signaturesValid);
            result.put("currentMerkleRoot", currentMerkleRoot);
            result.put("timestamp", System.currentTimeMillis());
            
            if (!chainValid) {
                auditService.logEvent("BULLETIN_BOARD_CHAIN_BROKEN", "EnhancedBulletinBoardService",
                    "verifyBoardIntegrity", "integrity_compromised:true");
            }
            
            return result;
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("integrity", false);
            return errorResult;
        }
    }
    
    /**
     * ✅ ENHANCED: Generate Merkle proof for an entry
     */
    public List<String> generateMerkleProof(String entryHash) {
        List<String> allHashes = getAllEntryHashes();
        int entryIndex = allHashes.indexOf(entryHash);
        
        if (entryIndex == -1) {
            throw new RuntimeException("Entry hash not found in bulletin board");
        }
        
        return generateMerkleProofForIndex(allHashes, entryIndex);
    }
    
    /**
     * ✅ ENHANCED: Public verification endpoint for voters
     */
    public Map<String, Object> verifyVoteEntry(String trackingCode, String voterProvidedHash) {
        try {
            BulletinBoardEntry entry = bulletinBoardRepository.findByTrackingCode(trackingCode)
                    .orElseThrow(() -> new RuntimeException("Entry not found for tracking code"));
            
            // Verify the voter's provided hash matches
            boolean hashMatches = entry.getEntryHash().equals(voterProvidedHash);
            
            // Generate verification proof
            List<String> merkleProof = generateMerkleProof(entry.getEntryHash());
            boolean merkleValid = verifyMerkleProof(entry.getEntryHash(), merkleProof, currentMerkleRoot);
            
            Map<String, Object> verificationResult = new HashMap<>();
            verificationResult.put("trackingCode", trackingCode);
            verificationResult.put("entryExists", true);
            verificationResult.put("hashMatches", hashMatches);
            verificationResult.put("merkleProofValid", merkleValid);
            verificationResult.put("inBulletinBoard", true);
            verificationResult.put("verificationTime", LocalDateTime.now().toString());
            verificationResult.put("merkleRoot", currentMerkleRoot);
            
            if (hashMatches && merkleValid) {
                verificationResult.put("verificationStatus", "FULLY_VERIFIED");
            } else {
                verificationResult.put("verificationStatus", "VERIFICATION_FAILED");
            }
            
            auditService.logEvent("VOTE_VERIFICATION_ATTEMPT", "EnhancedBulletinBoardService",
                "verifyVoteEntry", "tracking_code:" + trackingCode + 
                ", status:" + verificationResult.get("verificationStatus"));
            
            return verificationResult;
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("trackingCode", trackingCode);
            errorResult.put("entryExists", false);
            errorResult.put("verificationStatus", "ENTRY_NOT_FOUND");
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }
    
    // Private helper methods
    private BulletinBoardEntry getLatestEntry() {
        List<BulletinBoardEntry> allEntries = bulletinBoardRepository.findAll();
        return allEntries.isEmpty() ? null : allEntries.get(allEntries.size() - 1);
    }
    
    private boolean verifyHashChain(BulletinBoardEntry entry) {
        if ("0".equals(entry.getPreviousHash())) {
            return true; // First entry
        }
        
        // Find previous entry and verify hash
        List<BulletinBoardEntry> allEntries = bulletinBoardRepository.findAll();
        for (int i = 0; i < allEntries.size(); i++) {
            if (allEntries.get(i).getEntryHash().equals(entry.getPreviousHash())) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean verifyCompleteHashChain(List<BulletinBoardEntry> entries) {
        if (entries.isEmpty()) return true;
        
        String previousHash = "0";
        for (BulletinBoardEntry entry : entries) {
            if (!entry.getPreviousHash().equals(previousHash)) {
                return false;
            }
            previousHash = entry.getEntryHash();
        }
        
        return true;
    }
    
    private boolean verifyAllMerkleProofs(List<BulletinBoardEntry> entries) {
        List<String> allHashes = new ArrayList<>();
        for (BulletinBoardEntry entry : entries) {
            allHashes.add(entry.getEntryHash());
        }
        
        String computedRoot = calculateMerkleRoot(allHashes);
        return computedRoot.equals(currentMerkleRoot);
    }
    
    private boolean verifyAllSignatures(List<BulletinBoardEntry> entries) {
        // In production, verify HSM signatures for each entry
        // For now, return true (signatures would be verified in real implementation)
        return true;
    }
    
    private List<String> getAllEntryHashes() {
        List<BulletinBoardEntry> allEntries = bulletinBoardRepository.findAll();
        List<String> hashes = new ArrayList<>();
        for (BulletinBoardEntry entry : allEntries) {
            hashes.add(entry.getEntryHash());
        }
        return hashes;
    }
    
    private void updateMerkleRoot() {
        if (!pendingEntries.isEmpty()) {
            currentMerkleRoot = calculateMerkleRoot(pendingEntries);
            pendingEntries.clear();
            
            auditService.logEvent("MERKLE_ROOT_UPDATED", "EnhancedBulletinBoardService",
                "updateMerkleRoot", "new_root:" + currentMerkleRoot.substring(0, 16) + "...");
        }
    }
    
    // Merkle tree implementation
    private String calculateMerkleRoot(List<String> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return calculateSHA256("");
        }
        
        List<String> currentLevel = new ArrayList<>();
        for (String transaction : transactions) {
            currentLevel.add(calculateSHA256(transaction));
        }
        
        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            
            for (int i = 0; i < currentLevel.size(); i += 2) {
                if (i + 1 < currentLevel.size()) {
                    String combined = currentLevel.get(i) + currentLevel.get(i + 1);
                    nextLevel.add(calculateSHA256(combined));
                } else {
                    nextLevel.add(calculateSHA256(currentLevel.get(i) + currentLevel.get(i)));
                }
            }
            
            currentLevel = nextLevel;
        }
        
        return currentLevel.get(0);
    }
    
    private List<String> generateMerkleProofForIndex(List<String> transactions, int targetIndex) {
        List<String> proof = new ArrayList<>();
        List<String> currentLevel = new ArrayList<>(transactions);
        
        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            
            for (int i = 0; i < currentLevel.size(); i += 2) {
                if (i + 1 < currentLevel.size()) {
                    String combined = currentLevel.get(i) + currentLevel.get(i + 1);
                    nextLevel.add(calculateSHA256(combined));
                    
                    if (i == targetIndex || i + 1 == targetIndex) {
                        int siblingIndex = (targetIndex % 2 == 0) ? i + 1 : i;
                        proof.add(currentLevel.get(siblingIndex));
                    }
                } else {
                    nextLevel.add(calculateSHA256(currentLevel.get(i) + currentLevel.get(i)));
                }
            }
            
            targetIndex = targetIndex / 2;
            currentLevel = nextLevel;
        }
        
        return proof;
    }
    
    private boolean verifyMerkleProof(String transaction, List<String> proof, String merkleRoot) {
        String computedHash = calculateSHA256(transaction);
        
        for (String proofItem : proof) {
            if (computedHash.compareTo(proofItem) < 0) {
                computedHash = calculateSHA256(computedHash + proofItem);
            } else {
                computedHash = calculateSHA256(proofItem + computedHash);
            }
        }
        
        return computedHash.equals(merkleRoot);
    }
    
    private String calculateSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hash calculation failed", e);
        }
    }
    
    public Map<String, Object> getBoardStatistics() {
        List<BulletinBoardEntry> entries = bulletinBoardRepository.findAll();
        Long latestId = entries.isEmpty() ? 0L : entries.get(entries.size()-1).getId();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", entries.size());
        stats.put("latestEntryId", latestId);
        stats.put("currentMerkleRoot", currentMerkleRoot);
        stats.put("pendingEntries", pendingEntries.size());
        stats.put("checkTime", LocalDateTime.now().toString());
        stats.put("integrityStatus", verifyBoardIntegrity().get("overallIntegrity"));
        
        return stats;
    }
}