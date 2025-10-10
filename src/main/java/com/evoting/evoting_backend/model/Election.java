package com.evoting.evoting_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "elections")
public class Election {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ElectionState state;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "closed_at") 
    private LocalDateTime closedAt;

    // Additional election metadata
    @Column(name = "voting_start")
    private LocalDateTime votingStart;

    @Column(name = "voting_end")
    private LocalDateTime votingEnd;

    @Column(name = "max_candidates")
    private Integer maxCandidates;

    @Column(name = "allow_write_in")
    private Boolean allowWriteIn = false;

    // Constructors
    public Election() {
        this.createdAt = LocalDateTime.now();
        this.state = ElectionState.CREATED;
    }

    public Election(String title, String description) {
        this();
        this.title = title;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ElectionState getState() { return state; }
    public void setState(ElectionState state) { this.state = state; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public LocalDateTime getVotingStart() { return votingStart; }
    public void setVotingStart(LocalDateTime votingStart) { this.votingStart = votingStart; }

    public LocalDateTime getVotingEnd() { return votingEnd; }
    public void setVotingEnd(LocalDateTime votingEnd) { this.votingEnd = votingEnd; }

    public Integer getMaxCandidates() { return maxCandidates; }
    public void setMaxCandidates(Integer maxCandidates) { this.maxCandidates = maxCandidates; }

    public Boolean getAllowWriteIn() { return allowWriteIn; }
    public void setAllowWriteIn(Boolean allowWriteIn) { this.allowWriteIn = allowWriteIn; }

    // Helper methods
    public boolean isOpen() { 
        return ElectionState.OPEN.equals(state); 
    }
    
    public boolean isClosed() { 
        return ElectionState.CLOSED.equals(state); 
    }
    
    public boolean isCreated() { 
        return ElectionState.CREATED.equals(state); 
    }

    @Override
    public String toString() {
        return "Election{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", state=" + state +
                ", createdAt=" + createdAt +
                '}';
    }
}