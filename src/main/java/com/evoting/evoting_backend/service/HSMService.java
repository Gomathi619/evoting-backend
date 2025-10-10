package com.evoting.evoting_backend.service;

import org.springframework.stereotype.Service;

import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jakarta.annotation.PostConstruct;

@Service
public class HSMService {
    
    private final Map<String, KeyPair> keyStore = new HashMap<>();
    private final Map<String, String> dataStore = new HashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    
    @PostConstruct
    public void initializeDefaultKeys() {
        try {
            System.out.println("🔐 Initializing HSM Keys...");
            
            // Initialize audit_log_key for ImmutableAuditService
            generateHSMKeyPair("audit_log_key");
            System.out.println("✅ audit_log_key initialized");
            
            // Initialize blind_signature_key for BlindSignatureService
            generateHSMKeyPair("blind_signature_key");
            System.out.println("✅ blind_signature_key initialized");
            
            // Initialize any other required keys
            generateHSMKeyPair("hsm_health_key");
            System.out.println("✅ hsm_health_key initialized");
            
            System.out.println("🎉 HSM Keys initialization completed successfully");
            System.out.println("📋 Available keys: " + getKeyIds());
            
        } catch (Exception e) {
            System.err.println("❌ HSM Key initialization failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("HSM initialization failed", e);
        }
    }
    
    public String generateHSMKeyPair(String keyId) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048, secureRandom);
            KeyPair keyPair = keyGen.generateKeyPair();
            
            keyStore.put(keyId, keyPair);
            
            // Return public key for distribution
            return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            
        } catch (Exception e) {
            throw new RuntimeException("HSM key generation failed: " + e.getMessage(), e);
        }
    }
    
    public byte[] signWithHSM(String keyId, byte[] data) {
        try {
            // Auto-create key if it doesn't exist (safety net)
            if (!keyStore.containsKey(keyId)) {
                System.out.println("⚠️ Key not found: " + keyId + " - Auto-generating...");
                generateHSMKeyPair(keyId);
            }
            
            KeyPair keyPair = keyStore.get(keyId);
            if (keyPair == null) {
                throw new RuntimeException("HSM key not found: " + keyId);
            }
            
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keyPair.getPrivate(), secureRandom);
            signature.update(data);
            
            return signature.sign();
            
        } catch (Exception e) {
            throw new RuntimeException("HSM signing failed: " + e.getMessage(), e);
        }
    }
    
    public boolean verifyHSMSignature(String keyId, byte[] data, byte[] signatureBytes) {
        try {
            KeyPair keyPair = keyStore.get(keyId);
            if (keyPair == null) {
                throw new RuntimeException("HSM key not found: " + keyId);
            }
            
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(keyPair.getPublic());
            signature.update(data);
            
            return signature.verify(signatureBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("HSM verification failed: " + e.getMessage(), e);
        }
    }
    
    public String getPublicKey(String keyId) {
        try {
            KeyPair keyPair = keyStore.get(keyId);
            if (keyPair == null) {
                throw new RuntimeException("HSM key not found: " + keyId);
            }
            return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get public key: " + e.getMessage(), e);
        }
    }
    
    public void storeSensitiveData(String key, String data) {
        try {
            String encrypted = simpleEncrypt(data);
            dataStore.put("data_" + key, encrypted);
        } catch (Exception e) {
            throw new RuntimeException("HSM data storage failed: " + e.getMessage(), e);
        }
    }
    
    public String retrieveSensitiveData(String key) {
        try {
            String encrypted = dataStore.get("data_" + key);
            if (encrypted == null) return null;
            return simpleDecrypt(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("HSM data retrieval failed: " + e.getMessage(), e);
        }
    }
    
    private String simpleEncrypt(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes());
    }
    
    private String simpleDecrypt(String data) {
        return new String(Base64.getDecoder().decode(data));
    }
    
    public void rotateKey(String keyId) {
        generateHSMKeyPair(keyId);
        System.out.println("🔄 HSM key rotated: " + keyId);
    }
    
    public Set<String> getKeyIds() {
        return keyStore.keySet();
    }
    
    public boolean healthCheck() {
        try {
            String testKey = "hsm_health_key";
            
            // Ensure test key exists
            if (!keyStore.containsKey(testKey)) {
                generateHSMKeyPair(testKey);
            }
            
            String testData = "HSM Health Check " + System.currentTimeMillis();
            byte[] signature = signWithHSM(testKey, testData.getBytes());
            boolean verified = verifyHSMSignature(testKey, testData.getBytes(), signature);
            
            return verified;
        } catch (Exception e) {
            System.err.println("❌ HSM Health check failed: " + e.getMessage());
            return false;
        }
    }
    
    public boolean keyExists(String keyId) {
        return keyStore.containsKey(keyId);
    }
    
    public int getKeyCount() {
        return keyStore.size();
    }
}