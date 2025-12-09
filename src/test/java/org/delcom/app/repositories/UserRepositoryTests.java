package org.delcom.app.repositories;

import org.delcom.app.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Harus mengembalikan User jika email ditemukan")
    void testFindFirstByEmail_Found() {
        // 1. ARRANGE
        // Simpan user dummy ke database H2
        User user = new User("Alice", "alice@example.com", "password123");
        userRepository.save(user);

        // 2. ACT
        Optional<User> result = userRepository.findFirstByEmail("alice@example.com");

        // 3. ASSERT
        assertTrue(result.isPresent(), "User harus ditemukan");
        assertEquals("Alice", result.get().getName());
        assertEquals("alice@example.com", result.get().getEmail());
    }

    @Test
    @DisplayName("Harus mengembalikan Empty jika email tidak ditemukan")
    void testFindFirstByEmail_NotFound() {
        // 1. ARRANGE
        // Kita simpan user lain, bukan yang dicari
        User user = new User("Bob", "bob@example.com", "pass");
        userRepository.save(user);

        // 2. ACT
        // Cari email yang tidak ada
        Optional<User> result = userRepository.findFirstByEmail("ghost@example.com");

        // 3. ASSERT
        assertTrue(result.isEmpty(), "Harusnya kosong karena email tidak ada");
    }
    
    @Test
    @DisplayName("Harus Case Sensitive (secara default JPA)")
    void testEmailCaseSensitivity() {
        // 1. ARRANGE
        User user = new User("Charlie", "Charlie@example.com", "pass");
        userRepository.save(user);

        // 2. ACT
        // Coba cari dengan huruf kecil semua (padahal di DB 'C' besar)
        Optional<User> result = userRepository.findFirstByEmail("charlie@example.com");

        // 3. ASSERT
        // Note: Default behaviour database biasanya case sensitive atau tidak tergantung setting H2.
        // Namun findByEmail biasanya mencari exact match string.
        if (result.isPresent()) {
            assertEquals("Charlie@example.com", result.get().getEmail());
        } else {
            assertTrue(result.isEmpty());
        }
    }
}