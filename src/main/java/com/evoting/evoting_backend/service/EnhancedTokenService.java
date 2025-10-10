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

@Service
public class EnhancedTokenService {
    
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
    
    /**
     * ✅ NEW: Issue completely anonymous token without voter identity
     */
    @Retryable(value = {PessimisticLockingFailureException.class}, maxAttempts = 3)
    @Transactional
    public AnonymousVotingToken issueAnonymousToken(String sessionId, Long electionId, 
                                                   BigInteger unblindedSignature, 
                                                   String blindingFactor) {
        try {
            // Check if session already has a token (prevent duplicates)
            if (tokenRepository.findBySessionAndElection(sessionId, electionId).isPresent()) {
                auditService.logEvent("DUPLICATE_TOKEN_SESSION", "EnhancedTokenService",
                    "issueAnonymousToken", "session:" + sessionId + ", election:" + electionId);
                throw new RuntimeException("Token already issued for this session");
            }
            
            // Create hash of blinding factor for audit (not for linking)
            String blindingFactorHash = hashData(blindingFactor);
            
            // Create and save completely anonymous token
            AnonymousVotingToken token = new AnonymousVotingToken(electionId, sessionId, blindingFactorHash);
            token.setBlindSignature(unblindedSignature.toString());
            
            AnonymousVotingToken savedToken = tokenRepository.save(token);
            
            // Record metrics
            monitoringService.recordTokenIssuance();
            
            // Audit without voter identity
            auditService.logEvent("ANONYMOUS_TOKEN_ISSUED", "EnhancedTokenService",
                "issueAnonymousToken", "session:" + sessionId + ", election:" + electionId + 
                ", token:" + maskToken(savedToken.getToken()));
            
            System.out.println("✅ TRULY ANONYMOUS token issued: " + maskToken(savedToken.getToken()) + 
                " for election: " + electionId);
            
            return savedToken;
            
        } catch (Exception e) {
            auditService.logEvent("ANONYMOUS_TOKEN_ISSUE_FAILED", "EnhancedTokenService",
                "issueAnonymousToken", "session:" + sessionId + ", error:" + e.getMessage());
            throw new RuntimeException("Anonymous token issuance failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * ✅ UPDATED: Validate and consume token without voter identity
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
    
    public java.util.List<AnonymousVotingToken> getActiveTokensForElection(Long electionId) {
        return tokenRepository.findUnspentTokensByElection(electionId);
    }
    
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