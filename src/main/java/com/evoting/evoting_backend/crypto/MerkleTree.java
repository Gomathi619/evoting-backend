package com.evoting.evoting_backend.crypto;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class MerkleTree {
    
    public static String calculateMerkleRoot(List<String> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return calculateHash("");
        }
        
        List<String> currentLevel = new ArrayList<>();
        for (String transaction : transactions) {
            currentLevel.add(calculateHash(transaction));
        }
        
        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            
            for (int i = 0; i < currentLevel.size(); i += 2) {
                if (i + 1 < currentLevel.size()) {
                    String combined = currentLevel.get(i) + currentLevel.get(i + 1);
                    nextLevel.add(calculateHash(combined));
                } else {
                    // Odd number of elements, duplicate the last one
                    nextLevel.add(calculateHash(currentLevel.get(i) + currentLevel.get(i)));
                }
            }
            
            currentLevel = nextLevel;
        }
        
        return currentLevel.get(0);
    }
    
    public static String calculateHash(String data) {
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
    
    public static List<String> generateMerkleProof(List<String> transactions, int targetIndex) {
        List<String> proof = new ArrayList<>();
        List<String> currentLevel = new ArrayList<>();
        
        // Hash all transactions
        for (String transaction : transactions) {
            currentLevel.add(calculateHash(transaction));
        }
        
        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            
            for (int i = 0; i < currentLevel.size(); i += 2) {
                if (i + 1 < currentLevel.size()) {
                    String combined = currentLevel.get(i) + currentLevel.get(i + 1);
                    nextLevel.add(calculateHash(combined));
                    
                    // Add to proof if this pair contains our target
                    if (i == targetIndex || i + 1 == targetIndex) {
                        int siblingIndex = (targetIndex % 2 == 0) ? i + 1 : i;
                        proof.add(currentLevel.get(siblingIndex));
                    }
                } else {
                    nextLevel.add(calculateHash(currentLevel.get(i) + currentLevel.get(i)));
                }
            }
            
            // Update target index for next level
            targetIndex = targetIndex / 2;
            currentLevel = nextLevel;
        }
        
        return proof;
    }
    
    public static boolean verifyMerkleProof(String transaction, List<String> proof, String merkleRoot) {
        String computedHash = calculateHash(transaction);
        
        for (String proofItem : proof) {
            // Determine order based on hash comparison
            if (computedHash.compareTo(proofItem) < 0) {
                computedHash = calculateHash(computedHash + proofItem);
            } else {
                computedHash = calculateHash(proofItem + computedHash);
            }
        }
        
        return computedHash.equals(merkleRoot);
    }
}