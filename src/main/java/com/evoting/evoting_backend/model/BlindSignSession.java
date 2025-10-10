package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blind_sign_sessions")
public class BlindSignSession {
    @Id
    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "voter_identity_id", nullable = false)
    private Long voterIdentityId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "blinding_factor", columnDefinition = "TEXT", nullable = false)
    private String blindingFactor;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used")
    private boolean used = false;

    // Constructors
    public BlindSignSession() {}

    public BlindSignSession(String sessionId, Long voterIdentityId, Long electionId, String blindingFactor, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.voterIdentityId = voterIdentityId;
        this.electionId = electionId;
        this.blindingFactor = blindingFactor;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getVoterIdentityId() { return voterIdentityId; }
    public void setVoterIdentityId(Long voterIdentityId) { this.voterIdentityId = voterIdentityId; }

    public Long getElectionId() { return electionId; }
    public void setElectionId(Long electionId) { this.electionId = electionId; }

    public String getBlindingFactor() { return blindingFactor; }
    public void setBlindingFactor(String blindingFactor) { this.blindingFactor = blindingFactor; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }
}