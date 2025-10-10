package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voter_identity")
public class VoterIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String governmentIdHash;

    private String name;
    private String email;
    private String phone;
    
    @Enumerated(EnumType.STRING)
    private IdentityStatus status;

    private boolean isAlive;
    private boolean isDuplicate;
    
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;

    public VoterIdentity() {
        this.createdAt = LocalDateTime.now();
        this.status = IdentityStatus.PENDING;
        this.isAlive = true;
        this.isDuplicate = false;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGovernmentIdHash() { return governmentIdHash; }
    public void setGovernmentIdHash(String governmentIdHash) { this.governmentIdHash = governmentIdHash; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}