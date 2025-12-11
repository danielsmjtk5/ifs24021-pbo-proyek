package org.delcom.app.views;

import org.delcom.app.entities.User;
import org.delcom.app.services.DonationService;
// Hapus import TodoForm dan TodoService yang lama
// import org.delcom.app.utils.ConstUtil; // Opsional: Hapus jika ingin pakai String langsung
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeView {

    // 1. Ganti Service dari TodoService ke DonationService
    private final DonationService donationService;

    // Constructor Injection
    public HomeView(DonationService donationService) {
        this.donationService = donationService;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) String search,
                       @RequestParam(required = false) Boolean halal,
                       Model model) {
        
        // --- LOGIC KEAMANAN (TIDAK DIUBAH) ---
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
        // -------------------------------------

        // --- LOGIC BARU: DONASI MAKANAN ---
        
        // 1. Ambil daftar donasi (mendukung fitur search & filter)
        // Ini memenuhi Fitur Poin 6 (Daftar Data) & Fitur C (Pencarian)
        var donations = donationService.getAllDonations(search, halal);
        model.addAttribute("donations", donations);

        // 2. Ambil Statistik untuk Chart/Info (Fitur Poin 8 & D)
        long countHalal = donationService.countHalal(true);
        long countNonHalal = donationService.countHalal(false);
        
        model.addAttribute("halalCount", countHalal);
        model.addAttribute("nonHalalCount", countNonHalal);

        // 3. Kembalikan nilai search agar input field tidak reset
        model.addAttribute("search", search);

        // Arahkan ke file HTML list donasi yang sudah kita buat sebelumnya
        // Pastikan path ini sesuai dengan lokasi file HTML kamu (templates/pages/donation/list.html)
        return "pages/donation/list"; 
    }
}