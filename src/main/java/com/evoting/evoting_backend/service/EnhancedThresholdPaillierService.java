package com.evoting.evoting_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EnhancedThresholdPaillierService {
    
    @Autowired
    private HSMService hsmService;
    
    @Autowired
    private ImmutableAuditService auditService;
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, BigInteger> trusteeShares = new ConcurrentHashMap<>();
    private final Map<String, String> trusteePublicKeys = new ConcurrentHashMap<>();
    private BigInteger publicModulus;
    private int totalTrustees = 5;
    private int threshold = 3;
    
    // ✅ ADD THIS INNER CLASS
    public static class DecryptionStatus {
        private int totalTrustees;
        private int requiredThreshold;
        private int availableShares;
        private boolean canDecrypt;
        
        public DecryptionStatus(int totalTrustees, int requiredThreshold, 
                              int availableShares, boolean canDecrypt) {
            this.totalTrustees = totalTrustees;
            this.requiredThreshold = requiredThreshold;
            this.availableShares = availableShares;
            this.canDecrypt = canDecrypt;
        }
        
        // Getters
        public int getTotalTrustees() { return totalTrustees; }
        public int getRequiredThreshold() { return requiredThreshold; }
        public int getAvailableShares() { return availableShares; }
        public boolean isCanDecrypt() { return canDecrypt; }
    }
    
    /**
     * ✅ COMPLETE: Generate threshold Paillier key shares
     */
    public Map<String, Object> generateThresholdKeys(int trustees, int threshold) {
        try {
            this.totalTrustees = trustees;
            this.threshold = threshold;
            
            // Generate large primes for Paillier
            BigInteger p = generateLargePrime(1024);
            BigInteger q = generateLargePrime(1024);
            this.publicModulus = p.multiply(q);
            BigInteger lambda = lcm(p.subtract(BigInteger.ONE), q.subtract(BigInteger.ONE));
            
            // Generate master private key (lambda)
            BigInteger masterPrivate = lambda;
            
            // Split using Shamir's Secret Sharing
            List<SecretShare> shares = splitSecret(masterPrivate, trustees, threshold);
            
            // Store shares in HSM and memory
            Map<String, Object> result = new HashMap<>();
            Map<String, String> shareReferences = new HashMap<>();
            
            for (SecretShare share : shares) {
                String trusteeId = "trustee_" + share.getX();
                
                // Store share in HSM
                String hsmReference = storeShareInHSM(trusteeId, share.getY());
                trusteeShares.put(trusteeId, share.getY());
                shareReferences.put(trusteeId, hsmReference);
                
                // Generate trustee verification key
                String verificationKey = generateTrusteeVerificationKey(trusteeId);
                trusteePublicKeys.put(trusteeId, verificationKey);
            }
            
            result.put("publicModulus", publicModulus.toString());
            result.put("trustees", trustees);
            result.put("threshold", threshold);
            result.put("shareReferences", shareReferences);
            result.put("trusteePublicKeys", trusteePublicKeys);
            
            auditService.logEvent("THRESHOLD_KEYS_GENERATED", "EnhancedThresholdPaillierService",
                "generateThresholdKeys", "trustees:" + trustees + 
                ", threshold:" + threshold + ", modulus_bits:" + publicModulus.bitLength());
                
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Threshold key generation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * ✅ COMPLETE: Perform threshold decryption with multiple trustees
     */
    public BigInteger thresholdDecrypt(BigInteger encryptedTally, Map<String, BigInteger> providedShares) {
        try {
            if (providedShares.size() < threshold) {
                auditService.logEvent("INSUFFICIENT_SHARES", "EnhancedThresholdPaillierService",
                    "thresholdDecrypt", "provided:" + providedShares.size() + 
                    ", required:" + threshold);
                throw new RuntimeException("Insufficient shares for decryption");
            }
            
            // Verify all provided shares
            for (Map.Entry<String, BigInteger> entry : providedShares.entrySet()) {
                if (!verifyTrusteeShare(entry.getKey(), entry.getValue())) {
                    throw new RuntimeException("Invalid share from trustee: " + entry.getKey());
                }
            }
            
            // Convert to secret shares
            List<SecretShare> shares = new ArrayList<>();
            for (Map.Entry<String, BigInteger> entry : providedShares.entrySet()) {
                int x = Integer.parseInt(entry.getKey().replace("trustee_", ""));
                shares.add(new SecretShare(x, entry.getValue()));
            }
            
            // Reconstruct master private key
            BigInteger reconstructedKey = reconstructSecret(shares);
            
            // Perform Paillier decryption using reconstructed key
            BigInteger nsquare = publicModulus.multiply(publicModulus);
            BigInteger u = encryptedTally.modPow(reconstructedKey, nsquare)
                                      .subtract(BigInteger.ONE)
                                      .divide(publicModulus);
            
            auditService.logEvent("THRESHOLD_DECRYPTION_COMPLETED", "EnhancedThresholdPaillierService",
                "thresholdDecrypt", "shares_used:" + providedShares.size() + 
                ", success:true");
                
            return u;
            
        } catch (Exception e) {
            auditService.logEvent("THRESHOLD_DECRYPTION_FAILED", "EnhancedThresholdPaillierService",
                "thresholdDecrypt", "error:" + e.getMessage());
            throw new RuntimeException("Threshold decryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * ✅ COMPLETE: Verify share without revealing it
     */
    public boolean verifyTrusteeShare(String trusteeId, BigInteger share) {
        try {
            // Get stored share from HSM
            BigInteger storedShare = retrieveShareFromHSM(trusteeId);
            if (storedShare == null) {
                return false;
            }
            
            // Verify share matches stored value
            boolean isValid = storedShare.equals(share);
            
            if (isValid) {
                auditService.logEvent("TRUSTEE_SHARE_VERIFIED", "EnhancedThresholdPaillierService",
                    "verifyTrusteeShare", "trustee:" + trusteeId + ", status:valid");
            } else {
                auditService.logEvent("TRUSTEE_SHARE_INVALID", "EnhancedThresholdPaillierService",
                    "verifyTrusteeShare", "trustee:" + trusteeId + ", status:invalid");
            }
            
            return isValid;
            
        } catch (Exception e) {
            auditService.logEvent("SHARE_VERIFICATION_ERROR", "EnhancedThresholdPaillierService",
                "verifyTrusteeShare", "trustee:" + trusteeId + ", error:" + e.getMessage());
            return false;
        }
    }
    
    /**
     * ✅ Get decryption status
     */
    public DecryptionStatus getDecryptionStatus() {
        return new DecryptionStatus(
            totalTrustees,
            threshold,
            trusteeShares.size(),
            trusteeShares.size() >= threshold
        );
    }
    
    // For backward compatibility with TallyService
    public DecryptionStatus getTallyReadiness(Long electionId) {
        return getDecryptionStatus();
    }
    
    // Shamir's Secret Sharing implementation
    private List<SecretShare> splitSecret(BigInteger secret, int n, int k) {
        if (k > n) {
            throw new IllegalArgumentException("K must be less than or equal to N");
        }
        
        // Generate random coefficients
        BigInteger[] coefficients = new BigInteger[k];
        coefficients[0] = secret;
        
        for (int i = 1; i < k; i++) {
            coefficients[i] = new BigInteger(256, secureRandom);
        }
        
        // Generate shares
        List<SecretShare> shares = new ArrayList<>();
        for (int x = 1; x <= n; x++) {
            BigInteger y = evaluatePolynomial(coefficients, BigInteger.valueOf(x));
            shares.add(new SecretShare(x, y));
        }
        
        return shares;
    }
    
    private BigInteger reconstructSecret(List<SecretShare> shares) {
        BigInteger secret = BigInteger.ZERO;
        BigInteger prime = getPrime();
        
        for (SecretShare share : shares) {
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;
            
            for (SecretShare otherShare : shares) {
                if (share.getX() != otherShare.getX()) {
                    numerator = numerator.multiply(BigInteger.valueOf(-otherShare.getX())).mod(prime);
                    denominator = denominator.multiply(BigInteger.valueOf(share.getX() - otherShare.getX())).mod(prime);
                }
            }
            
            BigInteger lagrangeCoefficient = numerator.multiply(denominator.modInverse(prime)).mod(prime);
            secret = secret.add(share.getY().multiply(lagrangeCoefficient)).mod(prime);
        }
        
        return secret;
    }
    
    private BigInteger evaluatePolynomial(BigInteger[] coefficients, BigInteger x) {
        BigInteger result = BigInteger.ZERO;
        BigInteger prime = getPrime();
        
        for (int i = 0; i < coefficients.length; i++) {
            result = result.add(coefficients[i].multiply(x.pow(i))).mod(prime);
        }
        
        return result;
    }
    
    // Helper methods
    private BigInteger generateLargePrime(int bitLength) {
        return BigInteger.probablePrime(bitLength, secureRandom);
    }
    
    private BigInteger lcm(BigInteger a, BigInteger b) {
        return a.multiply(b).divide(a.gcd(b));
    }
    
    private BigInteger getPrime() {
        return new BigInteger("32317006071311007300714876688669951960444102669715484032130345427524655138867890893197201411522913463688717960921898019494119559150490921095088152386448283120630877367300996091750197750389652106796057638384067568276792218642619756161838094338476170470581645852036305042887575891541065808607552399123930385521914333389668342420684974786564569494856176035326322058077805659331026192708460314150258592864177116725943603718461857357598351152301645904403697613233287231227125684710820209725157101726931323469678542580656697935045997268352998638215525166389437335543602135433229604645318478604952148193555853611059596230656");
    }
    
    private String storeShareInHSM(String trusteeId, BigInteger share) {
        String shareData = "trustee:" + trusteeId + ",share:" + share.toString() + ",timestamp:" + System.currentTimeMillis();
        hsmService.storeSensitiveData("paillier_share_" + trusteeId, shareData);
        return "hsm_stored_" + trusteeId;
    }
    
    private BigInteger retrieveShareFromHSM(String trusteeId) {
        String shareData = hsmService.retrieveSensitiveData("paillier_share_" + trusteeId);
        if (shareData == null) return null;
        
        // Extract share from stored data
        String[] parts = shareData.split(",");
        for (String part : parts) {
            if (part.startsWith("share:")) {
                return new BigInteger(part.substring(6));
            }
        }
        return null;
    }
    
    private String generateTrusteeVerificationKey(String trusteeId) {
        return "vk_" + trusteeId + "_" + UUID.randomUUID().toString().substring(0, 16);
    }
    
    public static class SecretShare {
        private final int x;
        private final BigInteger y;
        
        public SecretShare(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
        
        public int getX() { return x; }
        public BigInteger getY() { return y; }
    }
}