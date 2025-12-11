package org.delcom.app.configs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser; // Import penting
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileSystemUtils; // Import untuk hapus folder aman

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebMvcConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final Path uploadsDir = Path.of("uploads");
    private final Path testFile = uploadsDir.resolve("test.txt");

    @BeforeEach
    void setUp() throws IOException {
        // Buat folder uploads jika belum ada
        if (!Files.exists(uploadsDir)) {
            Files.createDirectories(uploadsDir);
        }
        // Buat file dummy test.txt
        try (FileWriter writer = new FileWriter(testFile.toFile())) {
            writer.write("Dummy content");
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        // PERBAIKAN: Gunakan deleteRecursively dari Spring agar folder terhapus bersih
        // meskipun ada file sisa (menghindari DirectoryNotEmptyException)
        FileSystemUtils.deleteRecursively(uploadsDir);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER") // PERBAIKAN: Simulasi user login
    void testUploadsResourceHandlerServesFile() throws Exception {
        mockMvc.perform(get("/uploads/test.txt"))
                .andExpect(status().isOk()); // Sekarang harusnya 200 OK
    }
}