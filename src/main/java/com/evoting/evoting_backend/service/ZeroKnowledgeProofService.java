package com.evoting.evoting_backend.service;
import org.springframework.beans.factory.annotation.Autowired;
import com.evoting.evoting_backend.service.ImmutableAuditService;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;


@Service
public class ZeroKnowledgeProofService {
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    // ZKP for proving vote is for a valid candidate without revealing which one
    public Map<String, Object> generateVoteValidityProof(BigInteger encryptedVote, Long[] validCandidateIds, BigInteger publicKey) {
        try {
            Map<String, Object> proof = new HashMap<>();
            
            // Step 1: Commitment phase - hide which candidate was chosen
            BigInteger commitment = generateCommitment(encryptedVote, publicKey);
            proof.put("commitment", commitment.toString());
            
            // Step 2: Challenge phase (simulated)
            BigInteger challenge = generateChallenge(encryptedVote, commitment);
            proof.put("challenge", challenge.toString());
            
            // Step 3: Response phase - prove knowledge without revealing vote
            BigInteger response = generateResponse(encryptedVote, validCandidateIds, challenge, publicKey);
            proof.put("response", response.toString());
            
            // Step 4: Non-interactive proof (Fiat-Shamir transform)
            String nonInteractiveProof = generateNonInteractiveProof(encryptedVote, validCandidateIds, publicKey);
            proof.put("nonInteractiveProof", nonInteractiveProof);
            
            proof.put("timestamp", System.currentTimeMillis());
            proof.put("proofType", "vote_validity_zkp");
            
            return proof;
            
        } catch (Exception e) {
            throw new RuntimeException("ZKP generation failed: " + e.getMessage(), e);
        }
    }
    
    // Verify ZKP without learning the actual vote
    public boolean verifyVoteValidityProof(BigInteger encryptedVote, Map<String, Object> proof, 
                                         Long[] validCandidateIds, BigInteger publicKey) {
        try {
            String nonInteractiveProof = (String) proof.get("nonInteractiveProof");
            BigInteger commitment = new BigInteger((String) proof.get("commitment"));
            BigInteger response = new BigInteger((String) proof.get("response"));
            
            // Recompute the challenge using Fiat-Shamir
            BigInteger computedChallenge = computeFiatShamirChallenge(encryptedVote, commitment, validCandidateIds);
            
            // Verify the proof
            boolean isValid = verifyZKProof(encryptedVote, computedChallenge, response, validCandidateIds, publicKey);
            
            // Additional verification for non-interactive proof
            boolean nonInteractiveValid = verifyNonInteractiveProof(encryptedVote, nonInteractiveProof, validCandidateIds, publicKey);
            
            return isValid && nonInteractiveValid;
            
        } catch (Exception e) {
            System.err.println("ZKP verification failed: " + e.getMessage());
            return false;
        }
    }
    
    // ZKP for token ownership without revealing token details
    public Map<String, Object> generateTokenOwnershipProof(String anonymousToken, BigInteger publicModulus) {
        try {
            Map<String, Object> proof = new HashMap<>();
            
            // Simulate ZKP for token ownership
            // In real implementation, use proper ZKP protocols like Schnorr or Bulletproofs
            
            BigInteger tokenHash = hashToBigInteger(anonymousToken);
            BigInteger witness = generateRandomWitness();
            BigInteger commitment = witness.modPow(BigInteger.valueOf(2), publicModulus); // Simple commitment
            
            // Fiat-Shamir challenge
            BigInteger challenge = hashToBigInteger(commitment.toString() + tokenHash.toString());
            BigInteger response = witness.multiply(tokenHash.modPow(challenge, publicModulus)).mod(publicModulus);
            
            proof.put("commitment", commitment.toString());
            proof.put("challenge", challenge.toString());
            proof.put("response", response.toString());
            proof.put("proofType", "token_ownership_zkp");
            proof.put("timestamp", System.currentTimeMillis());
            
            return proof;
            
        } catch (Exception e) {
            throw new RuntimeException("Token ownership ZKP failed: " + e.getMessage(), e);
        }
    }
    
    public boolean verifyTokenOwnershipProof(String anonymousToken, Map<String, Object> proof, BigInteger publicModulus) {
        try {
            BigInteger commitment = new BigInteger((String) proof.get("commitment"));
            BigInteger challenge = new BigInteger((String) proof.get("challenge"));
            BigInteger response = new BigInteger((String) proof.get("response"));
            
            BigInteger tokenHash = hashToBigInteger(anonymousToken);
            
            // Verify: response^2 ≡ commitment * (tokenHash^challenge) mod modulus
            BigInteger leftSide = response.modPow(BigInteger.valueOf(2), publicModulus);
            BigInteger rightSide = commitment.multiply(tokenHash.modPow(challenge, publicModulus)).mod(publicModulus);
            
            return leftSide.equals(rightSide);
            
        } catch (Exception e) {
            System.err.println("Token ownership ZKP verification failed: " + e.getMessage());
            return false;
        }
    }
    
    // Range proof - prove vote is within valid range without revealing exact value
    public Map<String, Object> generateRangeProof(BigInteger encryptedVote, int minValue, int maxValue, BigInteger publicKey) {
        try {
            Map<String, Object> proof = new HashMap<>();
            
            // Simplified range proof using commitment schemes
            // In production, use proper range proofs like Bulletproofs
            
            BigInteger[] commitments = new BigInteger[maxValue - minValue + 1];
            for (int i = 0; i < commitments.length; i++) {
                int value = minValue + i;
                commitments[i] = generateCommitmentForValue(encryptedVote, value, publicKey);
            }
            
            proof.put("commitments", commitments);
            proof.put("minValue", minValue);
            proof.put("maxValue", maxValue);
            proof.put("proofType", "range_proof");
            proof.put("timestamp", System.currentTimeMillis());
            
            return proof;
            
        } catch (Exception e) {
            throw new RuntimeException("Range proof generation failed: " + e.getMessage(), e);
        }
    }
    
    private BigInteger generateCommitment(BigInteger data, BigInteger publicKey) {
        BigInteger random = new BigInteger(256, secureRandom);
        return data.multiply(random.modPow(publicKey, publicKey)).mod(publicKey);
    }
    
    private BigInteger generateChallenge(BigInteger data, BigInteger commitment) {
        String challengeData = data.toString() + commitment.toString() + System.currentTimeMillis();
        return hashToBigInteger(challengeData);
    }
    
    private BigInteger generateResponse(BigInteger encryptedVote, Long[] validCandidateIds, 
                                      BigInteger challenge, BigInteger publicKey) {
        // Simplified response generation
        // In real implementation, use proper ZKP response based on the actual vote
        BigInteger responseBase = encryptedVote.multiply(challenge).mod(publicKey);
        return responseBase.add(BigInteger.valueOf(validCandidateIds.length)).mod(publicKey);
    }
    
    private String generateNonInteractiveProof(BigInteger encryptedVote, Long[] validCandidateIds, BigInteger publicKey) {
        String proofData = encryptedVote.toString();
        for (Long candidateId : validCandidateIds) {
            proofData += candidateId.toString();
        }
        proofData += publicKey.toString() + System.currentTimeMillis();
        
        return calculateSHA256(proofData);
    }
    
    private BigInteger computeFiatShamirChallenge(BigInteger encryptedVote, BigInteger commitment, Long[] validCandidateIds) {
        String challengeData = encryptedVote.toString() + commitment.toString();
        for (Long candidateId : validCandidateIds) {
            challengeData += candidateId.toString();
        }
        return hashToBigInteger(challengeData);
    }
    
    private boolean verifyZKProof(BigInteger encryptedVote, BigInteger challenge, BigInteger response, 
                                Long[] validCandidateIds, BigInteger publicKey) {
        // Simplified verification - in real implementation, use proper ZKP verification
        BigInteger expected = encryptedVote.multiply(challenge).mod(publicKey);
        BigInteger actual = response.subtract(BigInteger.valueOf(validCandidateIds.length)).mod(publicKey);
        
        return expected.equals(actual);
    }
    
    private boolean verifyNonInteractiveProof(BigInteger encryptedVote, String proof, 
                                            Long[] validCandidateIds, BigInteger publicKey) {
        String computedProof = generateNonInteractiveProof(encryptedVote, validCandidateIds, publicKey);
        return proof.equals(computedProof);
    }
    
    private BigInteger generateRandomWitness() {
        return new BigInteger(256, secureRandom);
    }
    
    private BigInteger generateCommitmentForValue(BigInteger encryptedVote, int value, BigInteger publicKey) {
        BigInteger valueBigInt = BigInteger.valueOf(value);
        return encryptedVote.multiply(valueBigInt.modPow(publicKey, publicKey)).mod(publicKey);
    }
    
    private BigInteger hashToBigInteger(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            return new BigInteger(1, hash);
        } catch (Exception e) {
            throw new RuntimeException("Hash computation failed", e);
        }
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
            throw new RuntimeException("SHA-256 computation failed", e);
        }
    }
    
    // Health check for ZKP service
    public boolean healthCheck() {
        try {
            // Test ZKP generation and verification
            BigInteger testVote = BigInteger.valueOf(123);
            Long[] testCandidates = {1L, 2L, 3L};
            BigInteger testKey = BigInteger.valueOf(65537);
            
            Map<String, Object> proof = generateVoteValidityProof(testVote, testCandidates, testKey);
            boolean verified = verifyVoteValidityProof(testVote, proof, testCandidates, testKey);
            
            return verified;
        } catch (Exception e) {
            System.err.println("ZKP health check failed: " + e.getMessage());
            return false;
        }
    }
}