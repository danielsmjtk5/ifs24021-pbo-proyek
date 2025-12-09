package org.delcom.app.configs;

import org.delcom.app.entities.User;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class) // Mengaktifkan fitur Mockito
class AuthContextTest {

    @Mock
    private User mockUser; // Membuat object User palsu/mock

    @Test
    void testSetAndGetAuthUser() {
        // Arrange
        AuthContext authContext = new AuthContext();

        // Act
        authContext.setAuthUser(mockUser);

        // Assert
        assertNotNull(authContext.getAuthUser());
        assertEquals(mockUser, authContext.getAuthUser(), "User yang diambil harus sama dengan yang di-set");
    }

    @Test
    void testIsAuthenticated() {
        AuthContext authContext = new AuthContext();

        // 1. Cek kondisi awal (User belum ada)
        assertFalse(authContext.isAuthenticated(), "Harusnya false karena user masih null");

        // 2. Set user
        authContext.setAuthUser(mockUser);
        
        // 3. Cek setelah user ada
        assertTrue(authContext.isAuthenticated(), "Harusnya true karena user sudah di-set");
    }

    @Test
    void testLogoutOrNullUser() {
        AuthContext authContext = new AuthContext();
        
        // Set user lalu hapus (set null)
        authContext.setAuthUser(mockUser);
        authContext.setAuthUser(null);

        assertFalse(authContext.isAuthenticated(), "Harusnya false setelah user di-set null");
        assertNull(authContext.getAuthUser());
    }
}