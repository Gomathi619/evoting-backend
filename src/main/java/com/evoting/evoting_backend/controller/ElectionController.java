package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.model.Election;
import com.evoting.evoting_backend.service.ElectionService;
import com.evoting.evoting_backend.service.PaillierKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/elections")
public class ElectionController {

    @Autowired private ElectionService electionService;
    @Autowired private PaillierKeyService paillierKeyService;

    @GetMapping
    public List<Election> getAllElections() {
        return electionService.getAllElections();
    }

    @PostMapping
    @PreAuthorize("hasRole('ELECTION_OFFICER')")
    public Election addElection(@RequestBody Election election) {
        return electionService.createElection(election);
    }
    
    @GetMapping("/{id}/open")
    @PreAuthorize("hasRole('ELECTION_OFFICER')")
    public ResponseEntity<String> openElection(@PathVariable Long id) {
        electionService.openElection(id);
        return ResponseEntity.ok("Election with ID " + id + " has been opened.");
    }
    
    @GetMapping("/{id}/close")
    @PreAuthorize("hasRole('ELECTION_OFFICER')")
    public ResponseEntity<String> closeElection(@PathVariable Long id) {
        electionService.closeElection(id);
        return ResponseEntity.ok("Election with ID " + id + " has been closed.");
    }

    @GetMapping("/public-key")
    public String getPublicKey() {
        return paillierKeyService.getPublicKey();
    }
}