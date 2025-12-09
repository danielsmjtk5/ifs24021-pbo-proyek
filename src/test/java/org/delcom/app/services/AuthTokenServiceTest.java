package org.delcom.app.services;

import org.delcom.app.entities.AuthToken;
import org.delcom.app.repositories.AuthTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private AuthTokenRepository authTokenRepository;

    @InjectMocks
    private AuthTokenService authTokenService;

    @Test
    @DisplayName("Create AuthToken: Harus menyimpan data ke repository")
    void testCreateAuthToken() {
        // 1. ARRANGE
        UUID userId = UUID.randomUUID();
        AuthToken token = new AuthToken(userId, "token-baru-123");

        // Mock: Jika save dipanggil, kembalikan objek token itu sendiri
        when(authTokenRepository.save(token)).thenReturn(token);

        // 2. ACT
        AuthToken createdToken = authTokenService.createAuthToken(token);

        // 3. ASSERT
        assertNotNull(createdToken);
        assertEquals(userId, createdToken.getUserId());
        
        // Verifikasi repository dipanggil 1 kali
        verify(authTokenRepository, times(1)).save(token);
    }

    @Test
    @DisplayName("Find User Token: Harus mengembalikan token jika ditemukan")
    void testFindUserToken() {
        // 1. ARRANGE
        UUID userId = UUID.randomUUID();
        String tokenString = "jwt-valid";
        AuthToken existingToken = new AuthToken(userId, tokenString);

        when(authTokenRepository.findUserToken(userId, tokenString)).thenReturn(existingToken);

        // 2. ACT
        AuthToken result = authTokenService.findUserToken(userId, tokenString);

        // 3. ASSERT
        assertNotNull(result);
        assertEquals(tokenString, result.getToken());
    }

    @Test
    @DisplayName("Delete AuthToken: Harus memanggil delete di repository")
    void testDeleteAuthToken() {
        // 1. ARRANGE
        UUID userId = UUID.randomUUID();

        // 2. ACT
        authTokenService.deleteAuthToken(userId);

        // 3. ASSERT
        verify(authTokenRepository, times(1)).deleteByUserId(userId);
    }
}