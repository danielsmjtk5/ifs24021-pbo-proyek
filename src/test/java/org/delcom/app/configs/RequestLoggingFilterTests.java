package org.delcom.app.configs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream standardOut = System.out;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        
        // Menyuntikkan nilai field private @Value secara manual
        ReflectionTestUtils.setField(filter, "port", 8080);
        ReflectionTestUtils.setField(filter, "livereload", false);

        // Meredirect output console agar bisa kita baca di test
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        // Mengembalikan output console ke semula setelah test selesai
        System.setOut(standardOut);
    }

    @Test
    void testDoFilter_Success200() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // Simulasi bahwa request berjalan lancar dan set status 200
        doAnswer(invocation -> {
            response.setStatus(200);
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        String output = outputStreamCaptor.toString();
        // Pastikan log berisi informasi yang benar
        assertTrue(output.contains("GET"), "Harus ada method GET");
        assertTrue(output.contains("/api/users"), "Harus ada URI");
        assertTrue(output.contains("200"), "Harus ada status code 200");
        assertTrue(output.contains("\u001B[32m"), "Harus berwarna HIJAU (Green) untuk 200 OK");
    }

    @Test
    void testDoFilter_Error500() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/checkout");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // Simulasi request error 500
        doAnswer(invocation -> {
            response.setStatus(500);
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("POST"), "Harus ada method POST");
        assertTrue(output.contains("500"), "Harus ada status code 500");
        assertTrue(output.contains("\u001B[31m"), "Harus berwarna MERAH (Red) untuk error 500");
    }

    @Test
    void testDoFilter_IgnoreWellKnown() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/acme-challenge");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        String output = outputStreamCaptor.toString();
        // Log harus kosong karena URI dimulai dengan /.well-known
        assertTrue(output.isEmpty(), "Tidak boleh ada log untuk /.well-known");
    }
}