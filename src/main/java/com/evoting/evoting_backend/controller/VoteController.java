package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.model.Vote;
import com.evoting.evoting_backend.service.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/votes")
public class VoteController {
    @Autowired private VoteService voteService;

    @PostMapping
    @PreAuthorize("hasRole('VOTER')")
    public Vote castVote(@RequestBody Vote vote) {
        return voteService.saveVote(vote);
    }

    @GetMapping("/election/{electionId}")
    @PreAuthorize("hasRole('ELECTION_OFFICER') or hasRole('ADMIN')")
    public List<Vote> getVotesByElection(@PathVariable Long electionId) {
        return voteService.getVotesByElection(electionId);
    }
}