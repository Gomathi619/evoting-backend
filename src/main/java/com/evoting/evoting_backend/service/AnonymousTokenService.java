package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.AnonymousVotingToken;
import com.evoting.evoting_backend.repository.AnonymousVotingTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class AnonymousTokenService {
    
    @Autowired
    private AnonymousVotingTokenRepository tokenRepository;
    
    @Autowired
    private BlindSignatureService blindSignatureService;
    
    @Autowired
    private AtomicTokenService atomicTokenService;
    
    @Autowired
    private ImmutableAuditService auditService;
    
    @Autowired
    private MonitoringService monitoringService;
    
    // Inner class for blind sign session response
    public static class BlindSignSessionResponse {
        private String sessionId;
        private BigInteger blindingFactor;
        
        public BlindSignSessionResponse(String sessionId, BigInteger blindingFactor) {
            this.sessionId = sessionId;
            this.blindingFactor = blindingFactor;
        }
        
        public String getSessionId() { return sessionId; }
        public BigInteger getBlindingFactor() { return blindingFactor; }
    }
    
    /**
     * ✅ FIXED: Start blind signing session without voter identity
     */
    public BlindSignSessionResponse startBlindSignSession(Long electionId) {
        // Create blind signing session using the service
        String sessionId = UUID.randomUUID().toString();
        
        // Generate secure blinding factor
        BigInteger blindingFactor = blindSignatureService.getBlindSignatureUtil().generateSecureBlindingFactor();
        
        return new BlindSignSessionResponse(sessionId, blindingFactor);
    }
    
    /**
     * ✅ FIXED: Sign blinded message using HSM-backed service
     */
    public BigInteger signBlindedMessage(BigInteger blindedMessage, String sessionId) {
        return blindSignatureService.signBlindedMessage(blindedMessage, sessionId);
    }
    
    /**
     * ✅ FIXED: Issue anonymous token with enhanced security - NO VOTER IDENTITY
     */
    @Retryable(value = {PessimisticLockingFailureException.class}, maxAttempts = 3)
    @Transactional
    public AnonymousVotingToken issueAnonymousToken(String sessionId, Long electionId, 
                                                   BigInteger unblindedSignature, 
                                                   String blindingFactor) {
        try {
            // Verify the session hasn't already received a token for this election
            if (tokenRepository.existsBySessionAndElection(sessionId, electionId)) {
                auditService.logEvent("DUPLICATE_TOKEN_SESSION", "AnonymousTokenService",
                    "issueAnonymousToken", "session:" + sessionId + ", election:" + electionId);
                throw new RuntimeException("Token already issued for this session");
            }
            
            // Create hash of blinding factor for audit (not for linking)
            String blindingFactorHash = hashData(blindingFactor);
            
            // ✅ FIXED: Use correct constructor
            AnonymousVotingToken token = new AnonymousVotingToken(electionId, sessionId, blindingFactorHash);
            token.setBlindSignature(unblindedSignature.toString());
            
            AnonymousVotingToken savedToken = tokenRepository.save(token);
            
            // Record metrics
            monitoringService.recordTokenIssuance();
            
            // Audit without voter identity
            auditService.logEvent("ANONYMOUS_TOKEN_ISSUED", "AnonymousTokenService",
                "issueAnonymousToken", "session:" + sessionId + ", election:" + electionId + 
                ", token:" + maskToken(savedToken.getToken()));
            
            System.out.println("✅ TRULY ANONYMOUS token issued: " + maskToken(savedToken.getToken()) + 
                " for election: " + electionId);
            
            return savedToken;
            
        } catch (Exception e) {
            auditService.logEvent("ANONYMOUS_TOKEN_ISSUE_FAILED", "AnonymousTokenService",
                "issueAnonymousToken", "session:" + sessionId + ", error:" + e.getMessage());
            throw new RuntimeException("Anonymous token issuance failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * ✅ FIXED: Validate and consume token without voter identity
     */
    public boolean validateAndConsumeToken(String token, Long electionId) {
        return atomicTokenService.consumeTokenSafely(token, electionId, 
            "vote_cast_" + System.currentTimeMillis());
    }
    
    public boolean validateTokenQuick(String token, Long electionId) {
        return atomicTokenService.validateTokenWithoutConsumption(token, electionId);
    }
    
    private String hashData(String data) {
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
            return "hash_error";
        }
    }
    
    public String maskToken(String token) {
        if (token == null || token.length() < 8) return "***";
        return token.substring(0, 6) + "***" + token.substring(token.length() - 6);
    }
    
    // Getter for blind signature service
    public BlindSignatureService getBlindSignatureService() {
        return blindSignatureService;
    }
    
    /**
     * ✅ FIXED: Get active tokens for election monitoring
     */
    public java.util.List<AnonymousVotingToken> getActiveTokensForElection(Long electionId) {
        return tokenRepository.findUnspentTokensByElection(electionId);
    }
    
    /**
     * ✅ FIXED: Get token statistics for monitoring
     */
    public java.util.Map<String, Object> getTokenStatistics(Long electionId) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        long totalTokens = tokenRepository.countTokensByElection(electionId);
        long spentTokens = tokenRepository.countSpentTokensByElection(electionId);
        
        stats.put("electionId", electionId);
        stats.put("totalTokens", totalTokens);
        stats.put("spentTokens", spentTokens);
        stats.put("availableTokens", totalTokens - spentTokens);
        stats.put("utilizationRate", totalTokens > 0 ? (spentTokens * 100.0 / totalTokens) : 0);
        stats.put("timestamp", System.currentTimeMillis());
        
        return stats;
    }
}