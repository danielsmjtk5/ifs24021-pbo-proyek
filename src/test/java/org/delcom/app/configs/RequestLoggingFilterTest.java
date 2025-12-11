package org.delcom.app.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class) // Fitur untuk menangkap System.out
class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        // Karena ini Unit Test (bukan Integration), @Value tidak jalan otomatis.
        // Kita inject manual field private-nya.
        ReflectionTestUtils.setField(filter, "port", 8080);
        ReflectionTestUtils.setField(filter, "livereload", false);
    }

    @Test
    @DisplayName("Harus mencetak log warna HIJAU untuk status 200 OK")
    void testLogSuccessRequest(CapturedOutput output) throws ServletException, IOException {
        // 1. Setup Request & Response Mock
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse(); // Default status 200

        // 2. Setup FilterChain
        FilterChain filterChain = mock(FilterChain.class);
        
        // Simulasi chain berjalan lancar
        doAnswer(invocation -> {
            // Tidak mengubah status, jadi tetap 200
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        // 3. Jalankan Filter
        filter.doFilterInternal(request, response, filterChain);

        // 4. Verifikasi Output
        // Kode ANSI Hijau: \u001B[32m
        assertThat(output.getOut()).contains("\u001B[32m"); 
        assertThat(output.getOut()).contains("GET");
        assertThat(output.getOut()).contains("/api/users");
        assertThat(output.getOut()).contains("200");
    }

    @Test
    @DisplayName("Harus mencetak log warna KUNING untuk status 400-499")
    void testLogClientError(CapturedOutput output) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // Simulasi controller mengembalikan 404
        doAnswer(invocation -> {
            response.setStatus(404);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        // Kode ANSI Kuning: \u001B[33m
        assertThat(output.getOut()).contains("\u001B[33m");
        assertThat(output.getOut()).contains("404");
    }

    @Test
    @DisplayName("Harus mencetak log warna MERAH untuk status 500+")
    void testLogServerError(CapturedOutput output) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/error");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // Simulasi error server 500
        doAnswer(invocation -> {
            response.setStatus(500);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        // Kode ANSI Merah: \u001B[31m
        assertThat(output.getOut()).contains("\u001B[31m");
        assertThat(output.getOut()).contains("500");
    }

    @Test
    @DisplayName("Tidak boleh mencetak log jika URL dimulai dengan /.well-known")
    void testIgnoredUri(CapturedOutput output) throws ServletException, IOException {
        // URL yang sering dipanggil browser/bot, biasanya di-ignore agar log tidak penuh
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/security.txt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        // Assert bahwa TIDAK ADA output sama sekali
        assertThat(output.getOut()).isEmpty();
    }

    @Test
    @DisplayName("Harus mencetak log warna CYAN untuk status informasional (1xx)")
    void testLogInformationalStatus(CapturedOutput output) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/chat");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // Simulasi status 101 (Switching Protocols) -> Angka ini < 200
        doAnswer(invocation -> {
            response.setStatus(101); 
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        // Kode ANSI Cyan: \u001B[36m
        assertThat(output.getOut()).contains("\u001B[36m");
        assertThat(output.getOut()).contains("101");
    }
}