package org.delcom.app.interceptors;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import org.delcom.app.configs.AuthContext;
import org.delcom.app.entities.AuthToken;
import org.delcom.app.entities.User;
import org.delcom.app.services.AuthTokenService;
import org.delcom.app.services.UserService;
import org.delcom.app.utils.JwtUtil;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private AuthContext authContext;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthInterceptor authInterceptor;

    // Helper untuk menangkap output JSON dari response
    private StringWriter responseWriter;

    private void setupResponseWriter() throws Exception {
        responseWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(writer);
    }

    // --- TEST CASE 1: PUBLIC ENDPOINTS ---

    @Test
    @DisplayName("Harus lolos (return true) jika endpoint adalah /api/auth/**")
    void testPublicEndpointAuth() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verifyNoInteractions(authTokenService, userService, authContext);
    }

    @Test
    @DisplayName("Harus lolos (return true) jika endpoint adalah /error")
    void testPublicEndpointError() throws Exception {
        when(request.getRequestURI()).thenReturn("/error");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verifyNoInteractions(authTokenService, userService, authContext);
    }

    // --- TEST CASE 2: TOKEN MISSING / INVALID FORMAT ---

    @Test
    @DisplayName("Gagal 401: Header Authorization kosong (null)")
    void testTokenMissingNull() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn(null);
        setupResponseWriter();

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(401);
        assertTrue(responseWriter.toString().contains("Token autentikasi tidak ditemukan"));
    }

    @Test
    @DisplayName("Gagal 401: Header Authorization tidak ada 'Bearer '")
    void testTokenFormatInvalid() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Basic 12345"); // Bukan Bearer
        setupResponseWriter();

        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Logika kode Anda: extractToken return null jika tidak startWith Bearer
        // Akibatnya masuk ke blok "Token autentikasi tidak ditemukan"
        assertFalse(result);
        verify(response).setStatus(401);
        assertTrue(responseWriter.toString().contains("Token autentikasi tidak ditemukan"));
    }

    // --- TEST CASE 3: STATIC MOCKING JWT UTIL & LOGIC FLOW ---

    @Test
    @DisplayName("Gagal 401: JwtUtil gagal memvalidasi token")
    void testJwtInvalid() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token.here");
        setupResponseWriter();

        // Mock Static JwtUtil
        try (MockedStatic<JwtUtil> mockedJwt = mockStatic(JwtUtil.class)) {
            mockedJwt.when(() -> JwtUtil.validateToken("invalid.token.here", true)).thenReturn(false);

            boolean result = authInterceptor.preHandle(request, response, new Object());

            assertFalse(result);
            verify(response).setStatus(401);
            assertTrue(responseWriter.toString().contains("Token autentikasi tidak valid"));
        }
    }

    @Test
    @DisplayName("Gagal 401: Jwt valid tapi userId extraction null")
    void testJwtUserIdNull() throws Exception {
        String token = "valid.structure.token";
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        setupResponseWriter();

        try (MockedStatic<JwtUtil> mockedJwt = mockStatic(JwtUtil.class)) {
            mockedJwt.when(() -> JwtUtil.validateToken(token, true)).thenReturn(true);
            mockedJwt.when(() -> JwtUtil.extractUserId(token)).thenReturn(null);

            boolean result = authInterceptor.preHandle(request, response, new Object());

            assertFalse(result);
            verify(response).setStatus(401);
            assertTrue(responseWriter.toString().contains("Format token autentikasi tidak valid"));
        }
    }

    @Test
    @DisplayName("Gagal 401: Token tidak ditemukan di Database (Expired/Revoked)")
    void testTokenNotFoundInDB() throws Exception {
        String token = "valid.jwt.token";
        UUID userId = UUID.randomUUID();

        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        setupResponseWriter();

        try (MockedStatic<JwtUtil> mockedJwt = mockStatic(JwtUtil.class)) {
            mockedJwt.when(() -> JwtUtil.validateToken(token, true)).thenReturn(true);
            mockedJwt.when(() -> JwtUtil.extractUserId(token)).thenReturn(userId);

            // Mock Service return null (token tidak ada di DB)
            when(authTokenService.findUserToken(userId, token)).thenReturn(null);

            boolean result = authInterceptor.preHandle(request, response, new Object());

            assertFalse(result);
            verify(response).setStatus(401);
            assertTrue(responseWriter.toString().contains("Token autentikasi sudah expired"));
        }
    }

    @Test
    @DisplayName("Gagal 404: User tidak ditemukan di Database")
    void testUserNotFoundInDB() throws Exception {
        String token = "valid.jwt.token";
        UUID userId = UUID.randomUUID();
        AuthToken authToken = new AuthToken();
        authToken.setUserId(userId);

        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        setupResponseWriter();

        try (MockedStatic<JwtUtil> mockedJwt = mockStatic(JwtUtil.class)) {
            mockedJwt.when(() -> JwtUtil.validateToken(token, true)).thenReturn(true);
            mockedJwt.when(() -> JwtUtil.extractUserId(token)).thenReturn(userId);

            // Mock Token ditemukan
            when(authTokenService.findUserToken(userId, token)).thenReturn(authToken);
            // Mock User TIDAK ditemukan
            when(userService.getUserById(userId)).thenReturn(null);

            boolean result = authInterceptor.preHandle(request, response, new Object());

            assertFalse(result);
            verify(response).setStatus(404);
            assertTrue(responseWriter.toString().contains("User tidak ditemukan"));
        }
    }

    // --- TEST CASE 4: HAPPY PATH ---

    @Test
    @DisplayName("Sukses: Token valid, User valid -> Set AuthContext")
    void testHappyPath() throws Exception {
        String token = "valid.jwt.token";
        UUID userId = UUID.randomUUID();
        
        AuthToken authToken = new AuthToken();
        authToken.setUserId(userId);
        
        User user = new User();
        user.setId(userId);
        user.setEmail("test@delcom.org");

        when(request.getRequestURI()).thenReturn("/api/users/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        // Tidak perlu setupResponseWriter karena sukses tidak menulis response error

        try (MockedStatic<JwtUtil> mockedJwt = mockStatic(JwtUtil.class)) {
            mockedJwt.when(() -> JwtUtil.validateToken(token, true)).thenReturn(true);
            mockedJwt.when(() -> JwtUtil.extractUserId(token)).thenReturn(userId);

            when(authTokenService.findUserToken(userId, token)).thenReturn(authToken);
            when(userService.getUserById(userId)).thenReturn(user);

            boolean result = authInterceptor.preHandle(request, response, new Object());

            // Assertions
            assertTrue(result, "Harus mengembalikan true agar request dilanjutkan ke Controller");
            
            // Verifikasi bahwa user diset ke context
            verify(authContext).setAuthUser(user);
        }
    }
}