package org.delcom.app.configs;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomErrorControllerTest {

    @Mock
    private ErrorAttributes errorAttributes; // Mock dependencies

    @InjectMocks
    private CustomErrorController customErrorController; // Inject mock ke controller

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        // MockHttpServletRequest adalah object palsu yang disediakan Spring untuk testing
        request = new MockHttpServletRequest();
    }

    @Test
    void testHandleError_NotFound_404() {
        // 1. Arrange (Siapkan skenario error)
        Map<String, Object> mockAttributes = new HashMap<>();
        mockAttributes.put("status", 404);
        mockAttributes.put("error", "Not Found");
        mockAttributes.put("message", "Halaman tidak ditemukan");
        mockAttributes.put("path", "/halaman-hilang");

        // Ketika errorAttributes dipanggil, kembalikan map di atas
        when(errorAttributes.getErrorAttributes(any(WebRequest.class), any(ErrorAttributeOptions.class)))
                .thenReturn(mockAttributes);

        // 2. Act (Jalankan method)
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // 3. Assert (Cek hasil)
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode()); // Cek status code
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Not Found", response.getBody().get("error"));
        assertEquals("Halaman tidak ditemukan", response.getBody().get("message"));
        assertEquals("/halaman-hilang", response.getBody().get("path"));
        assertNotNull(response.getBody().get("timestamp")); // Pastikan timestamp ada
    }

    @Test
    void testHandleError_InternalServerError_500() {
        // 1. Arrange (Skenario error server, misal map kosong/default)
        Map<String, Object> mockAttributes = new HashMap<>();
        // Jika ErrorAttributes tidak mengembalikan status, kodemu default ke 500
        mockAttributes.put("message", "Something went wrong");

        when(errorAttributes.getErrorAttributes(any(WebRequest.class), any(ErrorAttributeOptions.class)))
                .thenReturn(mockAttributes);

        // 2. Act
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // 3. Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Terjadi kesalahan pada server", response.getBody().get("message")); // Cek fallback message
    }
}