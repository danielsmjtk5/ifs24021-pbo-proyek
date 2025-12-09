package org.delcom.app.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthTokenTest {

    @Test
    @DisplayName("Constructor dengan parameter harus mengisi userId, token, dan createdAt")
    void testParameterizedConstructor() {
        // 1. ARRANGE
        UUID userId = UUID.randomUUID();
        String tokenString = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

        // 2. ACT
        AuthToken authToken = new AuthToken(userId, tokenString);

        // 3. ASSERT
        assertEquals(userId, authToken.getUserId());
        assertEquals(tokenString, authToken.getToken());
        
        // Constructor Anda memiliki logic: this.createdAt = LocalDateTime.now();
        // Jadi createdAt tidak boleh null
        assertNotNull(authToken.getCreatedAt(), "createdAt harus terisi otomatis di constructor");
    }

    @Test
    @DisplayName("Getter dan Setter harus berfungsi menyimpan nilai")
    void testGettersAndSetters() {
        // 1. ARRANGE
        AuthToken authToken = new AuthToken();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String token = "random-token-string";

        // 2. ACT
        authToken.setId(id);
        authToken.setUserId(userId);
        authToken.setToken(token);

        // 3. ASSERT
        assertEquals(id, authToken.getId());
        assertEquals(userId, authToken.getUserId());
        assertEquals(token, authToken.getToken());
    }

    @Test
    @DisplayName("Method onCreate (@PrePersist) harus mengisi createdAt")
    void testOnCreate() {
        // 1. ARRANGE
        AuthToken authToken = new AuthToken();
        // Saat pakai constructor kosong, createdAt masih null (karena field belum diinisialisasi)
        assertNull(authToken.getCreatedAt(), "Awalnya createdAt harus null");

        // 2. ACT
        // Kita panggil method protected ini secara manual untuk simulasi saat JPA mau menyimpan ke DB
        authToken.onCreate();

        // 3. ASSERT
        assertNotNull(authToken.getCreatedAt(), "Setelah onCreate dipanggil, createdAt harus terisi");
        
        // Pastikan waktunya baru saja dibuat (misal, tidak lebih dari 1 detik yang lalu)
        assertTrue(authToken.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(authToken.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(5)));
    }
}