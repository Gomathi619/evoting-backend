package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.service.TallyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/tally")
public class TallyController {

    @Autowired private TallyService tallyService;

    @GetMapping("/{electionId}")
    @PreAuthorize("hasRole('ELECTION_OFFICER') or hasRole('ADMIN')")
    public Map<String, Integer> getElectionResults(@PathVariable Long electionId) {
        return tallyService.tallyVotes(electionId);
    }
}