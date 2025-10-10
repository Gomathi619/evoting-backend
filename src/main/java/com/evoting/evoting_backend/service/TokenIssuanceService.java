package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.AnonymousVotingToken;
import com.evoting.evoting_backend.repository.AnonymousVotingTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TokenIssuanceService {
    
    @Autowired
    private AnonymousVotingTokenRepository tokenRepository;
    
    @Autowired
    private AtomicTokenService atomicTokenService;
    
    @Autowired
    private MonitoringService monitoringService;
    
    @Autowired
    private ImmutableAuditService auditService;

    /**
     * ✅ UPDATED: Legacy method for backward compatibility
     * Uses session-based tokens instead of voter identity
     */
    public AnonymousVotingToken issueVotingToken(String sessionId, Long electionId) {
        try {
            // Check if session already has active token for this election
            if (tokenRepository.hasActiveUnspentToken(sessionId, electionId)) {
                auditService.logEvent("DUPLICATE_TOKEN_SESSION_ATTEMPT", "TokenIssuanceService",
                    "issueVotingToken", "session:" + sessionId + ", election:" + electionId);
                throw new RuntimeException("Active voting token already exists for this session");
            }
            
            // Create new anonymous voting token with session ID
            AnonymousVotingToken token = new AnonymousVotingToken();
            token.setToken(generateSecureToken());
            token.setElectionId(electionId);
            token.setSessionId(sessionId);
            token.setBlindingFactorHash("legacy_session_" + sessionId); // For audit
            token.setSpent(false);
            token.setActive(true);
            token.setIssuedAt(java.time.LocalDateTime.now());
            token.setExpiresAt(java.time.LocalDateTime.now().plusDays(7));
            
            // Generate blind signature (simplified - implement proper crypto in production)
            token.setBlindSignature(generateBlindSignature(token.getToken()));
            
            AnonymousVotingToken savedToken = tokenRepository.save(token);
            
            // Record metrics
            monitoringService.recordTokenIssuance();
            
            // ✅ FIXED: Now using the public method
            auditService.logEvent("TOKEN_ISSUED_LEGACY", "TokenIssuanceService",
                "issueVotingToken", "token:" + atomicTokenService.maskToken(savedToken.getToken()) +
                ", session:" + sessionId + ", election:" + electionId);
            
            return savedToken;
            
        } catch (Exception e) {
            monitoringService.recordSecurityEvent("token_issuance_failed");
            auditService.logEvent("TOKEN_ISSUANCE_FAILED_LEGACY", "TokenIssuanceService",
                "issueVotingToken", "session:" + sessionId +
                ", error:" + e.getMessage());
            throw new RuntimeException("Token issuance failed: " + e.getMessage());
        }
    }
    
    /**
     * ✅ ENHANCED: Uses atomic token consumption
     */
    public boolean validateAndConsumeToken(String token, Long electionId) {
        String clientInfo = getClientInfo(); // Extract from security context
        
        boolean consumed = atomicTokenService.consumeTokenSafely(token, electionId, clientInfo);
        
        if (consumed) {
            monitoringService.recordSecurityEvent("token_consumed_validation");
        } else {
            monitoringService.recordSecurityEvent("token_validation_failed");
        }
        
        return consumed;
    }
    
    /**
     * ✅ Quick validation without consumption (for pre-check)
     */
    public boolean validateTokenQuick(String token, Long electionId) {
        return atomicTokenService.validateTokenWithoutConsumption(token, electionId);
    }
    
    public List<AnonymousVotingToken> getActiveTokensForElection(Long electionId) {
        return tokenRepository.findUnspentTokensByElection(electionId);
    }
    
    public long getTokenStatistics(Long electionId) {
        long totalTokens = tokenRepository.countTokensByElection(electionId);
        long spentTokens = tokenRepository.countSpentTokensByElection(electionId);
        return totalTokens - spentTokens;
    }
    
    // Simplified crypto - replace with proper blind signatures in production
    private String generateBlindSignature(String token) {
        return "blind_sig_" + UUID.randomUUID().toString() + "_" + token.hashCode();
    }
    
    private String generateSecureToken() {
        return "TKN_" + UUID.randomUUID().toString().replace("-", "") + 
               "_" + System.currentTimeMillis();
    }
    
    private String getClientInfo() {
        try {
            // Extract from Spring Security context
            org.springframework.security.core.context.SecurityContext context = 
                org.springframework.security.core.context.SecurityContextHolder.getContext();
            if (context != null && context.getAuthentication() != null) {
                return context.getAuthentication().getName() + "@" + 
                       java.time.LocalDateTime.now().toString();
            }
        } catch (Exception e) {
            // Ignore - use default
        }
        return "unknown@" + java.time.LocalDateTime.now().toString();
    }
}