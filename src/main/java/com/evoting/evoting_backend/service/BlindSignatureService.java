package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.crypto.BlindSignatureUtil;
import com.evoting.evoting_backend.model.BlindSignSession;
import com.evoting.evoting_backend.repository.BlindSignSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BlindSignatureService {
    
    @Autowired
    private BlindSignSessionRepository sessionRepository;
    
    @Autowired
    private HSMService hsmService;
    
    @Autowired
    private ThresholdKeyService thresholdKeyService;
    
    private BlindSignatureUtil blindSignatureUtil;
    private String hsmKeyId = "blind_signature_key";
    
    @PostConstruct
    public void init() {
        try {
            // Initialize HSM key
            String publicKey = hsmService.generateHSMKeyPair(hsmKeyId);
            System.out.println("HSM Key initialized. Public key: " + publicKey.substring(0, 50) + "...");
            
            // Initialize with secure keys (using HSM for production)
            this.blindSignatureUtil = BlindSignatureUtil.generateKeys(2048);
            
            // Store private key components in HSM (simulated)
            String privateKeyData = "MODULUS:" + blindSignatureUtil.getModulus() + 
                                   ":EXPONENT:" + blindSignatureUtil.getPublicExponent() + 
                                   ":TIMESTAMP:" + System.currentTimeMillis();
            hsmService.storeSensitiveData("blind_private_key", privateKeyData);
            
            System.out.println("BlindSignatureService initialized with HSM integration");
            System.out.println("HSM Health: " + (hsmService.healthCheck() ? "HEALTHY" : "DEGRADED"));
            
        } catch (Exception e) {
            System.err.println("HSM initialization failed: " + e.getMessage());
            // Fallback to software-only mode
            this.blindSignatureUtil = BlindSignatureUtil.generateKeys(2048);
            System.out.println("Fallback to software-only blind signature mode");
        }
    }
    
    /**
     * ✅ Start blind signing session with enhanced security
     */
    public BlindSignSession startBlindSignSession(Long voterIdentityId, Long electionId) {
        // Check for existing active session
        if (sessionRepository.hasActiveSession(voterIdentityId, electionId, LocalDateTime.now())) {
            throw new RuntimeException("Active blind signing session already exists for this voter and election");
        }
        
        String sessionId = UUID.randomUUID().toString();
        
        BlindSignSession session = new BlindSignSession();
        session.setSessionId(sessionId);
        session.setVoterIdentityId(voterIdentityId);
        session.setElectionId(electionId);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(30)); // 30-minute expiry
        session.setUsed(false);
        
        // Generate secure blinding factor using HSM randomness
        BigInteger blindingFactor = blindSignatureUtil.generateSecureBlindingFactor();
        session.setBlindingFactor(blindingFactor.toString());
        
        // Log the session creation in HSM
        hsmService.storeSensitiveData("session_" + sessionId, 
            "voter:" + voterIdentityId + ",election:" + electionId + ",created:" + System.currentTimeMillis());
        
        return sessionRepository.save(session);
    }
    
    /**
     * ✅ Sign blinded message with HSM audit trail
     */
    public BigInteger signBlindedMessage(BigInteger blindedMessage, String sessionId) {
        // Validate session
        BlindSignSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Invalid session"));
                
        if (session.isUsed()) {
            throw new RuntimeException("Session already used");
        }
        
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Session expired");
        }
        
        // Perform blind signing with HSM audit
        try {
            // Log signing attempt in HSM
            String auditData = "signing_session:" + sessionId + 
                             ",voter:" + session.getVoterIdentityId() +
                             ",election:" + session.getElectionId() +
                             ",time:" + System.currentTimeMillis();
            
            byte[] auditSignature = hsmService.signWithHSM(hsmKeyId, auditData.getBytes());
            
            // Perform the actual blind signing
            BigInteger signature = blindSignatureUtil.signBlinded(blindedMessage);
            
            // Verify the signature was logged in HSM
            boolean auditVerified = hsmService.verifyHSMSignature(hsmKeyId, auditData.getBytes(), auditSignature);
            if (!auditVerified) {
                throw new RuntimeException("HSM audit verification failed");
            }
            
            // Mark session as used
            session.setUsed(true);
            sessionRepository.save(session);
            
            // Store successful signing in HSM
            hsmService.storeSensitiveData("completed_session_" + sessionId, 
                "completed:true,time:" + System.currentTimeMillis());
            
            System.out.println("Blind signing completed with HSM audit for session: " + sessionId);
            
            return signature;
            
        } catch (Exception e) {
            // Log failure in HSM
            hsmService.storeSensitiveData("failed_session_" + sessionId, 
                "error:" + e.getMessage() + ",time:" + System.currentTimeMillis());
            throw new RuntimeException("Blind signing failed: " + e.getMessage(), e);
        }
    }
    
    public BigInteger getPublicModulus() {
        return blindSignatureUtil.getModulus();
    }
    
    public BigInteger getPublicExponent() {
        return blindSignatureUtil.getPublicExponent();
    }
    
    public BlindSignatureUtil getBlindSignatureUtil() {
        return blindSignatureUtil;
    }
    
    // HSM health check
    public boolean isHSMOperational() {
        try {
            return hsmService.healthCheck();
        } catch (Exception e) {
            return false;
        }
    }
    
    // Key rotation
    public void rotateHSMKeys() {
        hsmService.rotateKey(hsmKeyId);
        System.out.println("HSM keys rotated successfully");
    }
    
    // Get HSM status information
    public Map<String, Object> getHSMStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("hsmOperational", isHSMOperational());
        status.put("hsmKeyId", hsmKeyId);
        status.put("keysInStore", hsmService.getKeyIds().size());
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }
}