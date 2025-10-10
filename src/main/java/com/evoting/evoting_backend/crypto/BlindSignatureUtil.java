package com.evoting.evoting_backend.crypto;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

public class BlindSignatureUtil {
    private BigInteger n; // RSA modulus
    private BigInteger d; // private key
    private BigInteger e; // public exponent
    private SecureRandom random;

    public BlindSignatureUtil(BigInteger n, BigInteger d, BigInteger e) {
        this.n = n;
        this.d = d;
        this.e = e;
        this.random = new SecureRandom();
    }

    // ✅ FIXED: Use cryptographic hash for token message
    public BigInteger createSecureTokenMessage(Long electionId, String sessionId) {
        try {
            // Create a secure random token
            String tokenData = UUID.randomUUID().toString() + ":" + 
                              electionId + ":" + 
                              System.nanoTime() + ":" + 
                              sessionId;
            
            // Hash the token data to create a secure message
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenData.getBytes());
            
            // Ensure the hash is within the RSA modulus range
            BigInteger message = new BigInteger(1, hash);
            if (message.compareTo(n) >= 0) {
                message = message.mod(n);
            }
            
            return message;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create secure token message", e);
        }
    }

    // Client-side: Blind the message
    public BigInteger blind(BigInteger message, BigInteger r) {
        try {
            // Validate inputs
            if (message == null || r == null) {
                throw new IllegalArgumentException("Message and blinding factor cannot be null");
            }
            
            if (message.compareTo(n) >= 0) {
                throw new IllegalArgumentException("Message must be less than modulus n");
            }
            
            // blinding: message * r^e mod n
            BigInteger r_e = r.modPow(e, n);
            BigInteger blinded = message.multiply(r_e).mod(n);
            
            return blinded;
            
        } catch (Exception ex) {
            throw new RuntimeException("Blinding failed: " + ex.getMessage(), ex);
        }
    }

    // Server-side: Sign blinded message
    public BigInteger signBlinded(BigInteger blindedMessage) {
        try {
            // Validate input
            if (blindedMessage == null) {
                throw new IllegalArgumentException("Blinded message cannot be null");
            }
            
            if (blindedMessage.compareTo(n) >= 0) {
                throw new IllegalArgumentException("Blinded message must be less than modulus n");
            }
            
            // sign: (blinded_message)^d mod n
            BigInteger signature = blindedMessage.modPow(d, n);
            
            return signature;
            
        } catch (Exception ex) {
            throw new RuntimeException("Blind signing failed: " + ex.getMessage(), ex);
        }
    }

    // Client-side: Unblind the signature
    public BigInteger unblind(BigInteger blindedSignature, BigInteger r) {
        try {
            // Validate inputs
            if (blindedSignature == null || r == null) {
                throw new IllegalArgumentException("Signature and blinding factor cannot be null");
            }
            
            // unblind: blinded_signature / r mod n
            BigInteger r_inv = r.modInverse(n);
            BigInteger unblindedSignature = blindedSignature.multiply(r_inv).mod(n);
            
            return unblindedSignature;
            
        } catch (Exception ex) {
            throw new RuntimeException("Unblinding failed: " + ex.getMessage(), ex);
        }
    }

    // Generate cryptographically secure blinding factor
    public BigInteger generateSecureBlindingFactor() {
        BigInteger r;
        int maxAttempts = 100;
        int attempts = 0;
        
        do {
            // Generate random number in range [2, n-1]
            r = new BigInteger(n.bitLength() - 1, random);
            r = r.add(BigInteger.ONE); // Ensure r >= 1
            attempts++;
            
            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate suitable blinding factor after " + maxAttempts + " attempts");
            }
            
        } while (r.compareTo(BigInteger.ONE) <= 0 || 
                 r.compareTo(n) >= 0 || 
                 !r.gcd(n).equals(BigInteger.ONE));
        
        return r;
    }

    // Verify signature
    public boolean verify(BigInteger message, BigInteger signature) {
        try {
            BigInteger verified = signature.modPow(e, n);
            return verified.equals(message);
        } catch (Exception e) {
            return false;
        }
    }

    // Generate RSA keys (for demo - in production, use proper key generation)
    public static BlindSignatureUtil generateKeys(int bitLength) {
        SecureRandom random = new SecureRandom();
        
        // Generate two large prime numbers
        BigInteger p = BigInteger.probablePrime(bitLength / 2, random);
        BigInteger q = BigInteger.probablePrime(bitLength / 2, random);
        
        // Ensure p and q are distinct
        while (p.equals(q)) {
            q = BigInteger.probablePrime(bitLength / 2, random);
        }
        
        BigInteger n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        BigInteger e = new BigInteger("65537"); // Common public exponent
        
        // Ensure e and phi are coprime
        while (!e.gcd(phi).equals(BigInteger.ONE)) {
            e = e.add(BigInteger.ONE);
        }
        
        BigInteger d = e.modInverse(phi);
        
        return new BlindSignatureUtil(n, d, e);
    }

    // Getters for key information
    public BigInteger getModulus() { return n; }
    public BigInteger getPublicExponent() { return e; }
}