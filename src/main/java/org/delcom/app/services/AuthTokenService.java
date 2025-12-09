package org.delcom.app.services;

import java.util.UUID;

import org.delcom.app.entities.AuthTokenTests;
import org.delcom.app.repositories.AuthTokenRepositoryTests;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {
    private final AuthTokenRepositoryTests authTokenRepository;

    public AuthTokenService(AuthTokenRepositoryTests authTokenRepository) {
        this.authTokenRepository = authTokenRepository;
    }

    @Transactional(readOnly = true)
    public AuthTokenTests findUserToken(UUID userId, String token) {
        return authTokenRepository.findUserToken(userId, token);
    }

    @Transactional
    public AuthTokenTests createAuthToken(AuthTokenTests authToken) {
        return authTokenRepository.save(authToken);
    }

    @Transactional
    public void deleteAuthToken(UUID userId) {
        authTokenRepository.deleteByUserId(userId);
    }
}
