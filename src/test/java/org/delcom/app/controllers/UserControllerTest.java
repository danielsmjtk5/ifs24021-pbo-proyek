package org.delcom.app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.delcom.app.configs.AuthContext;
import org.delcom.app.entities.AuthToken;
import org.delcom.app.entities.User;
import org.delcom.app.services.AuthTokenService;
import org.delcom.app.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false) // Matikan Security Filter (Login/CSRF)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthTokenService authTokenService;

    @MockBean
    private AuthContext authContext;

    private User mockUser;
    private String rawPassword = "password123";

    @BeforeEach
    void setUp() {
        // Enkripsi password asli agar cocok saat Controller melakukan cek matches()
        String encodedPassword = new BCryptPasswordEncoder().encode(rawPassword);

        mockUser = new User("Budi", "budi@mail.com", encodedPassword);
        mockUser.setId(UUID.randomUUID());
    }

    // ==========================================
    // 1. TEST REGISTER (/api/auth/register)
    // ==========================================

    @Test
    @DisplayName("Register: Success")
    void testRegister_Success() throws Exception {
        when(userService.getUserByEmail(any())).thenReturn(null);
        when(userService.createUser(any(), any(), any())).thenReturn(mockUser);

        User req = new User("Budi", "budi@mail.com", "pass");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("Register: Fail - Email Already Exists")
    void testRegister_EmailExists() throws Exception {
        when(userService.getUserByEmail(any())).thenReturn(mockUser); // User sudah ada

        User req = new User("Budi", "budi@mail.com", "pass");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pengguna sudah terdaftar dengan email ini"));
    }

    // --- VALIDASI REGISTER (NULL & EMPTY) ---

    @Test
    void testRegister_Fail_NullName() throws Exception {
        String json = "{\"email\":\"budi@mail.com\",\"password\":\"pass\"}"; // Name Null
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data nama tidak valid"));
    }

    @Test
    void testRegister_Fail_EmptyName() throws Exception {
        User req = new User("", "budi@mail.com", "pass"); // Name Empty
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data nama tidak valid"));
    }

    @Test
    void testRegister_Fail_NullEmail() throws Exception {
        String json = "{\"name\":\"Budi\",\"password\":\"pass\"}"; // Email Null
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data email tidak valid"));
    }

    @Test
    void testRegister_Fail_EmptyEmail() throws Exception {
        User req = new User("Budi", "", "password123"); // Email Empty
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data email tidak valid"));
    }

    @Test
    void testRegister_Fail_NullPassword() throws Exception {
        String json = "{\"name\":\"Budi\",\"email\":\"budi@mail.com\"}"; // Password Null
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data password tidak valid"));
    }

    @Test
    void testRegister_Fail_EmptyPassword() throws Exception {
        User req = new User("Budi", "budi@mail.com", ""); // Password Empty
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data password tidak valid"));
    }


    // ==========================================
    // 2. TEST LOGIN (/api/auth/login)
    // ==========================================

    @Test
    @DisplayName("Login: Success")
    void testLogin_Success() throws Exception {
        when(userService.getUserByEmail(mockUser.getEmail())).thenReturn(mockUser);
        
        AuthToken token = new AuthToken(mockUser.getId(), "jwt-token");
        when(authTokenService.createAuthToken(any())).thenReturn(token);
        
        // Mocking token lama ada
        when(authTokenService.findUserToken(eq(mockUser.getId()), anyString())).thenReturn(token);

        User req = new User(null, "budi@mail.com", rawPassword);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authToken").exists());
        
        verify(authTokenService).deleteAuthToken(mockUser.getId());
    }

    @Test
    @DisplayName("Login: Fail - User Not Found")
    void testLogin_UserNotFound() throws Exception {
        when(userService.getUserByEmail(any())).thenReturn(null);
        User req = new User(null, "unknown@mail.com", "pass");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email atau password salah"));
    }

    @Test
    @DisplayName("Login: Fail - Wrong Password")
    void testLogin_WrongPassword() throws Exception {
        when(userService.getUserByEmail(mockUser.getEmail())).thenReturn(mockUser);
        User req = new User(null, "budi@mail.com", "wrongpass");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email atau password salah"));
    }

    @Test
    @DisplayName("Login: Fail - Token Creation Error")
    void testLogin_TokenCreationFail() throws Exception {
        when(userService.getUserByEmail(mockUser.getEmail())).thenReturn(mockUser);
        when(authTokenService.createAuthToken(any())).thenReturn(null); // Return Null

        User req = new User(null, "budi@mail.com", rawPassword);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Gagal membuat token autentikasi"));
    }

    // --- VALIDASI LOGIN (NULL & EMPTY) ---

    @Test
    void testLogin_Fail_NullEmail() throws Exception {
        String json = "{\"password\":\"pass\"}";
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data tidak valid"));
    }

    @Test
    void testLogin_Fail_EmptyEmail() throws Exception {
        User req = new User(null, "", "pass");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data tidak valid"));
    }

    @Test
    void testLogin_Fail_NullPassword() throws Exception {
        String json = "{\"email\":\"budi@mail.com\"}";
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data tidak valid"));
    }

    @Test
    void testLogin_Fail_EmptyPassword() throws Exception {
        User req = new User(null, "budi@mail.com", "");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data tidak valid"));
    }


    // ==========================================
    // 3. TEST GET USER INFO (/api/users/me)
    // ==========================================

    @Test
    void testGetInfo_Success() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("budi@mail.com"));
    }

    @Test
    void testGetInfo_Unauthenticated() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(false);
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }


    // ==========================================
    // 4. TEST UPDATE USER (/api/users/me)
    // ==========================================

    @Test
    @DisplayName("Update User: Success")
    void testUpdateUser_Success() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(userService.updateUser(any(), any(), any())).thenReturn(mockUser);

        User req = new User("Budi Baru", "new@mail.com", null);
        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User berhasil diupdate"));
    }

    @Test
    void testUpdateUser_Unauthenticated() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(false);
        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Update User: Fail - Service Returns Null")
    void testUpdateUser_NotFound() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(userService.updateUser(any(), any(), any())).thenReturn(null);

        User req = new User("Budi", "budi@mail.com", null);
        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // --- VALIDASI UPDATE USER (NULL & EMPTY) ---

    @Test
    void testUpdateUser_Fail_NullName() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        String json = "{\"email\":\"new@mail.com\"}"; // Name Null
        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data nama tidak valid"));
    }

    @Test
    void testUpdateUser_Fail_EmptyName() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        User req = new User("", "new@mail.com", null); // Name Empty
        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data nama tidak valid"));
    }

    @Test
    void testUpdateUser_Fail_NullEmail() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        String json = "{\"name\":\"Budi Baru\"}"; // Email Null
        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data email tidak valid"));
    }
    
    @Test
    void testUpdateUser_Fail_EmptyEmail() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        User req = new User("Budi", "", null); // Email Empty
        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Data email tidak valid"));
    }


    // ==========================================
    // 5. TEST UPDATE PASSWORD (/api/users/me/password)
    // ==========================================

    @Test
    @DisplayName("Update Password: Success")
    void testUpdatePassword_Success() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(userService.updatePassword(any(), any())).thenReturn(mockUser);

        Map<String, String> payload = new HashMap<>();
        payload.put("password", rawPassword);
        payload.put("newPassword", "newPass123");

        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password berhasil diupdate"));
        
        verify(authTokenService).deleteAuthToken(mockUser.getId());
    }

    @Test
    void testUpdatePassword_Unauthenticated() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(false);
        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Update Password: Fail - Wrong Old Password")
    void testUpdatePassword_WrongOldPassword() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);

        Map<String, String> payload = new HashMap<>();
        payload.put("password", "salahbos"); 
        payload.put("newPassword", "baru");

        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Konfirmasi password tidak cocok"));
    }

    @Test
    @DisplayName("Update Password: Fail - Service Returns Null")
    void testUpdatePassword_UserNotFound() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        when(userService.updatePassword(any(), any())).thenReturn(null);

        Map<String, String> payload = new HashMap<>();
        payload.put("password", rawPassword);
        payload.put("newPassword", "baru");

        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    // --- VALIDASI UPDATE PASSWORD (NULL & EMPTY) ---

    @Test
    void testUpdatePassword_Fail_NullOldPassword() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        String json = "{\"newPassword\":\"newPass\"}"; // Old Password Null
        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Password lama dan baru wajib diisi"));
    }

    @Test
    void testUpdatePassword_Fail_EmptyOldPassword() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        Map<String, String> payload = new HashMap<>();
        payload.put("password", ""); // Old Password Empty
        payload.put("newPassword", "newPass");
        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Password lama dan baru wajib diisi"));
    }

    @Test
    void testUpdatePassword_Fail_NullNewPassword() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        String json = "{\"password\":\"oldPass\"}"; // New Password Null
        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Password lama dan baru wajib diisi"));
    }
    
    @Test
    void testUpdatePassword_Fail_EmptyNewPassword() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        Map<String, String> payload = new HashMap<>();
        payload.put("password", "oldPass");
        payload.put("newPassword", ""); // New Password Empty
        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Password lama dan baru wajib diisi"));
    }
}