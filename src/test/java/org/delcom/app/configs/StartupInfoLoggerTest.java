package org.delcom.app.configs;

import java.io.ByteArrayOutputStream; // Tambahkan ini
import java.io.PrintStream; // Tambahkan ini

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
// Hapus import static org.mockito.ArgumentMatchers.eq; karena tidak kita perlukan lagi agar warning hilang

@ExtendWith(MockitoExtension.class)
class StartupInfoLoggerTest {

    @InjectMocks
    private StartupInfoLogger startupInfoLogger;

    @Mock
    private ApplicationReadyEvent event;

    @Mock
    private ConfigurableApplicationContext context;

    @Mock
    private ConfigurableEnvironment environment;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    // PERBAIKAN 1: Tambahkan @BeforeEach
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        given(event.getApplicationContext()).willReturn(context);
        given(context.getEnvironment()).willReturn(environment);
    }

    // PERBAIKAN 2: Tambahkan @AfterEach
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should log correct URL with default configuration")
    void shouldLogDefaultConfiguration() {
        // Given
        given(environment.getProperty("server.port", "8080")).willReturn("8080");
        given(environment.getProperty("server.servlet.context-path", "/")).willReturn("/");
        given(environment.getProperty("server.address", "localhost")).willReturn("localhost");
        
        // PERBAIKAN 3: Hapus eq(...) untuk mengatasi "Null type safety"
        // Langsung tulis valuenya, Mockito otomatis menganggapnya sebagai 'eq'
        given(environment.getProperty("spring.devtools.livereload.enabled", Boolean.class, false)).willReturn(false);

        // When
        startupInfoLogger.onApplicationEvent(event);

        // Then
        String output = outContent.toString();
        assertThat(output).contains("http://localhost:8080");
        assertThat(output).doesNotContain(":8080/");
        assertThat(output).contains("LiveReload: DISABLED");
    }

    @Test
    @DisplayName("Should log custom configuration")
    void shouldLogCustomConfiguration() {
        // Given
        given(environment.getProperty("server.port", "8080")).willReturn("9090");
        given(environment.getProperty("server.servlet.context-path", "/")).willReturn("/api");
        given(environment.getProperty("server.address", "localhost")).willReturn("127.0.0.1");
        
        // Hapus eq(...) di sini juga
        given(environment.getProperty("spring.devtools.livereload.enabled", Boolean.class, false)).willReturn(false);

        // When
        startupInfoLogger.onApplicationEvent(event);

        // Then
        String output = outContent.toString();
        assertThat(output).contains("http://127.0.0.1:9090/api");
    }

    @Test
    @DisplayName("Should log LiveReload enabled")
    void shouldLogLiveReloadEnabled() {
        // Given
        given(environment.getProperty("server.port", "8080")).willReturn("8080");
        given(environment.getProperty("server.servlet.context-path", "/")).willReturn("/");
        given(environment.getProperty("server.address", "localhost")).willReturn("localhost");

        // Hapus eq(...) di sini juga
        given(environment.getProperty("spring.devtools.livereload.enabled", Boolean.class, false)).willReturn(true);
        given(environment.getProperty("spring.devtools.livereload.port", "35729")).willReturn("12345");

        // When
        startupInfoLogger.onApplicationEvent(event);

        // Then
        String output = outContent.toString();
        assertThat(output).contains("LiveReload: ENABLED");
        assertThat(output).contains("port 12345");
    }
}