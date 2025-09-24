package com.evoting.evoting_backend.model;

import jakarta.persistence.*;

@Entity
public class Voter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;

    @ManyToOne
    private Election election;

    public Voter() {}

    public Voter(Long id, String name, String email, String password, Election election) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.election = election;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Election getElection() { return election; }
    public void setElection(Election election) { this.election = election; }
}