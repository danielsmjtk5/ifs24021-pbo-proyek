package org.delcom.app.services;

import java.util.Optional;
import java.util.UUID;

import org.delcom.app.entities.User;
import org.delcom.app.repositories.UserRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // ==========================================
    // 1. Test createUser
    // ==========================================

    @Test
    @DisplayName("createUser: Harus membuat user baru dan menyimpannya")
    void testCreateUser_Success() {
        // Arrange
        String name = "John Doe";
        String email = "john@example.com";
        String password = "securePassword";
        
        // Mock save untuk mengembalikan objek yang sama (atau objek baru yg disimulasikan tersimpan)
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.createUser(name, email, password);

        // Assert
        assertNotNull(result);
        assertEquals(name, result.getName()); // Asumsi ada getter
        assertEquals(email, result.getEmail());
        // Verify repository dipanggil 1 kali
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ==========================================
    // 2. Test getUserByEmail
    // ==========================================

    @Test
    @DisplayName("getUserByEmail: Harus mengembalikan User jika ditemukan")
    void testGetUserByEmail_Found() {
        // Arrange
        String email = "exist@example.com";
        User mockUser = new User("Name", email, "pass");
        when(userRepository.findFirstByEmail(email)).thenReturn(Optional.of(mockUser));

        // Act
        User result = userService.getUserByEmail(email);

        // Assert
        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    @Test
    @DisplayName("getUserByEmail: Harus mengembalikan null jika tidak ditemukan (Cover .orElse(null))")
    void testGetUserByEmail_NotFound() {
        // Arrange
        String email = "unknown@example.com";
        when(userRepository.findFirstByEmail(email)).thenReturn(Optional.empty());

        // Act
        User result = userService.getUserByEmail(email);

        // Assert
        assertNull(result);
    }

    // ==========================================
    // 3. Test getUserById
    // ==========================================

    @Test
    @DisplayName("getUserById: Harus melempar NullPointerException jika ID null")
    void testGetUserById_NullId() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> userService.getUserById(null));
        
        // Pastikan repo tidak dipanggil karena fail di requireNonNull
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("getUserById: Harus mengembalikan null jika user tidak ada di DB")
    void testGetUserById_NotFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act
        User result = userService.getUserById(id);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("getUserById: Harus mengembalikan user jika ditemukan")
    void testGetUserById_Found() {
        // Arrange
        UUID id = UUID.randomUUID();
        User mockUser = new User("Name", "email", "pass");
        when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));

        // Act
        User result = userService.getUserById(id);

        // Assert
        assertNotNull(result);
        assertEquals(mockUser, result);
    }

    // ==========================================
    // 4. Test updateUser
    // ==========================================

    @Test
    @DisplayName("updateUser: Harus melempar NullPointerException jika ID null")
    void testUpdateUser_NullId() {
        assertThrows(NullPointerException.class, () -> 
            userService.updateUser(null, "New Name", "new@email.com")
        );
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("updateUser: Harus mengembalikan null jika user tidak ditemukan (Cover if user == null)")
    void testUpdateUser_NotFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act
        User result = userService.updateUser(id, "New Name", "new@email.com");

        // Assert
        assertNull(result);
        // Pastikan save TIDAK dipanggil
        verify(userRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("updateUser: Harus update field dan save jika user ditemukan")
    void testUpdateUser_Success() {
        // Arrange
        UUID id = UUID.randomUUID();
        User existingUser = new User("Old Name", "old@email.com", "pass");
        
        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.updateUser(id, "New Name", "new@email.com");

        // Assert
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("new@email.com", result.getEmail());
        verify(userRepository, times(1)).save(existingUser);
    }

    // ==========================================
    // 5. Test updatePassword
    // ==========================================

    @Test
    @DisplayName("updatePassword: Harus melempar NullPointerException jika ID null")
    void testUpdatePassword_NullId() {
        assertThrows(NullPointerException.class, () -> 
            userService.updatePassword(null, "newPass")
        );
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("updatePassword: Harus mengembalikan null jika user tidak ditemukan")
    void testUpdatePassword_NotFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act
        User result = userService.updatePassword(id, "newPass");

        // Assert
        assertNull(result);
        verify(userRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("updatePassword: Harus update password dan save jika user ditemukan")
    void testUpdatePassword_Success() {
        // Arrange
        UUID id = UUID.randomUUID();
        User existingUser = new User("Name", "email", "oldPass");
        
        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.updatePassword(id, "newSecretPass");

        // Assert
        assertNotNull(result);
        assertEquals("newSecretPass", result.getPassword()); // Asumsi ada getter
        verify(userRepository, times(1)).save(existingUser);
    }
}