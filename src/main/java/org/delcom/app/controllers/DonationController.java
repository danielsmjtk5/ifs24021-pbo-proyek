package org.delcom.app.controllers;

import org.delcom.app.dto.DonationForm;
import org.delcom.app.entities.Donation;
import org.delcom.app.entities.User;
import org.delcom.app.services.DonationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; // Import Penting
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/donations")
public class DonationController {

    @Autowired private DonationService donationService;

    // Helper method untuk mengambil User yang sedang login
    private User getAuthUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not logged in");
        }
        return (User) authentication.getPrincipal();
    }

    // 1. DETAIL DATA
    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("donation", donationService.getById(id));
        return "pages/donation/detail";
    }

    // 2. FORM TAMBAH
    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("donationForm", new DonationForm());
        return "pages/donation/form";
    }

    @PostMapping("/add")
    public String save(@ModelAttribute DonationForm form, Authentication authentication) {
        // PERBAIKAN: Ambil Real User dari session
        User currentUser = getAuthUser(authentication);
        
        donationService.saveDonation(form, currentUser);
        return "redirect:/"; // Redirect ke Home
    }

    // 3. FORM EDIT
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable UUID id, Model model) {
        Donation d = donationService.getById(id);
        DonationForm form = new DonationForm();
        form.setName(d.getName());
        form.setLocation(d.getLocation());
        form.setCategory(d.getCategory());
        form.setIsHalal(d.getIsHalal());
        form.setPortion(d.getPortion());
        form.setDescription(d.getDescription());
        if(d.getExpiredTime() != null) form.setExpiredTime(d.getExpiredTime().toString());
        
        model.addAttribute("donationForm", form);
        model.addAttribute("id", id);
        return "pages/donation/edit";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable UUID id, @ModelAttribute DonationForm form, Authentication authentication) {
        // PERBAIKAN: Ambil Real User
        User currentUser = getAuthUser(authentication);
        donationService.updateDonation(id, form, currentUser);
        return "redirect:/";
    }

    // 4. HAPUS
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable UUID id, Authentication authentication) {
        User currentUser = getAuthUser(authentication);
        donationService.deleteDonation(id, currentUser);
        return "redirect:/";
    }
    
    // 5. CLAIM
    @GetMapping("/claim/{id}")
    public String claim(@PathVariable UUID id, Authentication authentication) {
        User currentUser = getAuthUser(authentication);
        donationService.claimDonation(id, currentUser);
        return "redirect:/donations/" + id;
    }
}