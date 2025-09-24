package com.evoting.evoting_backend.model;

import jakarta.persistence.*;

@Entity
public class Election {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private ElectionState state;

    public Election() {}

    public Election(Long id, String title, ElectionState state) {
        this.id = id;
        this.title = title;
        this.state = state;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ElectionState getState() { return state; }
    public void setState(ElectionState state) { this.state = state; }
}