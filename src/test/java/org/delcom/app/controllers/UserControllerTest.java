package org.delcom.app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.delcom.app.configs.AuthContext;
import org.delcom.app.entities.AuthTokenTests;
import org.delcom.app.entities.User;
import org.delcom.app.services.AuthTokenService;
import org.delcom.app.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // Untuk konversi Object ke JSON String

    // Mock Service & Context
    @MockitoBean private UserService userService;
    @MockitoBean private AuthTokenService authTokenService;
    @MockitoBean private AuthContext authContext; // PENTING: Mock logic AuthContext

    // --- TEST REGISTER ---
    @Test
    @DisplayName("POST /api/auth/register - Sukses mendaftar user baru")
    @WithMockUser // Bypass filter Spring Security agar request masuk ke Controller
    void testRegisterSuccess() throws Exception {
        User reqUser = new User();
        reqUser.setName("Budi");
        reqUser.setEmail("budi@mail.com");
        reqUser.setPassword("password123");

        User createdUser = new User();
        createdUser.setId(UUID.randomUUID());
        createdUser.setName("Budi");

        // Mock logic
        when(userService.getUserByEmail(reqUser.getEmail())).thenReturn(null); // Email belum terpakai
        when(userService.createUser(anyString(), anyString(), anyString())).thenReturn(createdUser);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf()) // Wajib untuk POST
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - Gagal jika email sudah ada")
    @WithMockUser
    void testRegisterFailEmailExists() throws Exception {
        User reqUser = new User();
        reqUser.setName("Budi");
        reqUser.setEmail("exist@mail.com");
        reqUser.setPassword("pass");

        // Mock email sudah ada
        when(userService.getUserByEmail("exist@mail.com")).thenReturn(new User());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqUser)))
                .andExpect(status().isBadRequest()) // Harapannya 400 Bad Request
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Pengguna sudah terdaftar dengan email ini"));
    }

    // --- TEST LOGIN ---
    @Test
    @DisplayName("POST /api/auth/login - Sukses login")
    @WithMockUser
    void testLoginSuccess() throws Exception {
        // 1. Siapkan Data
        String rawPassword = "password123";
        // Enkripsi password dummy agar cocok dengan logic 'matches' di controller
        String encodedPassword = new BCryptPasswordEncoder().encode(rawPassword); 

        User dbUser = new User();
        dbUser.setId(UUID.randomUUID());
        dbUser.setEmail("budi@mail.com");
        dbUser.setPassword(encodedPassword); // Password di DB sudah ter-hash

        User reqUser = new User();
        reqUser.setEmail("budi@mail.com");
        reqUser.setPassword(rawPassword); // Password input user (plain)

        // 2. Mock Behavior
        when(userService.getUserByEmail(reqUser.getEmail())).thenReturn(dbUser);
        
        // Mock pembuatan token
        when(authTokenService.createAuthToken(any(AuthTokenTests.class))).thenReturn(new AuthTokenTests());

        // 3. Eksekusi
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.authToken").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login - Gagal jika password salah")
    @WithMockUser
    void testLoginWrongPassword() throws Exception {
        User dbUser = new User();
        dbUser.setEmail("budi@mail.com");
        dbUser.setPassword(new BCryptPasswordEncoder().encode("benar")); 

        User reqUser = new User();
        reqUser.setEmail("budi@mail.com");
        reqUser.setPassword("salah");

        when(userService.getUserByEmail(reqUser.getEmail())).thenReturn(dbUser);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email atau password salah"));
    }

    // --- TEST GET CURRENT USER ---
    @Test
    @DisplayName("GET /api/users/me - Sukses ambil data user sendiri")
    @WithMockUser
    void testGetUserInfo() throws Exception {
        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setName("Auth User");
        currentUser.setPassword("rahasia");

        // Mock AuthContext agar terdeteksi login
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(currentUser);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.name").value("Auth User"))
                // Password harus null di response (sesuai logic controller)
                .andExpect(jsonPath("$.data.user.password").doesNotExist()); 
    }

    @Test
    @DisplayName("GET /api/users/me - Gagal jika belum login (AuthContext false)")
    @WithMockUser
    void testGetUserInfoUnauthenticated() throws Exception {
        // Simulasi AuthContext kosong/false
        when(authContext.isAuthenticated()).thenReturn(false);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized()) // 401
                .andExpect(jsonPath("$.status").value("fail"));
    }

    // --- TEST UPDATE PASSWORD ---
    @Test
    @DisplayName("PUT /api/users/me/password - Sukses ganti password")
    @WithMockUser
    void testUpdatePassword() throws Exception {
        String oldPass = "lama123";
        String newPass = "baru456";

        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setPassword(new BCryptPasswordEncoder().encode(oldPass)); // DB hash

        // Payload Request
        Map<String, String> payload = new HashMap<>();
        payload.put("password", oldPass);
        payload.put("newPassword", newPass);

        // Mock AuthContext
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(currentUser);
        
        // Mock UserService Update
        when(userService.updatePassword(eq(currentUser.getId()), anyString())).thenReturn(currentUser);

        mockMvc.perform(put("/api/users/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password berhasil diupdate"));
        
        // Verifikasi token lama dihapus
        verify(authTokenService).deleteAuthToken(currentUser.getId());
    }
}