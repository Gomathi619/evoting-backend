package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.model.Voter;
import com.evoting.evoting_backend.repository.VoterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/voters")
public class VoterController {
    @Autowired private VoterRepository voterRepository;

    @GetMapping
    public List<Voter> getAllVoters() {
        return voterRepository.findAll();
    }

    @PostMapping
    public Voter addVoter(@RequestBody Voter voter) {
        return voterRepository.save(voter);
    }
}