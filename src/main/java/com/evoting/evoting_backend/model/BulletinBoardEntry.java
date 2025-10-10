package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulletin_board_entry")
public class BulletinBoardEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entryHash;
    private String previousHash;
    private String trackingCode;
    private Long electionId;
    
    @Column(columnDefinition = "TEXT") // ✅ ADD THIS LINE
    private String encryptedVote;
    
    private LocalDateTime timestamp;

    public BulletinBoardEntry() {}

    public BulletinBoardEntry(Long id, String entryHash, String previousHash, String trackingCode, Long electionId, String encryptedVote, LocalDateTime timestamp) {
        this.id = id;
        this.entryHash = entryHash;
        this.previousHash = previousHash;
        this.trackingCode = trackingCode;
        this.electionId = electionId;
        this.encryptedVote = encryptedVote;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntryHash() { return entryHash; }
    public void setEntryHash(String entryHash) { this.entryHash = entryHash; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

    public Long getElectionId() { return electionId; }
    public void setElectionId(Long electionId) { this.electionId = electionId; }

    public String getEncryptedVote() { return encryptedVote; }
    public void setEncryptedVote(String encryptedVote) { this.encryptedVote = encryptedVote; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}