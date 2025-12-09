package org.delcom.app.repositories;

import org.delcom.app.entities.AuthToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest // Khusus untuk mengetes Repository (Database)
class AuthTokenRepositoryTest {

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Test
    @DisplayName("Harus bisa menyimpan dan mencari token berdasarkan UserID & Token string")
    void testSaveAndFindUserToken() {
        // 1. ARRANGE
        UUID userId = UUID.randomUUID();
        String tokenString = "jwt-token-rahasia";
        
        AuthToken token = new AuthToken(userId, tokenString);
        authTokenRepository.save(token); // Simpan ke H2 DB

        // 2. ACT
        AuthToken foundToken = authTokenRepository.findUserToken(userId, tokenString);

        // 3. ASSERT
        assertNotNull(foundToken, "Token harus ditemukan");
        assertEquals(userId, foundToken.getUserId());
        assertEquals(tokenString, foundToken.getToken());
    }

    @Test
    @DisplayName("Harus mengembalikan NULL jika token salah atau user salah")
    void testFindUserTokenNotFound() {
        // 1. ARRANGE
        UUID userId = UUID.randomUUID();
        String tokenString = "asli";
        authTokenRepository.save(new AuthToken(userId, tokenString));

        // 2. ACT & ASSERT
        
        // Coba cari dengan token salah
        AuthToken result1 = authTokenRepository.findUserToken(userId, "palsu");
        assertNull(result1, "Harusnya null karena token beda");

        // Coba cari dengan user ID salah
        AuthToken result2 = authTokenRepository.findUserToken(UUID.randomUUID(), tokenString);
        assertNull(result2, "Harusnya null karena user ID beda");
    }

    @Test
    @DisplayName("Harus bisa menghapus semua token milik User ID tertentu")
    void testDeleteByUserId() {
        // 1. ARRANGE
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        // Simpan 2 token untuk User 1
        authTokenRepository.save(new AuthToken(user1, "token1-a"));
        authTokenRepository.save(new AuthToken(user1, "token1-b"));
        
        // Simpan 1 token untuk User 2
        authTokenRepository.save(new AuthToken(user2, "token2-a"));

        assertEquals(3, authTokenRepository.count(), "Total harus ada 3 token");

        // 2. ACT
        authTokenRepository.deleteByUserId(user1);

        // 3. ASSERT
        // Token user 1 harus hilang semua (sisa 1 punya user 2)
        assertEquals(1, authTokenRepository.count());
        
        // Pastikan punya user 2 masih ada
        assertNotNull(authTokenRepository.findUserToken(user2, "token2-a"));
        
        // Pastikan punya user 1 sudah hilang
        assertNull(authTokenRepository.findUserToken(user1, "token1-a"));
    }
}