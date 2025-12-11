package org.delcom.app.controllers;

import org.delcom.app.services.DonationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false) // ✅ SOLUSI: Matikan Security Filter (Login/CSRF)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonationService donationService;

    // Mocking Bean Security agar Application Context tidak error saat load
    @MockBean private org.delcom.app.services.UserService userService;
    @MockBean private org.delcom.app.configs.AuthContext authContext;
    @MockBean private org.delcom.app.services.AuthTokenService authTokenService;

    @Test
    void testDashboard() throws Exception {
        // Arrange
        when(donationService.countHalal(true)).thenReturn(10L);
        when(donationService.countHalal(false)).thenReturn(5L);

        // Act & Assert
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk()) // Harapannya 200 OK (Bukan 401/403)
                .andExpect(view().name("pages/donation/dashboard"))
                .andExpect(model().attribute("halalCount", 10L))
                .andExpect(model().attribute("nonHalalCount", 5L));
    }
}