package org.delcom.app.configs;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc; // Import ini penting
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- TEST 1: URL ACCESS CONTROL ---

    @Test
    @DisplayName("Public endpoints (/auth/**) should be accessible without login")
    void shouldAllowAccessToPublicEndpoints() throws Exception {
        // PERBAIKAN: Gunakan .andReturn() lalu cek statusnya manual dengan AssertJ
        // Ini menghindari error lambda pada Matcher
        
        // 1. Cek halaman login
        MvcResult loginResult = mockMvc.perform(get("/auth/login")).andReturn();
        int loginStatus = loginResult.getResponse().getStatus();
        
        // Pastikan tidak 401 (Unauthorized) atau 403 (Forbidden)
        assertThat(loginStatus).isNotEqualTo(401);
        assertThat(loginStatus).isNotEqualTo(403);

        // 2. Cek aset statis
        MvcResult assetResult = mockMvc.perform(get("/assets/css/style.css")).andReturn();
        int assetStatus = assetResult.getResponse().getStatus();
        
        assertThat(assetStatus).isNotEqualTo(403);
    }

    @Test
    @DisplayName("Protected endpoints (root /) should redirect unauthenticated users to /auth/login")
    void shouldRedirectUnauthenticatedUsers() throws Exception {
        // Bagian ini sudah benar karena menggunakan Matcher bawaan (.is3xxRedirection)
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    @DisplayName("Authenticated users should access protected endpoints")
    @WithMockUser(username = "testuser", roles = "USER") 
    void shouldAllowAuthenticatedUsers() throws Exception {
        // PERBAIKAN: Gunakan .andReturn()
        MvcResult result = mockMvc.perform(get("/")).andReturn();
        int status = result.getResponse().getStatus();

        // Jika user sudah login, dia TIDAK boleh dilempar (redirect 302) ke halaman login lagi
        // Statusnya harusnya 200 (OK) atau 404 (Not Found - jika halaman belum dibuat), tapi bukan 302/401/403
        assertThat(status).isNotEqualTo(302);
        assertThat(status).isNotEqualTo(401);
        assertThat(status).isNotEqualTo(403);
    }

    // --- TEST 2: PASSWORD ENCODER ---

    @Test
    @DisplayName("PasswordEncoder bean should be BCrypt and work correctly")
    void passwordEncoderShouldBeBCrypt() {
        // 1. Cek tipe bean
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);

        // 2. Cek fungsi encode
        String rawPassword = "mySecretPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Password asli tidak boleh sama dengan hasil enkripsi
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        
        // 3. Cek fungsi verifikasi (matches)
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("wrongPassword", encodedPassword)).isFalse();
    }
}