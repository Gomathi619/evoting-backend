package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.model.EligibilityToken;
import com.evoting.evoting_backend.repository.EligibilityTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class EligibilityService {
    @Autowired private EligibilityTokenRepository tokenRepository;

    public EligibilityToken generateToken(Long voterId, Long electionId) {
        EligibilityToken token = new EligibilityToken(voterId, electionId);
        return tokenRepository.save(token);
    }

    public boolean validateAndConsumeToken(Long voterId, Long electionId, String tokenStr) {
        Optional<EligibilityToken> opt = tokenRepository.findById(tokenStr);
        if (opt.isEmpty()) return false;
        
        EligibilityToken token = opt.get();
        if (!token.isValid()) return false;
        
        token.setSpent(true);
        tokenRepository.save(token);
        return true;
    }
}