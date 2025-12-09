package org.delcom.app.configs;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;  // <--- PENTING: Untuk tearDown
import org.junit.jupiter.api.DisplayName; // <--- PENTING: Untuk setUp
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTests {

    // @InjectMocks akan otomatis membuat instance class ini
    // dan menyuntikkan mock dependencies jika ada.
    // Ini juga mengatasi warning "Null type safety" pada inisialisasi manual.
    @InjectMocks
    private RequestLoggingFilter requestLoggingFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    // PERBAIKAN 1: Tambahkan @BeforeEach agar method ini jalan sebelum test
    @BeforeEach
    void setUp() {
        // Redirect System.out untuk menangkap log console
        System.setOut(new PrintStream(outContent));
    }

    // PERBAIKAN 2: Tambahkan @AfterEach agar method ini jalan setelah test
    @AfterEach
    void tearDown() {
        // Kembalikan System.out ke aslinya
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should pass request to the next filter in chain")
    void shouldPassRequestToNextFilter() throws Exception {
        // When
        // Memanggil method doFilter (standar untuk Servlet Filter / OncePerRequestFilter)
        requestLoggingFilter.doFilter(request, response, filterChain);

        // Then
        // 1. Pastikan filterChain.doFilter dipanggil (artinya request diteruskan, tidak macet)
        verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        
        // 2. (Opsional) Cek apakah ada log yang tercetak (jika filter Anda nge-print sesuatu)
        // String output = outContent.toString();
        // assertThat(output).isNotNull(); 
    }
}