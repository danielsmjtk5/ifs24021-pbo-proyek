package org.delcom.app.services;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.delcom.app.dto.DonationForm;
import org.delcom.app.entities.Donation;
import org.delcom.app.entities.User;
import org.delcom.app.repositories.DonationRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    // --- HELPER ---
    private User createUser(UUID id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private DonationForm createBasicForm() {
        DonationForm form = new DonationForm();
        form.setName("Test Food");
        form.setLocation("Test Loc");
        form.setCategory("Food");
        form.setIsHalal(true);
        form.setPortion(1);
        form.setDescription("Desc");
        return form;
    }

    // ==========================================
    // 1. TEST SAVE DONATION (General)
    // ==========================================

    @Test
    @DisplayName("save: Sukses dengan Foto")
    void testSave_Full() throws Exception {
        User user = createUser(UUID.randomUUID());
        DonationForm form = createBasicForm();
        
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        form.setPhoto(file);

        when(donationRepo.save(any())).thenAnswer(i -> {
            Donation d = i.getArgument(0);
            if(d.getId() == null) d.setId(UUID.randomUUID());
            return d;
        });
        when(fileService.storeFile(any(), any())).thenReturn("img.jpg");

        donationService.saveDonation(form, user);

        verify(donationRepo, times(2)).save(any());
        verify(fileService).storeFile(any(), any());
    }

    @Test
    @DisplayName("save: Foto Kosong -> Skip Upload")
    void testSave_PhotoEmpty() throws Exception {
        User user = createUser(UUID.randomUUID());
        DonationForm form = createBasicForm();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true); // Empty
        form.setPhoto(file);

        when(donationRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        donationService.saveDonation(form, user);

        verify(fileService, never()).storeFile(any(), any());
    }

    @Test
    @DisplayName("save: Foto Null -> Skip Upload")
    void testSave_PhotoNull() throws Exception {
        User user = createUser(UUID.randomUUID());
        DonationForm form = createBasicForm();
        form.setPhoto(null);

        when(donationRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        donationService.saveDonation(form, user);

        verify(fileService, never()).storeFile(any(), any());
    }

    @Test
    @DisplayName("save: Upload Error -> Throw RuntimeException")
    void testSave_UploadFail() throws Exception {
        User user = createUser(UUID.randomUUID());
        DonationForm form = createBasicForm();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        form.setPhoto(file);

        when(donationRepo.save(any())).thenAnswer(i -> {
            Donation d = i.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        doThrow(new IOException("Error")).when(fileService).storeFile(any(), any());

        assertThrows(RuntimeException.class, () -> donationService.saveDonation(form, user));
    }

    // ==========================================
    // 2. TEST MAPPING & EXPIRED TIME (Handling Red Lines in Helper)
    // ==========================================

    @Test
    @DisplayName("Mapping: ExpiredTime Valid -> Ter-set")
    void testSave_ExpiredTime_Valid() {
        User user = createUser(UUID.randomUUID());
        DonationForm form = createBasicForm();
        form.setExpiredTime("2025-12-31T23:59:00"); // Valid String

        when(donationRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        donationService.saveDonation(form, user);

        verify(donationRepo).save(any());
        // Logic parse dijalankan
    }

    @Test
    @DisplayName("Mapping: ExpiredTime Null -> Skip (False && ...)")
    void testSave_ExpiredTime_Null() {
        User user = createUser(UUID.randomUUID());
        DonationForm form = createBasicForm();
        form.setExpiredTime(null); // NULL

        when(donationRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        donationService.saveDonation(form, user);

        verify(donationRepo).save(any());
    }

    @Test
    @DisplayName("Mapping: ExpiredTime Empty String -> Skip (True && False)")
    void testSave_ExpiredTime_Empty() {
        User user = createUser(UUID.randomUUID());
        DonationForm form = createBasicForm();
        form.setExpiredTime(""); // EMPTY STRING

        when(donationRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        donationService.saveDonation(form, user);

        verify(donationRepo).save(any());
    }

    // ==========================================
    // 3. TEST UPDATE DONATION
    // ==========================================

    @Test
    @DisplayName("update: Sukses dengan Foto Baru")
    void testUpdate_WithNewPhoto() throws Exception {
        UUID id = UUID.randomUUID();
        User user = createUser(UUID.randomUUID());
        Donation existing = new Donation();
        existing.setId(id);
        existing.setCreatedBy(user);

        DonationForm form = createBasicForm();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        form.setPhoto(file);

        when(donationRepo.findById(id)).thenReturn(Optional.of(existing));
        when(fileService.storeFile(any(), any())).thenReturn("updated.jpg");

        donationService.updateDonation(id, form, user);

        verify(fileService).storeFile(any(), any());
        assertEquals("updated.jpg", existing.getPhotoUrl());
    }

    @Test
    @DisplayName("update: Foto Null -> Skip")
    void testUpdate_PhotoNull() throws Exception {
        UUID id = UUID.randomUUID();
        User user = createUser(UUID.randomUUID());
        Donation existing = new Donation();
        existing.setId(id);
        existing.setCreatedBy(user);

        DonationForm form = createBasicForm();
        form.setPhoto(null);

        when(donationRepo.findById(id)).thenReturn(Optional.of(existing));

        donationService.updateDonation(id, form, user);
        verify(fileService, never()).storeFile(any(), any());
    }

    @Test
    @DisplayName("update: Foto Empty -> Skip")
    void testUpdate_PhotoEmpty() throws Exception {
        UUID id = UUID.randomUUID();
        User user = createUser(UUID.randomUUID());
        Donation existing = new Donation();
        existing.setId(id);
        existing.setCreatedBy(user);

        DonationForm form = createBasicForm();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true); // Empty
        form.setPhoto(file);

        when(donationRepo.findById(id)).thenReturn(Optional.of(existing));

        donationService.updateDonation(id, form, user);
        verify(fileService, never()).storeFile(any(), any());
    }

    @Test
    void testUpdate_Unauthorized() {
        UUID id = UUID.randomUUID();
        User owner = createUser(UUID.randomUUID());
        User intruder = createUser(UUID.randomUUID());
        Donation existing = new Donation();
        existing.setId(id);
        existing.setCreatedBy(owner);

        when(donationRepo.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(RuntimeException.class, () -> 
            donationService.updateDonation(id, createBasicForm(), intruder)
        );
    }

    @Test
    void testUpdate_NotFound() {
        UUID id = UUID.randomUUID();
        when(donationRepo.findById(id)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> 
            donationService.updateDonation(id, createBasicForm(), new User())
        );
    }

    @Test
    void testUpdate_UploadError() throws Exception {
        UUID id = UUID.randomUUID();
        User user = createUser(UUID.randomUUID());
        Donation existing = new Donation();
        existing.setId(id);
        existing.setCreatedBy(user);

        DonationForm form = createBasicForm();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        form.setPhoto(file);

        when(donationRepo.findById(id)).thenReturn(Optional.of(existing));
        doThrow(new IOException("Fail")).when(fileService).storeFile(any(), any());

        assertThrows(RuntimeException.class, () -> donationService.updateDonation(id, form, user));
    }

    // ==========================================
    // 4. CLAIM DONATION (Handling Red Lines in if-else)
    // ==========================================

    @Test
    @DisplayName("claim: Status AVAILABLE -> Success (True Branch)")
    void testClaim_Success() {
        UUID id = UUID.randomUUID();
        User user = createUser(UUID.randomUUID());
        Donation d = new Donation();
        d.setStatus(Donation.DonationStatus.AVAILABLE);

        when(donationRepo.findById(id)).thenReturn(Optional.of(d));
        donationService.claimDonation(id, user);

        assertEquals(Donation.DonationStatus.BOOKED, d.getStatus());
        assertEquals(user, d.getClaimedBy());
        verify(donationRepo).save(d);
    }

    @Test
    @DisplayName("claim: Status BOOKED -> Skip (False Branch)")
    void testClaim_AlreadyBooked() {
        UUID id = UUID.randomUUID();
        Donation d = new Donation();
        d.setStatus(Donation.DonationStatus.BOOKED); // NOT AVAILABLE

        when(donationRepo.findById(id)).thenReturn(Optional.of(d));
        donationService.claimDonation(id, new User());

        // Save tidak boleh dipanggil
        verify(donationRepo, never()).save(any());
    }
    
    @Test
    void testClaim_NotFound() {
        UUID id = UUID.randomUUID();
        when(donationRepo.findById(id)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> donationService.claimDonation(id, new User()));
    }

    // ==========================================
    // 5. DELETE & GETTERS
    // ==========================================

    @Test
    void testDelete_Success() {
        UUID id = UUID.randomUUID();
        User user = createUser(UUID.randomUUID());
        Donation d = new Donation();
        d.setCreatedBy(user);

        when(donationRepo.findById(id)).thenReturn(Optional.of(d));
        donationService.deleteDonation(id, user);
        verify(donationRepo).delete(d);
    }

    @Test
    void testDelete_Unauthorized() {
        UUID id = UUID.randomUUID();
        User owner = createUser(UUID.randomUUID());
        User other = createUser(UUID.randomUUID());
        Donation d = new Donation();
        d.setCreatedBy(owner);

        when(donationRepo.findById(id)).thenReturn(Optional.of(d));
        donationService.deleteDonation(id, other);
        verify(donationRepo, never()).delete(any());
    }
    
    @Test
    void testDelete_NotFound() {
        UUID id = UUID.randomUUID();
        when(donationRepo.findById(id)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> donationService.deleteDonation(id, new User()));
    }

    @Test
    void testGetAll() {
        donationService.getAllDonations("key", true);
        verify(donationRepo).searchDonations("key", true);
    }

    @Test
    void testGetById_Found() {
        UUID id = UUID.randomUUID();
        Donation d = new Donation();
        when(donationRepo.findById(id)).thenReturn(Optional.of(d));
        assertEquals(d, donationService.getById(id));
    }

    @Test
    void testGetById_NotFound() {
        UUID id = UUID.randomUUID();
        when(donationRepo.findById(id)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> donationService.getById(id));
    }

    @Test
    void testCount() {
        donationService.countHalal(false);
        verify(donationRepo).countByIsHalal(false);
    }
}