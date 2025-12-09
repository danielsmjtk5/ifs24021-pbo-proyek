package org.delcom.app.controllers;

import org.delcom.app.services.DonationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
// Import khusus untuk Spring Boot 3.4+ (Pengganti @MockBean)
import org.springframework.test.context.bean.override.mockito.MockitoBean; 
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Menggunakan @MockitoBean karena Anda memakai Spring Boot versi 3.4.12
    // Ini menggantikan @MockBean yang sudah deprecated
    @MockitoBean
    private DonationService donationService;

    @Test
    @DisplayName("Dashboard menampilkan data donasi dengan benar saat user login")
    @WithMockUser(username = "admin", roles = {"ADMIN"}) // Wajib ada agar lolos Security
    public void testDashboard() throws Exception {
        
        // 1. ARRANGE (Persiapan Data Mock)
        long mockHalalCount = 150L;
        long mockNonHalalCount = 50L;

        // Simulasi respon dari DonationService
        // Pastikan DonationService sudah menggunakan parameter boolean (huruf kecil)
        when(donationService.countHalal(true)).thenReturn(mockHalalCount);
        when(donationService.countHalal(false)).thenReturn(mockNonHalalCount);

        // 2. ACT & ASSERT (Eksekusi & Verifikasi)
        mockMvc.perform(get("/dashboard"))
                // Harapannya: Status 200 OK
                .andExpect(status().isOk())
                
                // Harapannya: Nama view/file HTML sesuai controller
                .andExpect(view().name("pages/donation/dashboard"))
                
                // Harapannya: Model attribute 'halalCount' bernilai 150
                .andExpect(model().attribute("halalCount", mockHalalCount))
                
                // Harapannya: Model attribute 'nonHalalCount' bernilai 50
                .andExpect(model().attribute("nonHalalCount", mockNonHalalCount));
    }
}