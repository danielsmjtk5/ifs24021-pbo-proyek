package org.delcom.app.repositories;

import org.delcom.app.entities.Donation;
import org.delcom.app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Wajib ada
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DonationRepository extends JpaRepository<Donation, UUID> {
    
    // Filter standar
    List<Donation> findByIsHalal(Boolean isHalal);
    List<Donation> findByStatus(Donation.DonationStatus status);
    
    // Dashboard User
    List<Donation> findByCreatedBy(User user);
    
    // Statistik
    long countByIsHalal(Boolean isHalal);
    long countByStatus(Donation.DonationStatus status);
    
    // --- QUERY PENCARIAN (FIXED) ---
    // Perbaikan: Hapus fungsi LOWER() pada parameter :keyword agar PostgreSQL tidak error tipe data
    @Query("SELECT d FROM Donation d WHERE " +
           "(:keyword IS NULL OR LOWER(d.name) LIKE :keyword) AND " +
           "(:isHalal IS NULL OR d.isHalal = :isHalal)")
    List<Donation> searchDonations(@Param("keyword") String keyword, @Param("isHalal") Boolean isHalal);
}