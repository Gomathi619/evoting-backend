package com.evoting.evoting_backend.model;

public enum ElectionState {
    CREATED,    // Election created but not open for voting
    OPEN,       // Election open for voting
    CLOSED      // Election closed, voting ended
}