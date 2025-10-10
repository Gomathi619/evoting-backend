package com.evoting.evoting_backend.service;

import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

@Service
public class ThresholdKeyService {
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Shamir's Secret Sharing implementation for threshold cryptography
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
    
    // Split a secret into n shares where k shares are required to reconstruct
    public List<SecretShare> splitSecret(BigInteger secret, int n, int k) {
        if (k > n) {
            throw new IllegalArgumentException("K must be less than or equal to N");
        }
        
        // Generate random coefficients for the polynomial
        BigInteger[] coefficients = new BigInteger[k];
        coefficients[0] = secret; // Constant term is the secret
        
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
    
    // Reconstruct secret from shares using Lagrange interpolation
    public BigInteger reconstructSecret(List<SecretShare> shares) {
        if (shares.isEmpty()) {
            throw new IllegalArgumentException("No shares provided");
        }
        
        BigInteger secret = BigInteger.ZERO;
        
        for (SecretShare share : shares) {
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;
            
            for (SecretShare otherShare : shares) {
                if (share.getX() != otherShare.getX()) {
                    numerator = numerator.multiply(BigInteger.valueOf(-otherShare.getX()));
                    denominator = denominator.multiply(BigInteger.valueOf(share.getX() - otherShare.getX()));
                }
            }
            
            BigInteger lagrangeCoefficient = numerator.multiply(denominator.modInverse(getPrime()));
            secret = secret.add(share.getY().multiply(lagrangeCoefficient));
        }
        
        return secret.mod(getPrime());
    }
    
    private BigInteger evaluatePolynomial(BigInteger[] coefficients, BigInteger x) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < coefficients.length; i++) {
            result = result.add(coefficients[i].multiply(x.pow(i)));
        }
        return result.mod(getPrime());
    }
    
    // Use a large prime for the finite field
    private BigInteger getPrime() {
        return new BigInteger("32317006071311007300714876688669951960444102669715484032130345427524655138867890893197201411522913463688717960921898019494119559150490921095088152386448283120630877367300996091750197750389652106796057638384067568276792218642619756161838094338476170470581645852036305042887575891541065808607552399123930385521914333389668342420684974786564569494856176035326322058077805659331026192708460314150258592864177116725943603718461857357598351152301645904403697613233287231227125684710820209725157101726931323469678542580656697935045997268352998638215525166389437335543602135433229604645318478604952148193555853611059596230656");
    }
    
    // Generate threshold keys for blind signature
    public Map<String, Object> generateThresholdBlindSignatureKeys(int trustees, int threshold) {
        // Generate the master private key
        BigInteger masterPrivate = new BigInteger(2048, secureRandom);
        
        // Split into shares
        List<SecretShare> shares = splitSecret(masterPrivate, trustees, threshold);
        
        Map<String, Object> result = new HashMap<>();
        result.put("masterPublic", masterPrivate.modInverse(getPrime())); // Simplified public key
        result.put("shares", shares);
        result.put("trustees", trustees);
        result.put("threshold", threshold);
        
        return result;
    }
}