package org.delcom.app.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName; // Import Wajib
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthTokenTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Test 1: POJO Manual (Constructor, Getter, Setter)")
    void testPojoMethods() {
        // Data Dummy
        UUID userId = UUID.randomUUID();
        String token = "abc-123-token";

        // 1. Test Constructor Parameter
        AuthToken authToken = new AuthToken(userId, token);
        
        // Verifikasi nilai awal
        assertThat(authToken.getUserId()).isEqualTo(userId);
        assertThat(authToken.getToken()).isEqualTo(token);
        assertThat(authToken.getCreatedAt()).isNotNull(); // Constructor sudah mengisi ini

        // 2. Test Setter
        UUID newId = UUID.randomUUID();
        String newToken = "xyz-789-token";
        
        authToken.setId(newId);
        authToken.setToken(newToken);
        authToken.setUserId(UUID.randomUUID());

        // 3. Test Getter
        assertThat(authToken.getId()).isEqualTo(newId);
        assertThat(authToken.getToken()).isEqualTo(newToken);
    }

    @Test
    @DisplayName("Test 2: JPA Persistence & @PrePersist")
    void testPersistenceLifecycle() {
        // 1. Buat object menggunakan Constructor Kosong (No-Args)
        AuthToken authToken = new AuthToken();
        authToken.setUserId(UUID.randomUUID());
        authToken.setToken("db-token-test");
        
        // Saat ini ID masih null
        assertThat(authToken.getId()).isNull();

        // 2. Simpan ke Database
        // persistFlushFind = simpan, flush, lalu ambil ulang dari DB
        AuthToken savedToken = entityManager.persistFlushFind(authToken);

        // 3. Verifikasi @GeneratedValue (ID harus terisi otomatis)
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getId()).isInstanceOf(UUID.class);

        // 4. Verifikasi @PrePersist (onCreate harus jalan mengisi createdAt)
        assertThat(savedToken.getCreatedAt()).isNotNull();
        assertThat(savedToken.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        
        // 5. Verifikasi Data Lain
        assertThat(savedToken.getToken()).isEqualTo("db-token-test");
    }
}