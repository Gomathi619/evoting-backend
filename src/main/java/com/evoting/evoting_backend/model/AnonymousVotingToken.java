package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "anonymous_voting_tokens")
public class AnonymousVotingToken {
    @Id
    private String token;

    @Column(name = "election_id", nullable = false)
    private Long electionId;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "blinding_factor_hash")
    private String blindingFactorHash;
    
    @Column(name = "spent", nullable = false)
    private boolean spent = false;
    
    @Column(name = "active", nullable = false)
    private boolean active = true;
    
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "spent_at")
    private LocalDateTime spentAt;

    @Column(name = "blind_signature", columnDefinition = "TEXT")
    private String blindSignature;

    // ✅ ADDED: Default constructor for JPA
    public AnonymousVotingToken() {
        this.issuedAt = LocalDateTime.now();
        this.expiresAt = issuedAt.plusDays(7);
    }

    // Parameterized constructor
    public AnonymousVotingToken(Long electionId, String sessionId, String blindingFactorHash) {
        this();
        this.token = generateSecureToken();
        this.electionId = electionId;
        this.sessionId = sessionId;
        this.blindingFactorHash = blindingFactorHash;
    }

    private String generateSecureToken() {
        return "AT_" + UUID.randomUUID().toString().replace("-", "") + 
               "_" + System.currentTimeMillis();
    }

    // Getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getElectionId() { return electionId; }
    public void setElectionId(Long electionId) { this.electionId = electionId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getBlindingFactorHash() { return blindingFactorHash; }
    public void setBlindingFactorHash(String blindingFactorHash) { this.blindingFactorHash = blindingFactorHash; }
    public boolean isSpent() { return spent; }
    public void setSpent(boolean spent) { this.spent = spent; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getSpentAt() { return spentAt; }
    public void setSpentAt(LocalDateTime spentAt) { this.spentAt = spentAt; }
    public String getBlindSignature() { return blindSignature; }
    public void setBlindSignature(String blindSignature) { this.blindSignature = blindSignature; }

    public boolean isValid() {
        return active && !spent && LocalDateTime.now().isBefore(expiresAt);
    }
}