package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.AnonymousVotingToken;
import com.evoting.evoting_backend.repository.AnonymousVotingTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AtomicTokenService {
    
    @Autowired
    private AnonymousVotingTokenRepository tokenRepository;
    
    @Autowired
    private ImmutableAuditService auditService;
    
    @Autowired
    private MonitoringService monitoringService;
    
    // In-memory lock for distributed coordination (supplements DB locking)
    private final ConcurrentHashMap<String, ReentrantLock> tokenLocks = new ConcurrentHashMap<>();
    
    /**
     * ✅ ATOMIC Token Consumption with Multiple Safety Layers
     * Prevents double-spending even under high concurrency
     */
    @Retryable(value = {PessimisticLockingFailureException.class}, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 100))
    @Transactional
    public boolean consumeTokenSafely(String token, Long electionId, String clientInfo) {
        ReentrantLock inMemoryLock = null;
        boolean tokenConsumed = false;
        
        try {
            // Layer 1: In-memory lock for quick coordination
            inMemoryLock = tokenLocks.computeIfAbsent(token, k -> new ReentrantLock());
            if (!inMemoryLock.tryLock(2, TimeUnit.SECONDS)) {
                monitoringService.recordSecurityEvent("token_lock_timeout");
                auditService.logEvent("TOKEN_LOCK_TIMEOUT", "AtomicTokenService", 
                    "consumeTokenSafely", "token:" + maskToken(token) + ", client:" + clientInfo);
                return false;
            }
            
            // Layer 2: Database pessimistic locking
            AnonymousVotingToken tokenEntity = tokenRepository
                .findByTokenAndElectionIdWithLock(token, electionId)
                .orElse(null);
                
            if (tokenEntity == null) {
                monitoringService.recordSecurityEvent("token_not_found");
                auditService.logEvent("TOKEN_NOT_FOUND", "AtomicTokenService",
                    "consumeTokenSafely", "token:" + maskToken(token) + ", election:" + electionId);
                return false;
            }
            
            // Layer 3: Validation checks
            if (!tokenEntity.isActive()) {
                auditService.logEvent("TOKEN_INACTIVE", "AtomicTokenService",
                    "consumeTokenSafely", "token:" + maskToken(token) + ", status:inactive");
                return false;
            }
            
            if (tokenEntity.isSpent()) {
                monitoringService.recordSecurityEvent("token_already_spent");
                auditService.logEvent("TOKEN_ALREADY_SPENT", "AtomicTokenService",
                    "consumeTokenSafely", "token:" + maskToken(token) + ", client:" + clientInfo);
                return false;
            }
            
            if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
                auditService.logEvent("TOKEN_EXPIRED", "AtomicTokenService",
                    "consumeTokenSafely", "token:" + maskToken(token) + ", expired_at:" + tokenEntity.getExpiresAt());
                return false;
            }
            
            // Layer 4: Atomic update
            tokenEntity.setSpent(true);
            tokenEntity.setSpentAt(LocalDateTime.now());
            tokenRepository.save(tokenEntity);
            
            tokenConsumed = true;
            
            // Success audit
            auditService.logEvent("TOKEN_CONSUMED_ATOMIC", "AtomicTokenService",
                "consumeTokenSafely", "token:" + maskToken(token) + ", election:" + electionId + 
                ", client:" + clientInfo + ", success:true");
                
            monitoringService.recordSecurityEvent("token_consumed_success");
            
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            monitoringService.recordSecurityEvent("token_lock_interrupted");
            auditService.logEvent("TOKEN_LOCK_INTERRUPTED", "AtomicTokenService",
                "consumeTokenSafely", "token:" + maskToken(token) + ", error:" + e.getMessage());
            return false;
        } catch (PessimisticLockingFailureException e) {
            monitoringService.recordSecurityEvent("database_lock_failure");
            auditService.logEvent("DATABASE_LOCK_FAILURE", "AtomicTokenService",
                "consumeTokenSafely", "token:" + maskToken(token) + ", error:" + e.getMessage());
            throw e; // Will be retried
        } catch (Exception e) {
            monitoringService.recordSecurityEvent("token_consumption_error");
            auditService.logEvent("TOKEN_CONSUMPTION_ERROR", "AtomicTokenService",
                "consumeTokenSafely", "token:" + maskToken(token) + ", error:" + e.getMessage());
            return false;
        } finally {
            // Always release the in-memory lock
            if (inMemoryLock != null && inMemoryLock.isHeldByCurrentThread()) {
                inMemoryLock.unlock();
            }
        }
    }
    
    /**
     * ✅ UPDATED: Emergency token revocation without voter identity
     */
    @Transactional
    public boolean revokeToken(String token, Long electionId, String adminReason) {
        try {
            AnonymousVotingToken tokenEntity = tokenRepository
                .findByTokenAndElectionIdWithLock(token, electionId)
                .orElse(null);
                
            if (tokenEntity != null) {
                tokenEntity.setActive(false);
                tokenEntity.setSpent(true);
                tokenRepository.save(tokenEntity);
                
                // Clean up lock
                tokenLocks.remove(token);
                
                auditService.logEvent("TOKEN_REVOKED", "AtomicTokenService",
                    "revokeToken", "token:" + maskToken(token) + ", reason:" + adminReason + 
                    ", election:" + electionId + ", admin_action:true");
                    
                return true;
            }
            return false;
            
        } catch (Exception e) {
            auditService.logEvent("TOKEN_REVOCATION_ERROR", "AtomicTokenService",
                "revokeToken", "token:" + maskToken(token) + ", error:" + e.getMessage());
            return false;
        }
    }
    
    /**
     * ✅ Batch token validation for performance
     */
    @Transactional(readOnly = true)
    public boolean validateTokenWithoutConsumption(String token, Long electionId) {
        try {
            return tokenRepository.findByTokenAndElectionId(token, electionId)
                .map(t -> t.isActive() && !t.isSpent() && t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
        } catch (Exception e) {
            auditService.logEvent("TOKEN_VALIDATION_ERROR", "AtomicTokenService",
                "validateTokenWithoutConsumption", "token:" + maskToken(token) + ", error:" + e.getMessage());
            return false;
        }
    }
    
    /**
     * ✅ Clean up expired tokens (scheduled task)
     */
    @Transactional
    public int cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<AnonymousVotingToken> expiredTokens = tokenRepository.findExpiredTokens(now);
            
            for (AnonymousVotingToken token : expiredTokens) {
                token.setActive(false);
                tokenRepository.save(token);
                
                // Clean up in-memory locks
                tokenLocks.remove(token.getToken());
            }
            
            auditService.logEvent("TOKEN_CLEANUP", "AtomicTokenService",
                "cleanupExpiredTokens", "cleaned_count:" + expiredTokens.size() + ", timestamp:" + now);
                
            return expiredTokens.size();
            
        } catch (Exception e) {
            auditService.logEvent("TOKEN_CLEANUP_ERROR", "AtomicTokenService",
                "cleanupExpiredTokens", "error:" + e.getMessage());
            return 0;
        }
    }
    
    // ✅ FIXED: Changed to public so other services can use it
    public String maskToken(String token) {
        if (token == null || token.length() < 8) return "***";
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}