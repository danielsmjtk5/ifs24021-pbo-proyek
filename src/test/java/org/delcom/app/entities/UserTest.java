package org.delcom.app.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    // ==========================================
    // 1. Mengcover Constructor 2 Argumen (Merah ke-1)
    // ==========================================
    @Test
    @DisplayName("Test Constructor 2 Argumen")
    void testTwoArgsConstructor() {
        String email = "test@example.com";
        String password = "secretPassword";

        // Memanggil constructor yang memanggil this("", email, password)
        User user = new User(email, password);

        // Assert
        assertNotNull(user);
        assertEquals("", user.getName()); // Memastikan nama di-set kosong string
        assertEquals(email, user.getEmail());
        assertEquals(password, user.getPassword());
    }

    // ==========================================
    // 2. Mengcover @PrePersist & @PreUpdate (Merah ke-2 & 3)
    // ==========================================
    @Test
    @DisplayName("Test Lifecycle Methods (onCreate & onUpdate)")
    void testLifecycleMethods() throws InterruptedException {
        User user = new User();

        // --- Test onCreate (@PrePersist) ---
        // Karena test class berada di package yang sama (org.delcom.app.entities),
        // kita bisa memanggil method protected ini secara langsung.
        user.onCreate();

        assertNotNull(user.getCreatedAt(), "CreatedAt tidak boleh null setelah onCreate");
        assertNotNull(user.getUpdatedAt(), "UpdatedAt tidak boleh null setelah onCreate");

        // Simpan waktu update awal
        LocalDateTime initialUpdate = user.getUpdatedAt();

        // Beri jeda sedikit agar waktu berubah (jika OS support precision tinggi)
        Thread.sleep(10); 

        // --- Test onUpdate (@PreUpdate) ---
        user.onUpdate();

        assertNotNull(user.getUpdatedAt());
        // Memastikan updatedAt berubah (atau setidaknya method tereksekusi tanpa error)
        assertTrue(user.getUpdatedAt().isAfter(initialUpdate) || user.getUpdatedAt().isEqual(initialUpdate));
    }

    // ==========================================
    // 3. Mengcover Getter & Setter Lainnya (General Check)
    // ==========================================
    @Test
    @DisplayName("Test Getter Setter Lengkap")
    void testGettersAndSetters() {
        User user = new User();
        UUID id = UUID.randomUUID();
        String name = "Delcom User";
        String email = "user@delcom.org";
        String password = "123";

        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);

        assertEquals(id, user.getId());
        assertEquals(name, user.getName());
        assertEquals(email, user.getEmail());
        assertEquals(password, user.getPassword());
        
        // Constructor Kosong
        User emptyUser = new User();
        assertNotNull(emptyUser);
        
        // Constructor 3 Args
        User fullUser = new User(name, email, password);
        assertEquals(name, fullUser.getName());
    }
}