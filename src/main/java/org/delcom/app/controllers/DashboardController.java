package org.delcom.app.controllers;

import org.delcom.app.services.DonationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired private DonationService donationService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long halalCount = donationService.countHalal(true);
        long nonHalalCount = donationService.countHalal(false);
        
        model.addAttribute("halalCount", halalCount);
        model.addAttribute("nonHalalCount", nonHalalCount);
        
        // PERBAIKAN: Sesuaikan path dengan lokasi file HTML kamu
        // Sebelumnya: "pages/dashboard" (Salah)
        // Sekarang: "pages/donation/dashboard" (Benar)
        return "pages/donation/dashboard";
    }
}