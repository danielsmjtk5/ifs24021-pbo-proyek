package org.delcom.app.configs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    @DisplayName("Test Kotak Merah 1: Redirect ke /auth/login jika belum login")
    void testUnauthenticatedAccessRedirects() throws Exception {
        // Skenario: User belum login mencoba akses halaman terlindungi ("/dashboard")
        // Logika SecurityConfig baris 19 (res.sendRedirect) akan dieksekusi di sini.
        
        mockMvc.perform(get("/dashboard")) 
                .andExpect(status().is3xxRedirection()) // Memastikan statusnya 302 Found
                .andExpect(redirectedUrl("/auth/login")); // Memastikan tujuannya ke /auth/login
    }

    @Test
    @DisplayName("Test Kotak Merah 2: PasswordEncoder menggunakan BCrypt")
    void testPasswordEncoderBean() {
        // Skenario: Memastikan Bean yang dibuat di baris 41 adalah BCrypt
        
        // 1. Cek tipe class-nya (Menyentuh baris 'return new BCryptPasswordEncoder()')
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);

        // 2. Cek fungsionalitas (Opsional, tapi bagus untuk coverage)
        String passwordAsli = "rahasia123";
        String passwordHash = passwordEncoder.encode(passwordAsli);

        assertThat(passwordHash).isNotEqualTo(passwordAsli);
        assertThat(passwordEncoder.matches(passwordAsli, passwordHash)).isTrue();
    }
}