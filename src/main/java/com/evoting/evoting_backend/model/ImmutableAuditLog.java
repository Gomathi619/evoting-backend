package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "immutable_audit_log")
public class ImmutableAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String logHash;
    
    private String eventType;
    private String service;
    private String operation;
    
    @Column(columnDefinition = "TEXT")
    private String eventData;
    
    private String previousHash;
    
    @Column(columnDefinition = "TEXT")  // ✅ ADD THIS LINE - FIX FOR SIGNATURE COLUMN
    private String signature;
    
    private LocalDateTime timestamp;
    
    @Column(columnDefinition = "TEXT")  // ✅ RECOMMENDED FOR MERKLE ROOT TOO
    private String merkleRoot;

    // Constructors
    public ImmutableAuditLog() {}

    public ImmutableAuditLog(String logHash, String eventType, String service, String operation, 
                           String eventData, String previousHash, String signature, 
                           LocalDateTime timestamp, String merkleRoot) {
        this.logHash = logHash;
        this.eventType = eventType;
        this.service = service;
        this.operation = operation;
        this.eventData = eventData;
        this.previousHash = previousHash;
        this.signature = signature;
        this.timestamp = timestamp;
        this.merkleRoot = merkleRoot;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLogHash() { return logHash; }
    public void setLogHash(String logHash) { this.logHash = logHash; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getMerkleRoot() { return merkleRoot; }
    public void setMerkleRoot(String merkleRoot) { this.merkleRoot = merkleRoot; }

    // Helper methods
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "ImmutableAuditLog{" +
                "id=" + id +
                ", logHash='" + logHash + '\'' +
                ", eventType='" + eventType + '\'' +
                ", service='" + service + '\'' +
                ", operation='" + operation + '\'' +
                ", eventData='" + eventData + '\'' +
                ", previousHash='" + previousHash + '\'' +
                ", signature='" + (signature != null ? signature.substring(0, Math.min(50, signature.length())) + "..." : "null") + '\'' +
                ", timestamp=" + timestamp +
                ", merkleRoot='" + merkleRoot + '\'' +
                '}';
    }
}