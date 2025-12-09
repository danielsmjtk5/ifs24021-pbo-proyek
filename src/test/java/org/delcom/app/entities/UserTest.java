package org.delcom.app.entities;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("Constructor 3 Argumen: Harus mengisi nama, email, password")
    void testFullConstructor() {
        // 1. ACT
        User user = new User("Budi", "budi@mail.com", "pass123");

        // 2. ASSERT
        assertEquals("Budi", user.getName());
        assertEquals("budi@mail.com", user.getEmail());
        assertEquals("pass123", user.getPassword());
    }

    @Test
    @DisplayName("Constructor 2 Argumen: Nama harus otomatis string kosong (sesuai logic code)")
    void testPartialConstructor() {
        // Logic di User.java Anda: this("", email, password);
        
        // 1. ACT
        User user = new User("alice@mail.com", "alicePass");

        // 2. ASSERT
        assertEquals("alice@mail.com", user.getEmail());
        assertEquals("alicePass", user.getPassword());
        
        // Pastikan Name bukan null, tapi string kosong ""
        assertNotNull(user.getName()); 
        assertEquals("", user.getName(), "Constructor 2 argumen harus mengisi name dengan string kosong");
    }

    @Test
    @DisplayName("Getter & Setter standar harus berfungsi")
    void testGettersAndSetters() {
        // 1. ARRANGE
        User user = new User();
        UUID id = UUID.randomUUID();

        // 2. ACT
        user.setId(id);
        user.setName("Charlie");
        user.setEmail("charlie@mail.com");
        user.setPassword("secret");

        // 3. ASSERT
        assertEquals(id, user.getId());
        assertEquals("Charlie", user.getName());
        assertEquals("charlie@mail.com", user.getEmail());
        assertEquals("secret", user.getPassword());
    }

    @Test
    @DisplayName("@PrePersist onCreate: Harus mengisi createdAt dan updatedAt")
    void testOnCreate() {
        // 1. ARRANGE
        User user = new User();
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());

        // 2. ACT
        user.onCreate(); // Panggil manual

        // 3. ASSERT
        assertNotNull(user.getCreatedAt(), "createdAt harus terisi otomatis");
        assertNotNull(user.getUpdatedAt(), "updatedAt harus terisi otomatis");
        
        // Pastikan keduanya memiliki waktu yang (hampir) sama saat dibuat
        assertEquals(user.getCreatedAt(), user.getUpdatedAt());
    }

    @Test
    @DisplayName("@PreUpdate onUpdate: Harus memperbarui updatedAt saja")
    void testOnUpdate() throws InterruptedException {
        // 1. ARRANGE
        User user = new User();
        user.onCreate(); // Set waktu awal
        LocalDateTime createdTime = user.getCreatedAt();
        LocalDateTime oldUpdatedTime = user.getUpdatedAt();

        // Jeda sebentar (10ms)
        Thread.sleep(10);

        // 2. ACT
        user.onUpdate(); // Simulasi update

        // 3. ASSERT
        // CreatedAt TIDAK BOLEH berubah
        assertEquals(createdTime, user.getCreatedAt(), "createdAt tidak boleh berubah saat update");
        
        // UpdatedAt HARUS berubah menjadi lebih baru
        assertNotEquals(oldUpdatedTime, user.getUpdatedAt());
        assertTrue(user.getUpdatedAt().isAfter(oldUpdatedTime), "updatedAt harus diperbarui ke waktu sekarang");
    }
}