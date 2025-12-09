package org.delcom.app.configs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class CustomErrorControllerTests {

    private CustomErrorController customErrorController;

    @Mock
    private ErrorAttributes errorAttributes;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        // Inisialisasi Controller dengan Mock ErrorAttributes
        customErrorController = new CustomErrorController(errorAttributes);
    }

    @Test
    @DisplayName("Should return 404 Not Found response correctly")
    void shouldReturnNotFoundResponse() {
        // Given (Siapkan data pura-pura dari ErrorAttributes)
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("status", 404);
        attributes.put("error", "Not Found");
        attributes.put("message", "Halaman tidak ditemukan");
        attributes.put("path", "/halaman-rahasia");

        // Mock behavior: Saat getErrorAttributes dipanggil, kembalikan map di atas
        // Kita pakai any() karena objek WebRequest dibuat baru di dalam method handleError
        given(errorAttributes.getErrorAttributes(any(WebRequest.class), any(ErrorAttributeOptions.class)))
                .willReturn(attributes);

        // When (Panggil method asli)
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Then (Verifikasi hasil)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Cek Body (Gunakan requireNonNull agar tidak ada warning kuning)
        Map<String, Object> body = Objects.requireNonNull(response.getBody());

        assertThat(body.get("status")).isEqualTo(404);
        assertThat(body.get("error")).isEqualTo("Not Found");
        assertThat(body.get("path")).isEqualTo("/halaman-rahasia");
        assertThat(body).containsKey("timestamp"); // Pastikan timestamp ada
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error correctly")
    void shouldReturnInternalServerErrorResponse() {
        // Given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("status", 500);
        attributes.put("error", "Internal Server Error");
        attributes.put("message", "Ada masalah di server");
        attributes.put("path", "/api/data");

        given(errorAttributes.getErrorAttributes(any(WebRequest.class), any(ErrorAttributeOptions.class)))
                .willReturn(attributes);

        // When
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        Map<String, Object> body = Objects.requireNonNull(response.getBody());
        assertThat(body.get("status")).isEqualTo(500);
        assertThat(body.get("error")).isEqualTo("Internal Server Error");
    }

    @Test
    @DisplayName("Should handle default values if attributes are missing")
    void shouldHandleDefaultValues() {
        // Given: ErrorAttributes mengembalikan map kosong (kasus ekstrem)
        Map<String, Object> emptyAttributes = new HashMap<>();
        
        given(errorAttributes.getErrorAttributes(any(WebRequest.class), any(ErrorAttributeOptions.class)))
                .willReturn(emptyAttributes);

        // When
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Then
        // Sesuai logika kode Anda: attributes.getOrDefault("status", 500)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR); // 500

        Map<String, Object> body = Objects.requireNonNull(response.getBody());
        assertThat(body.get("status")).isEqualTo(500);
        assertThat(body.get("message")).isEqualTo("Terjadi kesalahan pada server"); // Default message di kode Anda
        assertThat(body.get("path")).isEqualTo("unknown"); // Default path di kode Anda
    }
}