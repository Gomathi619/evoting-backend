package com.evoting.evoting_backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class EligibilityToken {
    @Id
    private String token;

    private Long voterId;
    private Long electionId;
    private boolean spent;

    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;

    public EligibilityToken() {}

    public EligibilityToken(Long voterId, Long electionId) {
        this.token = UUID.randomUUID().toString();
        this.voterId = voterId;
        this.electionId = electionId;
        this.spent = false;
        this.issuedAt = LocalDateTime.now();
        this.expiresAt = issuedAt.plusHours(1);
    }

    public EligibilityToken(String token, Long voterId, Long electionId, boolean spent, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this.token = token;
        this.voterId = voterId;
        this.electionId = electionId;
        this.spent = spent;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getVoterId() { return voterId; }
    public void setVoterId(Long voterId) { this.voterId = voterId; }

    public Long getElectionId() { return electionId; }
    public void setElectionId(Long electionId) { this.electionId = electionId; }

    public boolean isSpent() { return spent; }
    public void setSpent(boolean spent) { this.spent = spent; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isValid() {
        return !spent && LocalDateTime.now().isBefore(expiresAt);
    }
}