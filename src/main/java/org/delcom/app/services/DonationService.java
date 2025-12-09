package org.delcom.app.services;

import org.delcom.app.dto.DonationForm;
import org.delcom.app.entities.Donation;
import org.delcom.app.entities.User;
import org.delcom.app.repositories.DonationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DonationService {

    @Autowired private DonationRepository donationRepo;
    @Autowired private FileStorageService fileService;

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
        Donation donation = donationRepo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        
        if (!donation.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
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

    public void deleteDonation(UUID id, User user) {
        Donation donation = donationRepo.findById(id).orElseThrow();
        if (donation.getCreatedBy().getId().equals(user.getId())) {
            donationRepo.delete(donation);
        }
    }
    
    public void claimDonation(UUID id, User user) {
        Donation d = donationRepo.findById(id).orElseThrow();
        if (d.getStatus() == Donation.DonationStatus.AVAILABLE) {
            d.setClaimedBy(user);
            d.setStatus(Donation.DonationStatus.BOOKED);
            donationRepo.save(d);
        }
    }

    // --- GET DATA (FIXED) ---
    public List<Donation> getAllDonations(String keyword, Boolean isHalal) {
        String searchPattern = null;
        
        // Perbaikan: Lakukan lowercase dan wildcard % di Java
        if (keyword != null && !keyword.isBlank()) {
            searchPattern = "%" + keyword.toLowerCase() + "%";
        }

        return donationRepo.searchDonations(searchPattern, isHalal);
    }

    public Donation getById(UUID id) {
        return donationRepo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
    }
    
    public long countHalal(Boolean isHalal) {
        return donationRepo.countByIsHalal(isHalal);
    }
    
    // Tambahan method countAll jika diperlukan di HomeView
    public long countAll() {
        return donationRepo.count();
    }
}