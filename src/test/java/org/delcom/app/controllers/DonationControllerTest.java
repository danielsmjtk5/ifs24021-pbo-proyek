package org.delcom.app.controllers;

import org.delcom.app.dto.DonationForm;
import org.delcom.app.entities.Donation;
import org.delcom.app.entities.User;
import org.delcom.app.services.DonationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DonationController.class)
@AutoConfigureMockMvc(addFilters = false)
class DonationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonationService donationService;

    @MockBean private org.delcom.app.services.UserService userService;
    @MockBean private org.delcom.app.configs.AuthContext authContext;
    @MockBean private org.delcom.app.services.AuthTokenService authTokenService;

    private User mockUser;
    private UsernamePasswordAuthenticationToken principal;

    @BeforeEach
    void setUp() {
        mockUser = new User("Budi", "budi@mail.com", "123");
        mockUser.setId(UUID.randomUUID());
        principal = new UsernamePasswordAuthenticationToken(mockUser, "password");
    }

    // --- TEST DETAIL (Lengkap dengan atribut agar Thymeleaf tidak error) ---
    @Test
    void testDetail() throws Exception {
        UUID id = UUID.randomUUID();
        
        Donation d = new Donation();
        d.setId(id);
        d.setName("Nasi Goreng");
        d.setLocation("Kantite");
        d.setCategory("Makanan Berat");
        d.setPortion(1);
        d.setIsHalal(true);
        d.setDescription("Enak");
        d.setStatus(Donation.DonationStatus.AVAILABLE); 
        d.setCreatedBy(mockUser);

        when(donationService.getById(id)).thenReturn(d);

        mockMvc.perform(get("/donations/" + id)
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/donation/detail"))
                .andExpect(model().attributeExists("donation"));
    }

    // --- TEST ADD FORM ---
    @Test
    void testAddForm() throws Exception {
        mockMvc.perform(get("/donations/add")
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/donation/form"));
    }

    @Test
    void testSave() throws Exception {
        mockMvc.perform(post("/donations/add")
                .principal(principal)
                .flashAttr("donationForm", new DonationForm()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(donationService).saveDonation(any(DonationForm.class), eq(mockUser));
    }

    // --- TEST EDIT FORM (Happy Path) ---
    @Test
    void testEditForm() throws Exception {
        UUID id = UUID.randomUUID();
        Donation d = new Donation();
        d.setName("Old Name");
        // Expired time null (default) -> untuk test cabang 'else' atau 'false'
        
        when(donationService.getById(id)).thenReturn(d);

        mockMvc.perform(get("/donations/edit/" + id)
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/donation/edit"));
    }

    // 🔥 BARU: TEST EDIT FORM DENGAN EXPIRED TIME (Agar Baris 63 Hijau)
    @Test
    void testEditForm_WithExpiredTime() throws Exception {
        UUID id = UUID.randomUUID();
        Donation d = new Donation();
        d.setName("Makanan Basi");
        // Kita set ExpiredTime agar kondisi IF menjadi TRUE
        d.setExpiredTime(LocalDateTime.now()); 
        
        when(donationService.getById(id)).thenReturn(d);

        mockMvc.perform(get("/donations/edit/" + id)
                .principal(principal))
                .andExpect(status().isOk());
        // Code coverage akan mendeteksi baris di dalam IF dijalankan
    }

    @Test
    void testUpdate() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/donations/edit/" + id)
                .principal(principal)
                .flashAttr("donationForm", new DonationForm()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(donationService).updateDonation(eq(id), any(DonationForm.class), eq(mockUser));
    }

    @Test
    void testDelete() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/donations/delete/" + id)
                .principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(donationService).deleteDonation(id, mockUser);
    }

    @Test
    void testClaim() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/donations/claim/" + id)
                .principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/donations/" + id));

        verify(donationService).claimDonation(id, mockUser);
    }

    // 🔥 PERBAIKAN: Tambahkan 'throws Exception' di baris ini
    // 🔥 PERBAIKAN: Gunakan assertThrows untuk menangkap Exception yang meledak
    @Test
    void testDelete_NotLoggedIn_ShouldThrowException() {
        UUID id = UUID.randomUUID();

        // Kita "bungkus" pemanggilan mockMvc dengan assertThrows.
        // Artinya: "Saya berekspektasi kode di dalam blok ini akan ERROR/MELEDAK"
        Exception exception = Assertions.assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/donations/delete/" + id));
        });

        // Setelah error tertangkap, kita bedah isinya.
        // Exception dari MockMvc biasanya terbungkus ServletException, 
        // jadi kita cek 'Cause' (penyebab aslinya).
        
        Assertions.assertNotNull(exception.getCause());
        Assertions.assertTrue(exception.getCause() instanceof RuntimeException);
        Assertions.assertEquals("User not logged in", exception.getCause().getMessage());
    }

    // --- SKENARIO 1: Authentication Null (Baris 23 bagian kiri) ---
    @Test
    void testGetAuthUser_WhenAuthenticationIsNull_ShouldThrowException() {
        UUID id = UUID.randomUUID();

        // Panggil endpoint TANPA .principal() sama sekali
        Exception exception = Assertions.assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/donations/delete/" + id));
        });

        // Verifikasi error
        Assertions.assertNotNull(exception.getCause());
        Assertions.assertTrue(exception.getCause() instanceof RuntimeException);
        Assertions.assertEquals("User not logged in", exception.getCause().getMessage());
    }

    // --- SKENARIO 2: Principal Null (Baris 23 bagian kanan) ---
    // Ini yang bikin garis kuning/merah jadi hijau total!
    @Test
    void testGetAuthUser_WhenPrincipalIsNull_ShouldThrowException() {
        UUID id = UUID.randomUUID();

        // 1. Kita Mock objek Authentication yang "rusak" (getPrincipal return null)
        org.springframework.security.core.Authentication brokenAuth = 
                org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        
        when(brokenAuth.getPrincipal()).thenReturn(null);

        // 2. Panggil endpoint DENGAN auth yang rusak tadi
        Exception exception = Assertions.assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/donations/delete/" + id)
                    .principal(brokenAuth)); 
        });

        // 3. Verifikasi error tetap muncul
        Assertions.assertNotNull(exception.getCause());
        Assertions.assertTrue(exception.getCause() instanceof RuntimeException);
        Assertions.assertEquals("User not logged in", exception.getCause().getMessage());
    }
}