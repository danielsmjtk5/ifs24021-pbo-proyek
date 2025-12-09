package org.delcom.app.controllers;

import org.delcom.app.dto.DonationForm;
import org.delcom.app.entities.Donation;
import org.delcom.app.entities.User;
import org.delcom.app.services.DonationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DonationController.class)
public class DonationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DonationService donationService;

    // Variabel dummy untuk keperluan tes
    private User mockUser;
    private Authentication mockAuth;
    private UUID donationId;

    @BeforeEach
    void setUp() {
        // 1. Siapkan User Entity Palsu (Mock)
        // Ini penting karena controller Anda melakukan casting: (User) authentication.getPrincipal()
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@delcom.org");

        // 2. Siapkan Authentication Palsu
        mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser); // Agar casting berhasil
        when(mockAuth.isAuthenticated()).thenReturn(true);
        
        // 3. ID Donasi Dummy
        donationId = UUID.randomUUID();
    }

    // --- TEST HALAMAN DETAIL ---
    @Test
    @DisplayName("GET /donations/{id} - Menampilkan detail donasi")
    void testDetail() throws Exception {
        Donation donation = new Donation();
        donation.setId(donationId);
        
        when(donationService.getById(donationId)).thenReturn(donation);

        mockMvc.perform(get("/donations/{id}", donationId)
                        .with(authentication(mockAuth))) // Inject user login
                .andExpect(status().isOk())
                .andExpect(view().name("pages/donation/detail"))
                .andExpect(model().attributeExists("donation"));
    }

    // --- TEST HALAMAN FORM ADD ---
    @Test
    @DisplayName("GET /donations/add - Menampilkan form tambah")
    void testAddForm() throws Exception {
        mockMvc.perform(get("/donations/add")
                        .with(authentication(mockAuth)))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/donation/form"))
                .andExpect(model().attributeExists("donationForm"));
    }

    // --- TEST POST ADD (SIMPAN) ---
    @Test
    @DisplayName("POST /donations/add - Menyimpan donasi baru")
    void testSave() throws Exception {
        // Simulasi input form
        mockMvc.perform(post("/donations/add")
                        .with(authentication(mockAuth)) // User login
                        .with(csrf()) // Token CSRF (Wajib untuk POST di Spring Security)
                        .param("name", "Nasi Kotak")
                        .param("location", "Bandung")
                        .param("isHalal", "true")) // Parameter form
                .andExpect(status().is3xxRedirection()) // Harapannya redirect
                .andExpect(redirectedUrl("/")); // Redirect ke Home

        // Verifikasi service dipanggil dengan benar
        verify(donationService).saveDonation(any(DonationForm.class), eq(mockUser));
    }

    // --- TEST HALAMAN FORM EDIT ---
    @Test
    @DisplayName("GET /donations/edit/{id} - Menampilkan form edit dengan data lama")
    void testEditForm() throws Exception {
        Donation d = new Donation();
        d.setId(donationId);
        d.setName("Donasi Lama");
        d.setExpiredTime(LocalDateTime.now()); // Supaya tidak null saat dikonversi ke String

        when(donationService.getById(donationId)).thenReturn(d);

        mockMvc.perform(get("/donations/edit/{id}", donationId)
                        .with(authentication(mockAuth)))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/donation/edit"))
                .andExpect(model().attributeExists("donationForm"))
                .andExpect(model().attribute("id", donationId));
    }

    // --- TEST POST EDIT (UPDATE) ---
    @Test
    @DisplayName("POST /donations/edit/{id} - Mengupdate donasi")
    void testUpdate() throws Exception {
        mockMvc.perform(post("/donations/edit/{id}", donationId)
                        .with(authentication(mockAuth))
                        .with(csrf())
                        .param("name", "Nasi Update")
                        .param("description", "Deskripsi baru"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(donationService).updateDonation(eq(donationId), any(DonationForm.class), eq(mockUser));
    }

    // --- TEST DELETE ---
    @Test
    @DisplayName("GET /donations/delete/{id} - Menghapus donasi")
    void testDelete() throws Exception {
        mockMvc.perform(get("/donations/delete/{id}", donationId)
                        .with(authentication(mockAuth)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(donationService).deleteDonation(eq(donationId), eq(mockUser));
    }

    // --- TEST CLAIM ---
    @Test
    @DisplayName("GET /donations/claim/{id} - Mengklaim donasi")
    void testClaim() throws Exception {
        mockMvc.perform(get("/donations/claim/{id}", donationId)
                        .with(authentication(mockAuth)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/donations/" + donationId));

        verify(donationService).claimDonation(eq(donationId), eq(mockUser));
    }
}