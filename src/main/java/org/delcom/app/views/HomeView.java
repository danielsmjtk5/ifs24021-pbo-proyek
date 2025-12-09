package org.delcom.app.views;

import org.delcom.app.entities.Donation; // Pastikan import ini ada
import org.delcom.app.entities.User;
import org.delcom.app.services.DonationService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List; // Import List

@Controller
public class HomeView {

    private final DonationService donationService;

    public HomeView(DonationService donationService) {
        this.donationService = donationService;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) String search,
                       @RequestParam(required = false) Boolean halal,
                       Model model) {
        
        // --- LOGIC KEAMANAN ---
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ((authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/auth/logout";
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            return "redirect:/auth/logout";
        }

        User authUser = (User) principal;
        model.addAttribute("auth", authUser);
        // ----------------------

        // --- PERBAIKAN: GANTI 'var' DENGAN 'List<Donation>' ---
        List<Donation> donations = donationService.getAllDonations(search, halal);
        model.addAttribute("donations", donations);

        long countHalal = donationService.countHalal(true);
        long countNonHalal = donationService.countHalal(false);
        
        model.addAttribute("halalCount", countHalal);
        model.addAttribute("nonHalalCount", countNonHalal);
        model.addAttribute("search", search);

        return "pages/donation/list"; 
    }
}