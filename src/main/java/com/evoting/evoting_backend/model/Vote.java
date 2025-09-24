package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String encryptedVote;
    private String trackingCode;
    private LocalDateTime timestamp;
    private Long electionId;
    private Long candidateId;

    public Vote() {}

    public Vote(Long id, String encryptedVote, String trackingCode, LocalDateTime timestamp, Long electionId, Long candidateId) {
        this.id = id;
        this.encryptedVote = encryptedVote;
        this.trackingCode = trackingCode;
        this.timestamp = timestamp;
        this.electionId = electionId;
        this.candidateId = candidateId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEncryptedVote() { return encryptedVote; }
    public void setEncryptedVote(String encryptedVote) { this.encryptedVote = encryptedVote; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getElectionId() { return electionId; }
    public void setElectionId(Long electionId) { this.electionId = electionId; }
    
    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
}