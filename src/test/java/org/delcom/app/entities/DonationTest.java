package org.delcom.app.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DonationTest {

    @Test
    @DisplayName("Getter dan Setter harus menyimpan data dengan benar (Termasuk Koordinat & Relasi)")
    void testGettersAndSetters() {
        // 1. ARRANGE
        Donation donation = new Donation();
        
        UUID id = UUID.randomUUID();
        String name = "Nasi Goreng Berkah";
        String location = "Masjid Raya";
        Double lat = -6.2088;
        Double lon = 106.8456;
        Boolean isHalal = true;
        
        // Mock User object (cukup new User() karena ini Unit Test biasa)
        User creator = new User();
        creator.setName("Donatur");
        
        User claimer = new User();
        claimer.setName("Penerima");

        // 2. ACT
        donation.setId(id);
        donation.setName(name);
        donation.setLocation(location);
        donation.setLatitude(lat);
        donation.setLongitude(lon);
        donation.setIsHalal(isHalal);
        donation.setCreatedBy(creator);
        donation.setClaimedBy(claimer);
        donation.setStatus(Donation.DonationStatus.BOOKED);

        // 3. ASSERT
        assertEquals(id, donation.getId());
        assertEquals(name, donation.getName());
        assertEquals(location, donation.getLocation());
        assertEquals(lat, donation.getLatitude(), "Latitude tidak sesuai");
        assertEquals(lon, donation.getLongitude(), "Longitude tidak sesuai");
        assertEquals(isHalal, donation.getIsHalal());
        assertEquals(creator, donation.getCreatedBy());
        assertEquals(claimer, donation.getClaimedBy());
        assertEquals(Donation.DonationStatus.BOOKED, donation.getStatus());
    }

    @Test
    @DisplayName("@PrePersist onCreate: Harus isi tanggal otomatis & set status default")
    void testOnCreate() {
        // 1. ARRANGE
        Donation donation = new Donation();
        
        // Pastikan awal null
        assertNull(donation.getCreatedAt());
        assertNull(donation.getStatus());

        // 2. ACT
        // Panggil method lifecycle secara manual (Bisa karena satu package)
        donation.onCreate();

        // 3. ASSERT
        // Cek Waktu
        assertNotNull(donation.getCreatedAt(), "CreatedAt harus terisi otomatis");
        assertNotNull(donation.getUpdatedAt(), "UpdatedAt harus terisi otomatis");
        
        // Cek Status Default
        assertEquals(Donation.DonationStatus.AVAILABLE, donation.getStatus(), 
                "Status default harus AVAILABLE jika null");
    }

    @Test
    @DisplayName("@PrePersist onCreate: Jangan timpa status jika sudah di-set sebelumnya")
    void testOnCreateWithExistingStatus() {
        Donation donation = new Donation();
        donation.setStatus(Donation.DonationStatus.EXPIRED); // Kita set manual

        donation.onCreate();

        // Harapannya TETAP EXPIRED, jangan berubah jadi AVAILABLE
        assertEquals(Donation.DonationStatus.EXPIRED, donation.getStatus());
    }

    @Test
    @DisplayName("@PreUpdate onUpdate: Harus update tanggal updatedAt")
    void testOnUpdate() throws InterruptedException {
        // 1. ARRANGE
        Donation donation = new Donation();
        donation.onCreate(); // Set waktu awal
        LocalDateTime oldUpdate = donation.getUpdatedAt();

        // Beri jeda sedikit agar waktu berubah (10 milidetik)
        Thread.sleep(10); 

        // 2. ACT
        donation.onUpdate();

        // 3. ASSERT
        assertNotNull(donation.getUpdatedAt());
        // Pastikan updatedAt yang baru LEBIH BARU (After) daripada yang lama
        assertTrue(donation.getUpdatedAt().isAfter(oldUpdate), 
                "UpdatedAt harus diperbarui ke waktu yang lebih baru");
    }
}