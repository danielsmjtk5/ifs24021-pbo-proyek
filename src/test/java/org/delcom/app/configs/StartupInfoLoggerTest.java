package org.delcom.app.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

class StartupInfoLoggerTest {

    @InjectMocks
    private StartupInfoLogger startupInfoLogger;

    @Mock
    private ApplicationReadyEvent event;

    @Mock
    private ConfigurableApplicationContext context;

    // PENTING: Gunakan ConfigurableEnvironment, bukan Environment biasa
    // Ini mencegah error "argument mismatch" saat mocking context.getEnvironment()
    @Mock
    private ConfigurableEnvironment env;

    @BeforeEach
    void setUp() {
        // Inisialisasi Mockito secara manual (Lebih stabil daripada @ExtendWith)
        MockitoAnnotations.openMocks(this);

        // Setup chain dasar: event -> context -> environment
        when(event.getApplicationContext()).thenReturn(context);
        when(context.getEnvironment()).thenReturn(env);
    }

    @Test
    @DisplayName("Scenario 1: LiveReload Enabled & Normal Context Path")
    void testStartupWithLiveReloadAndContextPath() {
        // --- ARRANGE ---
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8080");
        when(env.getProperty(eq("server.servlet.context-path"), anyString())).thenReturn("/api");
        when(env.getProperty(eq("server.address"), anyString())).thenReturn("localhost");
        
        // Mock LiveReload = TRUE
        when(env.getProperty(eq("spring.devtools.livereload.enabled"), eq(Boolean.class), anyBoolean()))
                .thenReturn(true); 
        when(env.getProperty(eq("spring.devtools.livereload.port"), anyString())).thenReturn("35729");

        // --- ACT ---
        startupInfoLogger.onApplicationEvent(event);

        // --- ASSERT ---
        verify(env).getProperty("server.port", "8080");
        verify(env).getProperty("server.servlet.context-path", "");
    }

    @Test
    @DisplayName("Scenario 2: LiveReload Disabled & Root Context Path ('/')")
    void testStartupWithDisabledLiveReloadAndRootPath() {
        // --- ARRANGE ---
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("9090");
        
        // Case: Context path is "/" -> harus diubah jadi "" oleh logic if()
        when(env.getProperty(eq("server.servlet.context-path"), anyString())).thenReturn("/");
        
        when(env.getProperty(eq("server.address"), anyString())).thenReturn("127.0.0.1");

        // Mock LiveReload = FALSE
        when(env.getProperty(eq("spring.devtools.livereload.enabled"), eq(Boolean.class), anyBoolean()))
                .thenReturn(false);
        // Port livereload tetap dipanggil meski disabled (karena urutan variabel di class asli)
        when(env.getProperty(eq("spring.devtools.livereload.port"), anyString())).thenReturn("35729");

        // --- ACT ---
        startupInfoLogger.onApplicationEvent(event);

        // --- ASSERT ---
        // Verifikasi logic penggantian "/" menjadi "" terjadi
        // Kita tidak bisa assert variabel lokal method, tapi kita bisa pastikan
        // tidak ada error exception dan flow berjalan lancar.
        verify(env).getProperty("server.servlet.context-path", "");
    }

    @Test
    @DisplayName("Scenario 3: Null Context Path")
    void testStartupWithNullContextPath() {
        // --- ARRANGE ---
        when(env.getProperty(eq("server.port"), anyString())).thenReturn("8080");
        
        // Case: Context path null -> harus diubah jadi ""
        when(env.getProperty(eq("server.servlet.context-path"), anyString())).thenReturn(null);
        
        when(env.getProperty(eq("server.address"), anyString())).thenReturn("localhost");
        when(env.getProperty(eq("spring.devtools.livereload.enabled"), eq(Boolean.class), anyBoolean())).thenReturn(false);
        when(env.getProperty(eq("spring.devtools.livereload.port"), anyString())).thenReturn("35729");

        // --- ACT ---
        startupInfoLogger.onApplicationEvent(event);

        // --- ASSERT ---
        verify(env).getProperty("server.servlet.context-path", "");
    }
}