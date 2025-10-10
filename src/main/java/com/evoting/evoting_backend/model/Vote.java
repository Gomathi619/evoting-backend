package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vote")
public class Vote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "encrypted_vote", columnDefinition = "TEXT") // ✅ FIXED: Changed to TEXT
    private String encryptedVote;
    
    @Column(name = "election_id")
    private Long electionId;
    
    @Column(name = "candidate_id")
    private Long candidateId;
    
    @Column(name = "tracking_code")
    private String trackingCode;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    // Constructors
    public Vote() {
        this.timestamp = LocalDateTime.now();
        this.trackingCode = generateTrackingCode();
    }
    
    public Vote(String encryptedVote, Long electionId, Long candidateId) {
        this();
        this.encryptedVote = encryptedVote;
        this.electionId = electionId;
        this.candidateId = candidateId;
    }
    
    private String generateTrackingCode() {
        return "VOTE_" + System.currentTimeMillis() + "_" + 
               java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEncryptedVote() { return encryptedVote; }
    public void setEncryptedVote(String encryptedVote) { this.encryptedVote = encryptedVote; }
    
    public Long getElectionId() { return electionId; }
    public void setElectionId(Long electionId) { this.electionId = electionId; }
    
    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
    
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}