package org.delcom.app.entities;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DonationTest {

    @Test
    void testGetterSetterAndConstructors() {
        Donation donation = new Donation();
        UUID id = UUID.randomUUID();
        User user = new User("Test User", "test@mail.com", "pass");
        LocalDateTime now = LocalDateTime.now();

        donation.setId(id);
        donation.setName("Nasi Goreng");
        donation.setLocation("Jakarta");
        donation.setLatitude(10.5);
        donation.setLongitude(100.5);
        donation.setCategory("Makanan");
        donation.setIsHalal(true);
        donation.setPortion(10);
        donation.setExpiredTime(now);
        donation.setDescription("Enak");
        donation.setPhotoUrl("img.jpg");
        donation.setStatus(Donation.DonationStatus.AVAILABLE);
        donation.setCreatedBy(user);
        donation.setClaimedBy(user);
        donation.setCreatedAt(now);
        donation.setUpdatedAt(now);

        Assertions.assertEquals(id, donation.getId());
        Assertions.assertEquals("Nasi Goreng", donation.getName());
        Assertions.assertEquals("Jakarta", donation.getLocation());
        Assertions.assertEquals(10.5, donation.getLatitude());
        Assertions.assertEquals(100.5, donation.getLongitude());
        Assertions.assertEquals("Makanan", donation.getCategory());
        Assertions.assertTrue(donation.getIsHalal());
        Assertions.assertEquals(10, donation.getPortion());
        Assertions.assertEquals(now, donation.getExpiredTime());
        Assertions.assertEquals("Enak", donation.getDescription());
        Assertions.assertEquals("img.jpg", donation.getPhotoUrl());
        Assertions.assertEquals(Donation.DonationStatus.AVAILABLE, donation.getStatus());
        Assertions.assertEquals(user, donation.getCreatedBy());
        Assertions.assertEquals(user, donation.getClaimedBy());
        Assertions.assertEquals(now, donation.getCreatedAt());
        Assertions.assertEquals(now, donation.getUpdatedAt());
    }

    @Test
    void testLifecycleCallbacks() throws Exception {
        // Karena method onCreate dan onUpdate protected, kita panggil via Reflection
        Donation donation = new Donation();
        
        // Test onCreate (Saat status NULL)
        Method onCreate = Donation.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(donation);

        Assertions.assertNotNull(donation.getCreatedAt());
        Assertions.assertNotNull(donation.getUpdatedAt());
        // Memastikan logic if (status == null) -> AVAILABLE berjalan
        Assertions.assertEquals(Donation.DonationStatus.AVAILABLE, donation.getStatus());

        // Test onUpdate
        Method onUpdate = Donation.class.getDeclaredMethod("onUpdate");
        onUpdate.setAccessible(true);
        onUpdate.invoke(donation);
        
        Assertions.assertNotNull(donation.getUpdatedAt());
    }

    // --- TAMBAHAN PENTING AGAR HIJAU ---
    @Test
    void testOnCreate_WhenStatusAlreadySet() throws Exception {
        Donation donation = new Donation();
        
        // Skenario: Kita set status MANUAL sebelum onCreate dipanggil
        // Ini untuk menguji cabang 'False' dari if (this.status == null)
        donation.setStatus(Donation.DonationStatus.BOOKED);

        Method onCreate = Donation.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(donation);

        // Assert: Status TIDAK boleh berubah jadi AVAILABLE, harus tetap BOOKED
        Assertions.assertEquals(Donation.DonationStatus.BOOKED, donation.getStatus());
        Assertions.assertNotNull(donation.getCreatedAt());
    }
}