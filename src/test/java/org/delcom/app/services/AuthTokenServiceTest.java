package org.delcom.app.services;

import java.util.UUID;

import org.delcom.app.entities.AuthToken;
import org.delcom.app.repositories.AuthTokenRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private AuthTokenRepository authTokenRepository;

    @InjectMocks
    private AuthTokenService authTokenService;

    // ==========================================
    // Test untuk: findUserToken
    // ==========================================

    @Test
    @DisplayName("findUserToken: Harus memanggil repository saat userId tidak null")
    void testFindUserToken_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String tokenString = "sample-token-string";
        AuthToken mockToken = new AuthToken(); 
        // Set properti mockToken jika perlu, misal mockToken.setToken(tokenString);
        
        when(authTokenRepository.findUserToken(userId, tokenString)).thenReturn(mockToken);

        // Act
        AuthToken result = authTokenService.findUserToken(userId, tokenString);

        // Assert
        assertNotNull(result);
        assertEquals(mockToken, result);
        verify(authTokenRepository, times(1)).findUserToken(userId, tokenString);
    }

    @Test
    @DisplayName("findUserToken: Harus melempar NullPointerException jika userId null")
    void testFindUserToken_NullUserId() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            authTokenService.findUserToken(null, "some-token");
        });

        // Verifikasi bahwa repository TIDAK dipanggil karena gagal di requireNonNull
        verifyNoInteractions(authTokenRepository);
    }

    // ==========================================
    // Test untuk: createAuthToken
    // ==========================================

    @Test
    @DisplayName("createAuthToken: Harus menyimpan token saat object tidak null")
    void testCreateAuthToken_Success() {
        // Arrange
        AuthToken tokenToSave = new AuthToken();
        AuthToken savedToken = new AuthToken();
        
        when(authTokenRepository.save(tokenToSave)).thenReturn(savedToken);

        // Act
        AuthToken result = authTokenService.createAuthToken(tokenToSave);

        // Assert
        assertNotNull(result);
        verify(authTokenRepository, times(1)).save(tokenToSave);
    }

    @Test
    @DisplayName("createAuthToken: Harus melempar NullPointerException jika object null")
    void testCreateAuthToken_NullObject() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            authTokenService.createAuthToken(null);
        });

        verifyNoInteractions(authTokenRepository);
    }

    // ==========================================
    // Test untuk: deleteAuthToken
    // ==========================================

    @Test
    @DisplayName("deleteAuthToken: Harus menghapus token saat userId tidak null")
    void testDeleteAuthToken_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        authTokenService.deleteAuthToken(userId);

        // Assert
        verify(authTokenRepository, times(1)).deleteByUserId(userId);
    }

    @Test
    @DisplayName("deleteAuthToken: Harus melempar NullPointerException jika userId null")
    void testDeleteAuthToken_NullUserId() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            authTokenService.deleteAuthToken(null);
        });

        verifyNoInteractions(authTokenRepository);
    }
}