package org.delcom.app.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DonationFormTest {

    @Test
    @DisplayName("DonationForm harus bisa menyimpan dan mengambil data (Getter/Setter)")
    void testDonationFormGettersAndSetters() {
        // 1. ARRANGE (Siapkan Objek & Data Dummy)
        DonationForm form = new DonationForm();
        
        String dummyName = "Nasi Jumat Berkah";
        String dummyLocation = "Jakarta Selatan";
        String dummyCategory = "Makanan Berat";
        Boolean dummyIsHalal = true;
        Integer dummyPortion = 100;
        String dummyExpiredTime = "2024-12-31T10:00";
        String dummyDesc = "Makanan gratis untuk umum";
        
        // Kita mock MultipartFile karena itu Interface (tidak bisa di-new langsung)
        MultipartFile mockPhoto = mock(MultipartFile.class);

        // 2. ACT (Lakukan Set Data)
        form.setName(dummyName);
        form.setLocation(dummyLocation);
        form.setCategory(dummyCategory);
        form.setIsHalal(dummyIsHalal);
        form.setPortion(dummyPortion);
        form.setExpiredTime(dummyExpiredTime);
        form.setDescription(dummyDesc);
        form.setPhoto(mockPhoto);

        // 3. ASSERT (Verifikasi Data Tersimpan)
        // Pastikan objek tidak null
        assertNotNull(form);

        // Cek satu per satu
        assertEquals(dummyName, form.getName(), "Nama tidak sesuai");
        assertEquals(dummyLocation, form.getLocation(), "Lokasi tidak sesuai");
        assertEquals(dummyCategory, form.getCategory(), "Kategori tidak sesuai");
        assertEquals(dummyIsHalal, form.getIsHalal(), "Status Halal tidak sesuai");
        assertEquals(dummyPortion, form.getPortion(), "Porsi tidak sesuai");
        assertEquals(dummyExpiredTime, form.getExpiredTime(), "Waktu expired tidak sesuai");
        assertEquals(dummyDesc, form.getDescription(), "Deskripsi tidak sesuai");
        
        // Pastikan foto yang diambil sama dengan objek mock yang kita set
        assertEquals(mockPhoto, form.getPhoto(), "File foto tidak sesuai");
    }
}