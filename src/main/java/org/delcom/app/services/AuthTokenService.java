package org.delcom.app.services;

import java.util.UUID;
import java.util.Objects; // ✅ Tambahan Import

import org.delcom.app.entities.AuthToken;
import org.delcom.app.repositories.AuthTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {
    
    private final AuthTokenRepository authTokenRepository;

    public AuthTokenService(AuthTokenRepository authTokenRepository) {
        this.authTokenRepository = authTokenRepository;
    }

    @Transactional(readOnly = true)
    public AuthToken findUserToken(UUID userId, String token) {
        // ✅ Fix: Membungkus userId agar aman dari null warning
        return authTokenRepository.findUserToken(Objects.requireNonNull(userId), token);
    }

    @Transactional
    public AuthToken createAuthToken(AuthToken authToken) {
        // ✅ Fix: Memastikan objek entity tidak null sebelum di-save
        // Ini memperbaiki warning di baris 27
        return authTokenRepository.save(Objects.requireNonNull(authToken));
    }

    @Transactional
    public void deleteAuthToken(UUID userId) {
        // ✅ Fix: Membungkus userId
        authTokenRepository.deleteByUserId(Objects.requireNonNull(userId));
    }
}