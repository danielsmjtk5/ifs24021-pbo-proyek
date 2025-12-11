package org.delcom.app.services;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.delcom.app.dto.DonationForm;
import org.delcom.app.entities.Donation;
import org.delcom.app.entities.User;
import org.delcom.app.repositories.DonationRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepo;

    @Mock
    private FileStorageService fileService;

    @InjectMocks
    private DonationService donationService;

    // --- HELPER UNTUK MEMBUAT DATA DUMMY ---
    private User createUser(UUID id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private DonationForm createForm(boolean withPhoto, boolean withExpiredTime) {
        DonationForm form = new DonationForm();
        form.setName("Nasi Goreng");
        form.setLocation("Jakarta");
        form.setCategory("Food");
        form.setIsHalal(true);
        form.setPortion(10);
        form.setDescription("Enak");

        if (withExpiredTime) {
            form.setExpiredTime("2025-12-31T23:59:00");
        } else {
            form.setExpiredTime(null); // Penting untuk cover cabang 'else' di mapFormToEntity
        }

        if (withPhoto) {
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            form.setPhoto(mockFile);
        }

        return form;
    }

    // ==========================================
    // 1. TEST SAVE DONATION (CREATE)
    // ==========================================

    @Test
    @DisplayName("saveDonation: Sukses simpan dengan Foto dan ExpiredTime")
    void testSaveDonation_Success_WithPhoto() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        DonationForm form = createForm(true, true);

        // Mock save pertama (simulasi generate ID oleh DB)
        when(donationRepo.save(any(Donation.class))).thenAnswer(inv -> {
            Donation d = inv.getArgument(0);
            if(d.getId() == null) d.setId(UUID.randomUUID());
            return d;
        });

        // Mock upload file sukses
        when(fileService.storeFile(any(), any())).thenReturn("gambar.jpg");

        // Act
        donationService.saveDonation(form, user);

        // Assert
        verify(donationRepo, times(2)).save(any(Donation.class)); // 1x create, 1x update url
        verify(fileService, times(1)).storeFile(any(), any());
    }

    @Test
    @DisplayName("saveDonation: Sukses simpan TANPA Foto dan TANPA ExpiredTime")
    void testSaveDonation_Success_NoPhoto() throws Exception { 
        User user = createUser(UUID.randomUUID());
        DonationForm form = createForm(false, false); // No Photo, No Date (null)

        when(donationRepo.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        donationService.saveDonation(form, user);

        // Assert
        verify(fileService, times(0)).storeFile(any(), any()); 
        verify(donationRepo, times(1)).save(any(Donation.class));
    }

    @Test
    @DisplayName("saveDonation: Gagal Upload Foto (Catch RuntimeException)")
    void testSaveDonation_UploadFailed() throws Exception {
        User user = createUser(UUID.randomUUID());
        DonationForm form = createForm(true, false);

        // Mock save agar punya ID
        when(donationRepo.save(any(Donation.class))).thenAnswer(inv -> {
            Donation d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        // Paksa error IO saat upload
        doThrow(new IOException("Disk Full")).when(fileService).storeFile(any(), any());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            donationService.saveDonation(form, user);
        });

        assertTrue(ex.getMessage().contains("Gagal mengupload gambar"));
    }

    // ==========================================
    // 2. TEST UPDATE DONATION
    // ==========================================

    @Test
    @DisplayName("updateDonation: Sukses Update (Owner cocok, Ganti Foto)")
    void testUpdateDonation_Success() throws Exception {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        DonationForm form = createForm(true, true);

        Donation existingDonation = new Donation();
        existingDonation.setId(docId);
        existingDonation.setCreatedBy(user); // Owner cocok

        when(donationRepo.findById(docId)).thenReturn(Optional.of(existingDonation));
        when(fileService.storeFile(any(), any())).thenReturn("new-image.jpg");

        // Act
        donationService.updateDonation(docId, form, user);

        // Assert
        verify(donationRepo, times(1)).save(existingDonation);
        assertEquals("new-image.jpg", existingDonation.getPhotoUrl());
    }

    @Test
    @DisplayName("updateDonation: Error 'Unauthorized' jika User beda")
    void testUpdateDonation_Unauthorized() {
        // --- PENTING: TEST INI HANYA LULUS JIKA VALIDASI DI SERVICE DINYALAKAN ---
        UUID docId = UUID.randomUUID();
        User owner = createUser(UUID.randomUUID());
        User intruder = createUser(UUID.randomUUID()); // ID Beda

        Donation existingDonation = new Donation();
        existingDonation.setId(docId);
        existingDonation.setCreatedBy(owner);

        when(donationRepo.findById(docId)).thenReturn(Optional.of(existingDonation));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            donationService.updateDonation(docId, new DonationForm(), intruder);
        });

        assertEquals("Unauthorized", ex.getMessage());
        // Pastikan tidak tersimpan
        verify(donationRepo, times(0)).save(any());
    }

    @Test
    @DisplayName("updateDonation: Error 'Donation not found' jika ID salah")
    void testUpdateDonation_NotFound() {
        UUID docId = UUID.randomUUID();
        when(donationRepo.findById(docId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            donationService.updateDonation(docId, new DonationForm(), new User());
        });
        assertEquals("Donation not found", ex.getMessage());
    }

    @Test
    @DisplayName("updateDonation: Gagal Upload Foto saat Update")
    void testUpdateDonation_UploadFailed() throws Exception {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        DonationForm form = createForm(true, false);

        Donation existing = new Donation();
        existing.setId(docId);
        existing.setCreatedBy(user);

        when(donationRepo.findById(docId)).thenReturn(Optional.of(existing));
        doThrow(new IOException("Network Error")).when(fileService).storeFile(any(), any());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            donationService.updateDonation(docId, form, user);
        });

        assertTrue(ex.getMessage().contains("Gagal mengupload gambar saat update"));
    }

    // ==========================================
    // 3. TEST DELETE DONATION
    // ==========================================

    @Test
    @DisplayName("deleteDonation: Sukses Hapus (Owner cocok)")
    void testDeleteDonation_Success() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);

        Donation donation = new Donation();
        donation.setId(docId);
        donation.setCreatedBy(user);

        when(donationRepo.findById(docId)).thenReturn(Optional.of(donation));

        // Act
        donationService.deleteDonation(docId, user);

        // Assert
        verify(donationRepo, times(1)).delete(donation);
    }

    @Test
    @DisplayName("deleteDonation: Tidak Hapus jika bukan Owner (Coverage if false)")
    void testDeleteDonation_NotOwner() {
        UUID docId = UUID.randomUUID();
        User owner = createUser(UUID.randomUUID());
        User otherUser = createUser(UUID.randomUUID());

        Donation donation = new Donation();
        donation.setCreatedBy(owner);

        when(donationRepo.findById(docId)).thenReturn(Optional.of(donation));

        // Act
        donationService.deleteDonation(docId, otherUser);

        // Assert
        verify(donationRepo, times(0)).delete(any());
    }

    @Test
    @DisplayName("deleteDonation: Error jika ID tidak ketemu")
    void testDeleteDonation_NotFound() {
        UUID docId = UUID.randomUUID();
        when(donationRepo.findById(docId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            donationService.deleteDonation(docId, new User());
        });
    }

    // ==========================================
    // 4. TEST CLAIM DONATION
    // ==========================================

    @Test
    @DisplayName("claimDonation: Sukses Klaim (Status AVAILABLE)")
    void testClaimDonation_Success() {
        UUID docId = UUID.randomUUID();
        User claimer = createUser(UUID.randomUUID());
        Donation donation = new Donation();
        donation.setStatus(Donation.DonationStatus.AVAILABLE);

        when(donationRepo.findById(docId)).thenReturn(Optional.of(donation));

        // Act
        donationService.claimDonation(docId, claimer);

        // Assert
        assertEquals(Donation.DonationStatus.BOOKED, donation.getStatus());
        assertEquals(claimer, donation.getClaimedBy());
        verify(donationRepo, times(1)).save(donation);
    }

    @Test
    @DisplayName("claimDonation: Gagal Klaim (Status BOOKED/Taken)")
    void testClaimDonation_AlreadyBooked() {
        UUID docId = UUID.randomUUID();
        Donation donation = new Donation();
        donation.setStatus(Donation.DonationStatus.BOOKED);

        when(donationRepo.findById(docId)).thenReturn(Optional.of(donation));

        // Act
        donationService.claimDonation(docId, new User());

        // Assert
        assertEquals(Donation.DonationStatus.BOOKED, donation.getStatus());
        verify(donationRepo, times(0)).save(any());
    }

    @Test
    @DisplayName("claimDonation: Not Found")
    void testClaimDonation_NotFound() {
        UUID docId = UUID.randomUUID();
        when(donationRepo.findById(docId)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> donationService.claimDonation(docId, new User()));
    }

    // ==========================================
    // 5. TEST GET & STATISTIK
    // ==========================================

    @Test
    @DisplayName("getAllDonations: Delegasi ke repo sukses")
    void testGetAllDonations() {
        when(donationRepo.searchDonations("nasi", true))
                .thenReturn(Collections.emptyList());

        List<Donation> result = donationService.getAllDonations("nasi", true);
        assertNotNull(result);
        verify(donationRepo).searchDonations("nasi", true);
    }

    @Test
    @DisplayName("getById: Found")
    void testGetById_Found() {
        UUID id = UUID.randomUUID();
        Donation d = new Donation();
        when(donationRepo.findById(id)).thenReturn(Optional.of(d));

        Donation result = donationService.getById(id);
        assertEquals(d, result);
    }

    @Test
    @DisplayName("getById: Not Found throws RuntimeException")
    void testGetById_NotFound() {
        UUID id = UUID.randomUUID();
        when(donationRepo.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            donationService.getById(id)
        );
        assertEquals("Not found", ex.getMessage());
    }

    @Test
    @DisplayName("countHalal: Delegasi ke repo")
    void testCountHalal() {
        when(donationRepo.countByIsHalal(true)).thenReturn(10L);
        long count = donationService.countHalal(true);
        assertEquals(10L, count);
    }
}