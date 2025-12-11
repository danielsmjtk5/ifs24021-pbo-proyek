package org.delcom.app.configs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WebMvcConfigUnitTest {

    @Test
    @DisplayName("Harus menghapus prefix '../' dari dirName saat konfigurasi resource handler")
    void testExposeDirectoryWithDotDotPrefix() {
        // 1. Setup Mock
        WebMvcConfig webMvcConfig = new WebMvcConfig();
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);

        // --- BAGIAN PENTING (FIX) ---
        // Kita harus memberi tahu Mockito: 
        // "Jika addResourceHandler dipanggil dengan string apapun, kembalikan objek 'registration'"
        // Jika baris ini tidak ada, dia mengembalikan null -> NullPointerException saat .addResourceLocations dipanggil
        when(registry.addResourceHandler(anyString())).thenReturn(registration);

        // 2. Tentukan input trigger
        String dirNameWithDotDot = "../outside-uploads";

        // 3. Panggil method private menggunakan Reflection
        ReflectionTestUtils.invokeMethod(webMvcConfig, "exposeDirectory", dirNameWithDotDot, registry);

        // 4. Verifikasi Logika
        // Kode aslimu: dirName.replace("../", "")
        // Jadi "/outside-uploads/**" adalah hasil yang diharapkan
        verify(registry).addResourceHandler("/outside-uploads/**");
        
        // Opsional: Verifikasi juga bahwa addResourceLocations dipanggil (untuk memastikan chain jalan)
        verify(registration).addResourceLocations(anyString());
    }
}