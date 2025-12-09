package org.delcom.app.services;

import java.io.IOException;
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

    @Autowired
    private DonationRepository donationRepo;

    @Autowired
    private FileStorageService fileService;

    // --- CREATE ---
    public void saveDonation(DonationForm form, User user) {
        Donation donation = new Donation();
        mapFormToEntity(form, donation);
        donation.setCreatedBy(user);
        
        // Simpan dulu untuk generate ID
        donation = donationRepo.save(donation);

        // Upload Foto
        if (form.getPhoto() != null && !form.getPhoto().isEmpty()) {
            try {
                String fileName = fileService.storeFile(form.getPhoto(), donation.getId());
                donation.setPhotoUrl(fileName);
                donationRepo.save(donation); // Update nama file
            } catch (IOException e) {
                throw new RuntimeException("Gagal upload gambar: " + e.getMessage());
            }
        }
    }

    // --- EDIT ---
    public void updateDonation(UUID id, DonationForm form, User user) {
        Donation donation = donationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation not found with ID: " + id));
        
        // Validasi Pemilik
        if (!donation.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You are not the owner of this donation");
        }

        mapFormToEntity(form, donation);

        if (form.getPhoto() != null && !form.getPhoto().isEmpty()) {
            try {
                String fileName = fileService.storeFile(form.getPhoto(), donation.getId());
                donation.setPhotoUrl(fileName);
            } catch (IOException e) {
                 throw new RuntimeException("Gagal upload gambar update: " + e.getMessage());
            }
        }
        donationRepo.save(donation);
    }

    // Helper untuk mapping form ke entity
    private void mapFormToEntity(DonationForm form, Donation donation) {
        donation.setName(form.getName());
        donation.setLocation(form.getLocation());
        donation.setCategory(form.getCategory());
        donation.setIsHalal(form.getIsHalal());
        donation.setPortion(form.getPortion());
        donation.setDescription(form.getDescription());
        
        if (form.getExpiredTime() != null && !form.getExpiredTime().isEmpty()) {
             // Pastikan format String sesuai dengan ISO-8601 atau format yang diharapkan
             donation.setExpiredTime(LocalDateTime.parse(form.getExpiredTime()));
        }
    }

    // --- DELETE ---
    public void deleteDonation(UUID id, User user) {
        Donation donation = donationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation not found"));
                
        if (donation.getCreatedBy().getId().equals(user.getId())) {
            donationRepo.delete(donation);
        } else {
            throw new RuntimeException("Unauthorized delete action");
        }
    }
    
    // --- CLAIM ---
    public void claimDonation(UUID id, User user) {
        Donation d = donationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation not found"));
                
        if (d.getStatus() == Donation.DonationStatus.AVAILABLE) {
            d.setClaimedBy(user);
            d.setStatus(Donation.DonationStatus.BOOKED);
            donationRepo.save(d);
        }
    }

    // --- GET DATA (SEARCH) ---
    public List<Donation> getAllDonations(String keyword, Boolean isHalal) {
        String searchPattern = null;
        
        if (keyword != null && !keyword.isBlank()) {
            searchPattern = "%" + keyword.toLowerCase() + "%";
        }

        return donationRepo.searchDonations(searchPattern, isHalal);
    }

    // --- GET SINGLE ---
    public Donation getById(UUID id) {
        return donationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation not found"));
    }
    
    // --- COUNT FOR DASHBOARD (METHOD INI YANG DIBUTUHKAN TEST) ---
    public long countHalal(Boolean isHalal) {
        // Menggunakan wrapper class Boolean agar konsisten dengan entity
        return donationRepo.countByIsHalal(isHalal);
    }
    
    // --- COUNT ALL (Optional) ---
    public long countAll() {
        return donationRepo.count();
    }
}