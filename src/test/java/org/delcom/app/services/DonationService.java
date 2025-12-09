package org.delcom.app.services;

import org.delcom.app.dto.DonationForm;
import org.delcom.app.entities.Donation;
import org.delcom.app.entities.User;
import org.delcom.app.repositories.DonationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepo;

    @Mock
    private FileStorageService fileService;

    @InjectMocks
    private DonationService donationService;

    // --- TEST SAVE ---

    @Test
    @DisplayName("Save Donation: Sukses tanpa foto")
    void testSaveDonation_NoPhoto() {
        // 1. ARRANGE
        User user = new User();
        user.setId(UUID.randomUUID());

        DonationForm form = new DonationForm();
        form.setName("Nasi Kotak");
        form.setLocation("Jakarta");
        form.setCategory("Makanan");
        form.setIsHalal(true);
        form.setExpiredTime(LocalDateTime.now().plusDays(1).toString()); // String format ISO
        
        // Mock perilaku repository
        when(donationRepo.save(any(Donation.class))).thenAnswer(invocation -> {
            Donation d = invocation.getArgument(0);
            d.setId(UUID.randomUUID()); // Simulasi ID generate dari DB
            return d;
        });

        // 2. ACT
        donationService.saveDonation(form, user);

        // 3. ASSERT
        verify(donationRepo, times(1)).save(any(Donation.class));
        verifyNoInteractions(fileService); // Pastikan tidak ada upload file
    }

    @Test
    @DisplayName("Save Donation: Sukses DENGAN foto")
    void testSaveDonation_WithPhoto() throws IOException {
        // 1. ARRANGE
        User user = new User();
        
        // Mock MultipartFile
        MultipartFile mockPhoto = mock(MultipartFile.class);
        when(mockPhoto.isEmpty()).thenReturn(false); // Foto ada isinya

        DonationForm form = new DonationForm();
        form.setName("Sate");
        form.setPhoto(mockPhoto);

        // Mock save pertama (simpan entity)
        Donation savedDonation = new Donation();
        savedDonation.setId(UUID.randomUUID());
        when(donationRepo.save(any(Donation.class))).thenReturn(savedDonation);

        // Mock upload file
        String fakeFileName = "foto-sate.jpg";
        when(fileService.storeFile(eq(mockPhoto), any(UUID.class))).thenReturn(fakeFileName);

        // 2. ACT
        donationService.saveDonation(form, user);

        // 3. ASSERT
        // Save dipanggil 2 kali (1. simpan data awal, 2. update nama file foto)
        verify(donationRepo, times(2)).save(any(Donation.class));
        verify(fileService).storeFile(eq(mockPhoto), eq(savedDonation.getId()));
    }

    // --- TEST UPDATE ---

    @Test
    @DisplayName("Update Donation: Gagal jika User bukan pemilik (Unauthorized)")
    void testUpdateDonation_Unauthorized() {
        // 1. ARRANGE
        UUID donationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        User owner = new User(); owner.setId(ownerId);
        User otherUser = new User(); otherUser.setId(otherUserId); // User yang mau ngedit (Maling)

        Donation existingDonation = new Donation();
        existingDonation.setId(donationId);
        existingDonation.setCreatedBy(owner); // Milik Owner

        when(donationRepo.findById(donationId)).thenReturn(Optional.of(existingDonation));

        // 2. ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            donationService.updateDonation(donationId, new DonationForm(), otherUser);
        });

        assertEquals("Unauthorized: You are not the owner of this donation", exception.getMessage());
    }

    @Test
    @DisplayName("Update Donation: Sukses jika User adalah pemilik")
    void testUpdateDonation_Success() {
        // 1. ARRANGE
        UUID id = UUID.randomUUID();
        User user = new User(); user.setId(id); // ID user sama

        Donation existingDonation = new Donation();
        existingDonation.setId(id);
        existingDonation.setCreatedBy(user); // Milik User ini

        DonationForm form = new DonationForm();
        form.setName("Update Nama");
        form.setIsHalal(false);

        when(donationRepo.findById(id)).thenReturn(Optional.of(existingDonation));

        // 2. ACT
        donationService.updateDonation(id, form, user);

        // 3. ASSERT
        assertEquals("Update Nama", existingDonation.getName());
        assertFalse(existingDonation.getIsHalal());
        verify(donationRepo).save(existingDonation);
    }

    // --- TEST DELETE ---

    @Test
    @DisplayName("Delete Donation: Sukses menghapus")
    void testDeleteDonation_Success() {
        UUID id = UUID.randomUUID();
        User user = new User(); user.setId(id);

        Donation donation = new Donation();
        donation.setCreatedBy(user);

        when(donationRepo.findById(id)).thenReturn(Optional.of(donation));

        donationService.deleteDonation(id, user);

        verify(donationRepo).delete(donation);
    }

    @Test
    @DisplayName("Delete Donation: Gagal jika bukan pemilik")
    void testDeleteDonation_Unauthorized() {
        UUID id = UUID.randomUUID();
        User owner = new User(); owner.setId(UUID.randomUUID());
        User hacker = new User(); hacker.setId(UUID.randomUUID());

        Donation donation = new Donation();
        donation.setCreatedBy(owner);

        when(donationRepo.findById(id)).thenReturn(Optional.of(donation));

        assertThrows(RuntimeException.class, () -> {
            donationService.deleteDonation(id, hacker);
        });
        
        verify(donationRepo, never()).delete(any());
    }

    // --- TEST CLAIM ---

    @Test
    @DisplayName("Claim Donation: Berhasil jika status AVAILABLE")
    void testClaimDonation_Success() {
        UUID id = UUID.randomUUID();
        User claimer = new User();
        
        Donation donation = new Donation();
        donation.setStatus(Donation.DonationStatus.AVAILABLE); // Status OK

        when(donationRepo.findById(id)).thenReturn(Optional.of(donation));

        donationService.claimDonation(id, claimer);

        assertEquals(Donation.DonationStatus.BOOKED, donation.getStatus());
        assertEquals(claimer, donation.getClaimedBy());
        verify(donationRepo).save(donation);
    }

    @Test
    @DisplayName("Claim Donation: Tidak ada perubahan jika status BOOKED/EXPIRED")
    void testClaimDonation_AlreadyBooked() {
        UUID id = UUID.randomUUID();
        User claimer = new User();

        Donation donation = new Donation();
        donation.setStatus(Donation.DonationStatus.BOOKED); // Sudah diambil orang

        when(donationRepo.findById(id)).thenReturn(Optional.of(donation));

        donationService.claimDonation(id, claimer);

        // Status tidak berubah, repo.save tidak dipanggil
        assertEquals(Donation.DonationStatus.BOOKED, donation.getStatus());
        verify(donationRepo, never()).save(any());
    }

    // --- TEST GET DATA (SEARCH) ---

    @Test
    @DisplayName("Get All Donations: Keyword harus di-lowercase dan pakai wildcard")
    void testGetAllDonations_SearchLogic() {
        // Input user: "Ayam"
        String keyword = "Ayam"; 
        Boolean isHalal = true;

        donationService.getAllDonations(keyword, isHalal);

        // Verifikasi bahwa repo dipanggil dengan format "%ayam%"
        verify(donationRepo).searchDonations("%ayam%", true);
    }
    
    @Test
    @DisplayName("Get All Donations: Jika keyword null, kirim null ke repo")
    void testGetAllDonations_NullKeyword() {
        donationService.getAllDonations(null, false);
        verify(donationRepo).searchDonations(null, false);
    }

    // --- TEST COUNT ---
    @Test
    @DisplayName("Count Halal: Memanggil repository dengan benar")
    void testCountHalal() {
        donationService.countHalal(true);
        verify(donationRepo).countByIsHalal(true);
    }
    
    @Test
    @DisplayName("Get By Id: Throw Exception jika tidak ditemukan")
    void testGetById_NotFound() {
        UUID id = UUID.randomUUID();
        when(donationRepo.findById(id)).thenReturn(Optional.empty());

        Exception e = assertThrows(RuntimeException.class, () -> donationService.getById(id));
        assertTrue(e.getMessage().contains("Donation not found"));
    }
}