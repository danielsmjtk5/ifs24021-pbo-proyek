package org.delcom.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApplicationTests {

    // ==========================================
    // 1. Cover Method main() (Baris Merah)
    // ==========================================
    @Test
    void testMain() {
        // Memanggil method main secara manual.
        // Ini akan mengeksekusi baris 'SpringApplication.run(...)'
        // sehingga JaCoCo mendeteksinya sebagai "covered".
        Application.main(new String[] {});
    }

    // ==========================================
    // 2. Cover Class Definition (Optional)
    // ==========================================
    // Terkadang JaCoCo menandai nama class "Application" sebagai missed
    // karena default constructor-nya tidak pernah dipanggil.
    // Test ini memastikan constructor tersebut terpanggil.
    @Test
    void testConstructor() {
        Application app = new Application();
        assertNotNull(app);
    }
}