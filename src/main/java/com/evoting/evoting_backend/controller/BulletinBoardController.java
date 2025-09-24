package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.model.BulletinBoardEntry;
import com.evoting.evoting_backend.service.BulletinBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bulletin-board")
public class BulletinBoardController {
    @Autowired private BulletinBoardService bulletinBoardService;

    @GetMapping("/election/{electionId}")
    public List<BulletinBoardEntry> getElectionEntries(@PathVariable Long electionId) {
        return bulletinBoardService.getElectionEntries(electionId);
    }

    @GetMapping("/verify")
    public String verifyIntegrity() {
        boolean valid = bulletinBoardService.verifyBoardIntegrity();
        return valid ? "Integrity verified" : "Integrity compromised";
    }
}