package com.evoting.evoting_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "candidates")
public class Candidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String party;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "candidate_code")
    private String candidateCode; // Unique code for encryption

    @Column(columnDefinition = "TEXT")
    private String description;

    // Constructors
    public Candidate() {}

    public Candidate(String name, String party, Long electionId) {
        this.name = name;
        this.party = party;
        this.electionId = electionId;
        this.candidateCode = generateCandidateCode();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParty() { return party; }
    public void setParty(String party) { this.party = party; }

    public Long getElectionId() { return electionId; }
    public void setElectionId(Long electionId) { this.electionId = electionId; }

    public String getCandidateCode() { return candidateCode; }
    public void setCandidateCode(String candidateCode) { this.candidateCode = candidateCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    private String generateCandidateCode() {
        return "CAND_" + System.currentTimeMillis() + "_" + this.name.substring(0, Math.min(3, this.name.length())).toUpperCase();
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", party='" + party + '\'' +
                ", electionId=" + electionId +
                '}';
    }
}