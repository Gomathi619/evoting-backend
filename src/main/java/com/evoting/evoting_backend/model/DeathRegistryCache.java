package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "death_registry_cache")
public class DeathRegistryCache {
    @Id
    @Column(name = "government_id_hash")
    private String governmentIdHash;

    @Column(name = "is_alive", nullable = false)
    private boolean isAlive;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Constructors
    public DeathRegistryCache() {}

    public DeathRegistryCache(String governmentIdHash, boolean isAlive, LocalDateTime expiresAt) {
        this.governmentIdHash = governmentIdHash;
        this.isAlive = isAlive;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public String getGovernmentIdHash() { return governmentIdHash; }
    public void setGovernmentIdHash(String governmentIdHash) { this.governmentIdHash = governmentIdHash; }

    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { isAlive = alive; }

    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isValid() {
        return LocalDateTime.now().isBefore(expiresAt);
    }
}