package org.delcom.app.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.delcom.app.dto.DonationForm;
import org.delcom.app.entities.Donation;
import org.delcom.app.entities.User;
import org.delcom.app.repositories.DonationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DonationService {

    @Autowired private DonationRepository donationRepo;
    @Autowired private FileStorageService fileService;

    // --- CREATE / TAMBAH DATA ---
    public void saveDonation(DonationForm form, User user) {
        Donation donation = new Donation();
        mapFormToEntity(form, donation);
        donation.setCreatedBy(user);
        
        // 1. Simpan dulu ke database agar ID (UUID) terbentuk
        donation = donationRepo.save(donation);

        // 2. Handle Upload Foto
        if (form.getPhoto() != null && !form.getPhoto().isEmpty()) {
            try {
                // Menggunakan ID donasi untuk penamaan file
                String fileName = fileService.storeFile(form.getPhoto(), donation.getId());
                donation.setPhotoUrl(fileName);
                
                // 3. Simpan ulang (Update) untuk memasukkan nama file
                donationRepo.save(donation);
            } catch (Exception e) {
                // Lempar RuntimeException agar tertangkap oleh Controller/Test
                throw new RuntimeException("Gagal mengupload gambar: " + e.getMessage());
            }
        }
    }

    // --- EDIT / UBAH DATA ---
    public void updateDonation(UUID id, DonationForm form, User user) {
        Donation donation = donationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation not found"));
        
        // --- VALIDASI KEPEMILIKAN (SECURITY) ---
        // Test: testUpdateDonation_Unauthorized akan LULUS karena blok ini aktif
        if (!donation.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        mapFormToEntity(form, donation);

        // Handle Ganti Foto
        if (form.getPhoto() != null && !form.getPhoto().isEmpty()) {
            try {
                String fileName = fileService.storeFile(form.getPhoto(), donation.getId());
                donation.setPhotoUrl(fileName);
            } catch (Exception e) {
                 throw new RuntimeException("Gagal mengupload gambar saat update: " + e.getMessage());
            }
        }

        donationRepo.save(donation);
    }

    // --- DELETE / HAPUS DATA ---
    public void deleteDonation(UUID id, User user) {
        Donation donation = donationRepo.findById(id).orElseThrow();
        
        // Hanya pemilik yang bisa menghapus
        if (donation.getCreatedBy().getId().equals(user.getId())) {
            donationRepo.delete(donation);
        }
    }
    
    // --- CLAIM DONASI ---
    public void claimDonation(UUID id, User user) {
        Donation d = donationRepo.findById(id).orElseThrow();
        if (d.getStatus() == Donation.DonationStatus.AVAILABLE) {
            d.setClaimedBy(user);
            d.setStatus(Donation.DonationStatus.BOOKED);
            donationRepo.save(d);
        }
    }

    // --- GET DATA ---
    public List<Donation> getAllDonations(String keyword, Boolean isHalal) {
        return donationRepo.searchDonations(keyword, isHalal);
    }

    public Donation getById(UUID id) {
        return donationRepo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
    }
    
    // --- STATISTIK ---
    public long countHalal(Boolean isHalal) {
        return donationRepo.countByIsHalal(isHalal);
    }

    // --- HELPER MAPPING ---
    private void mapFormToEntity(DonationForm form, Donation donation) {
        donation.setName(form.getName());
        donation.setLocation(form.getLocation());
        donation.setCategory(form.getCategory());
        donation.setIsHalal(form.getIsHalal());
        donation.setPortion(form.getPortion());
        donation.setDescription(form.getDescription());
        
        if (form.getExpiredTime() != null && !form.getExpiredTime().isEmpty()) {
             donation.setExpiredTime(LocalDateTime.parse(form.getExpiredTime()));
        }
    }
}