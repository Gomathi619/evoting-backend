package com.evoting.evoting_backend.model;

import jakarta.persistence.*;

@Entity
public class Candidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String party;

    @ManyToOne
    private Election election;

    public Candidate() {}

    public Candidate(Long id, String name, String party, Election election) {
        this.id = id;
        this.name = name;
        this.party = party;
        this.election = election;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParty() { return party; }
    public void setParty(String party) { this.party = party; }

    public Election getElection() { return election; }
    public void setElection(Election election) { this.election = election; }
}