package org.delcom.app.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.delcom.app.configs.AuthContext;
import org.delcom.app.entities.AuthToken;
import org.delcom.app.entities.User;
import org.delcom.app.services.AuthTokenService;
import org.delcom.app.services.UserService;
import org.delcom.app.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @InjectMocks
    private AuthInterceptor authInterceptor;

    @Mock private AuthContext authContext;
    @Mock private AuthTokenService authTokenService;
    @Mock private UserService userService;

    // Mock Objek HTTP Request & Response
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        // Persiapan untuk menangkap JSON error response
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    // --- CASE 1: PUBLIC ENDPOINT ---
    @Test
    @DisplayName("Harus LOLOS (return true) jika akses endpoint public")
    void testPublicEndpoint() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verifyNoInteractions(authTokenService); // Service tidak boleh dipanggil
    }

    // --- CASE 2: NO TOKEN ---
    @Test
    @DisplayName("Harus GAGAL jika tidak ada token")
    void testMissingToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/me");
        when(request.getHeader("Authorization")).thenReturn(null);

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(401);
        assertTrue(responseWriter.toString().contains("Token autentikasi tidak ditemukan"));
    }

    // --- CASE 3: INVALID JWT FORMAT ---
    @Test
    @DisplayName("Harus GAGAL jika format JWT tidak valid")
    void testInvalidJwtFormat() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/me");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");

        // Mock Static Method JwtUtil
        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // Paksa validateToken return false
            jwtUtilMock.when(() -> JwtUtil.validateToken(anyString(), anyBoolean())).thenReturn(false);

            boolean result = authInterceptor.preHandle(request, response, new Object());

            assertFalse(result);
            verify(response).setStatus(401);
            assertTrue(responseWriter.toString().contains("Token autentikasi tidak valid"));
        }
    }

    // --- CASE 4: TOKEN EXPIRED / TIDAK ADA DI DB ---
    @Test
    @DisplayName("Harus GAGAL jika token valid tapi tidak ditemukan di Database")
    void testTokenNotFoundInDB() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "valid-token-string";

        when(request.getRequestURI()).thenReturn("/api/users/me");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // 1. JWT Format Valid
            jwtUtilMock.when(() -> JwtUtil.validateToken(token, true)).thenReturn(true);
            // 2. Ekstrak ID Berhasil
            jwtUtilMock.when(() -> JwtUtil.extractUserId(token)).thenReturn(userId);

            // 3. TAPI... Di Database tidak ditemukan (return null)
            when(authTokenService.findUserToken(userId, token)).thenReturn(null);

            boolean result = authInterceptor.preHandle(request, response, new Object());

            assertFalse(result);
            verify(response).setStatus(401);
            assertTrue(responseWriter.toString().contains("Token autentikasi sudah expired"));
        }
    }

    // --- CASE 5: SUCCESS ---
    @Test
    @DisplayName("Harus SUKSES (return true) dan set AuthContext jika semua valid")
    void testSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "valid-jwt";

        // Siapkan Mock Data
        User mockUser = new User();
        mockUser.setId(userId);
        
        AuthToken mockAuthToken = new AuthToken();
        mockAuthToken.setUserId(userId);

        // Siapkan Request
        when(request.getRequestURI()).thenReturn("/api/users/me");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            // Mock Static Utils
            jwtUtilMock.when(() -> JwtUtil.validateToken(token, true)).thenReturn(true);
            jwtUtilMock.when(() -> JwtUtil.extractUserId(token)).thenReturn(userId);

            // Mock Services
            when(authTokenService.findUserToken(userId, token)).thenReturn(mockAuthToken);
            when(userService.getUserById(userId)).thenReturn(mockUser);

            // EKSEKUSI
            boolean result = authInterceptor.preHandle(request, response, new Object());

            // ASSERT
            assertTrue(result); // Harus true agar lanjut ke Controller
            verify(authContext).setAuthUser(mockUser); // Pastikan user disimpan di context
            verify(response, never()).setStatus(401); // Pastikan tidak ada error 401
        }
    }
}