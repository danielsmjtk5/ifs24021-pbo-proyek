package org.delcom.app.repositories;

import org.delcom.app.entities.Donation;
import org.delcom.app.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DonationRepositoryTest {

    @Autowired
    private DonationRepository donationRepo;

    @Autowired
    private UserRepository userRepo; // Dibutuhkan untuk menyimpan User sebelum Donasi

    private User testUser;

    @BeforeEach
    void setUp() {
        // Bersihkan database sebelum setiap test
        donationRepo.deleteAll();
        userRepo.deleteAll();

        // 1. Buat & Simpan User Dummy (Wajib karena relasi @ManyToOne)
        testUser = new User("Donatur", "donatur@mail.com", "pass");
        userRepo.save(testUser);

        // 2. Siapkan Data Dummy Donasi
        createDonation("Nasi Goreng Ayam", true, Donation.DonationStatus.AVAILABLE);
        createDonation("Sate Ayam Madura", true, Donation.DonationStatus.AVAILABLE);
        createDonation("Mie Goreng Babi", false, Donation.DonationStatus.AVAILABLE); // Non-Halal
        createDonation("Roti Kadaluarsa", true, Donation.DonationStatus.EXPIRED);
    }

    // Helper untuk membuat data dummy dengan cepat
    private void createDonation(String name, boolean isHalal, Donation.DonationStatus status) {
        Donation d = new Donation();
        d.setName(name);
        d.setIsHalal(isHalal);
        d.setStatus(status);
        d.setCreatedBy(testUser); // Set User
        // Field lain opsional/nullable di entity
        donationRepo.save(d);
    }

    // --- TEST 1: COUNTING ---
    @Test
    @DisplayName("Hitung jumlah donasi Halal dan Non-Halal")
    void testCountByIsHalal() {
        long halalCount = donationRepo.countByIsHalal(true);
        long nonHalalCount = donationRepo.countByIsHalal(false);

        // Dari setup: Nasi(Halal), Sate(Halal), Roti(Halal) -> Total 3
        assertEquals(3, halalCount);
        
        // Dari setup: Mie Babi(Non-Halal) -> Total 1
        assertEquals(1, nonHalalCount);
    }

    @Test
    @DisplayName("Hitung berdasarkan Status")
    void testCountByStatus() {
        long available = donationRepo.countByStatus(Donation.DonationStatus.AVAILABLE);
        long expired = donationRepo.countByStatus(Donation.DonationStatus.EXPIRED);

        assertEquals(3, available); // Nasi, Sate, Mie
        assertEquals(1, expired);   // Roti
    }

    // --- TEST 2: CUSTOM SEARCH QUERY ---
    @Test
    @DisplayName("Search: Filter berdasarkan Keyword saja (Case Insensitive)")
    void testSearchByKeyword() {
        // Simulasi keyword "ayam" (harus pakai % karena query pakai LIKE)
        // Di Service Anda logic-nya: "%" + keyword.toLowerCase() + "%"
        String keyword = "%ayam%"; 

        List<Donation> results = donationRepo.searchDonations(keyword, null);

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(d -> d.getName().equals("Nasi Goreng Ayam")));
        assertTrue(results.stream().anyMatch(d -> d.getName().equals("Sate Ayam Madura")));
    }

    @Test
    @DisplayName("Search: Filter berdasarkan Halal saja")
    void testSearchByHalalOnly() {
        // Keyword null, IsHalal = false
        List<Donation> results = donationRepo.searchDonations(null, false);

        assertEquals(1, results.size());
        assertEquals("Mie Goreng Babi", results.get(0).getName());
    }

    @Test
    @DisplayName("Search: Kombinasi Keyword DAN Halal")
    void testSearchKeywordAndHalal() {
        // Cari yang ada kata "Goreng" TAPI harus "Non-Halal"
        List<Donation> results = donationRepo.searchDonations("%goreng%", false);

        assertEquals(1, results.size());
        assertEquals("Mie Goreng Babi", results.get(0).getName());
        
        // Catatan: "Nasi Goreng Ayam" tidak muncul karena dia Halal
    }

    @Test
    @DisplayName("Search: Jika semua parameter null, tampilkan semua")
    void testSearchAllNull() {
        List<Donation> results = donationRepo.searchDonations(null, null);
        
        // Total data di setUp ada 4
        assertEquals(4, results.size());
    }

    // --- TEST 3: FIND BY USER ---
    @Test
    @DisplayName("Cari donasi berdasarkan User pembuat")
    void testFindByCreatedBy() {
        List<Donation> myDonations = donationRepo.findByCreatedBy(testUser);
        
        assertEquals(4, myDonations.size());

        // Coba user lain
        User otherUser = new User("Orang Asing", "asing@mail.com", "123");
        userRepo.save(otherUser);
        
        List<Donation> others = donationRepo.findByCreatedBy(otherUser);
        assertTrue(others.isEmpty());
    }
}