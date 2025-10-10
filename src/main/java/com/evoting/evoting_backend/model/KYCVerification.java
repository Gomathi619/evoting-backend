package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_verification")
public class KYCVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "government_id_hash", nullable = false, unique = true)
    private String governmentIdHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;
    private String phone;

    @Enumerated(EnumType.STRING)
    private IdentityStatus status;

    @Column(name = "is_alive")
    private boolean isAlive = true;

    @Column(name = "is_duplicate")
    private boolean isDuplicate = false;

    @Column(name = "death_registry_checked")
    private boolean deathRegistryChecked = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public KYCVerification() {}

    public KYCVerification(String governmentIdHash, String fullName, String email, String phone) {
        this.governmentIdHash = governmentIdHash;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.status = IdentityStatus.PENDING;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGovernmentIdHash() { return governmentIdHash; }
    public void setGovernmentIdHash(String governmentIdHash) { this.governmentIdHash = governmentIdHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public IdentityStatus getStatus() { return status; }
    public void setStatus(IdentityStatus status) { this.status = status; }

    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { isAlive = alive; }

    public boolean isDuplicate() { return isDuplicate; }
    public void setDuplicate(boolean duplicate) { isDuplicate = duplicate; }

    public boolean isDeathRegistryChecked() { return deathRegistryChecked; }
    public void setDeathRegistryChecked(boolean deathRegistryChecked) { this.deathRegistryChecked = deathRegistryChecked; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}