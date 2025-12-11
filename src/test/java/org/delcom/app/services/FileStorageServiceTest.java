package org.delcom.app.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class FileStorageServiceTest {

    private FileStorageService fileService;

    // JUnit 5 akan membuatkan folder sementara yang otomatis dihapus setelah test selesai
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileService = new FileStorageService();
        // Kita suntikkan path folder sementara ke variable 'uploadDir' milik service
        // Ini menggantikan fungsi @Value("${app.upload.dir}")
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
    }

    // ==========================================
    // 1. TEST STORE FILE (Normal & No Extension)
    // ==========================================

    @Test
    @DisplayName("Store File: Success with Extension")
    void testStoreFile_WithExtension() throws IOException {
        // Setup
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "photo", "avatar.png", "image/png", "dummy-content".getBytes());

        // Act
        String resultFilename = fileService.storeFile(file, id);

        // Assert
        // Harusnya: cover_UUID.png
        String expectedFilename = "cover_" + id.toString() + ".png";
        assertEquals(expectedFilename, resultFilename);

        // Cek apakah file benar-benar ada di disk (di folder temp)
        Path savedPath = tempDir.resolve(resultFilename);
        assertTrue(Files.exists(savedPath));
    }

    @Test
    @DisplayName("Store File: Success without Extension")
    void testStoreFile_NoExtension() throws IOException {
        // Setup (Nama file tidak ada titiknya)
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "photo", "avatar", "image/png", "dummy-content".getBytes());

        // Act
        String resultFilename = fileService.storeFile(file, id);

        // Assert
        // Harusnya: cover_UUID (tanpa ekstensi)
        String expectedFilename = "cover_" + id.toString();
        assertEquals(expectedFilename, resultFilename);
        assertTrue(Files.exists(tempDir.resolve(resultFilename)));
    }

    @Test
    @DisplayName("Store File: Creates Directory if not exists")
    void testStoreFile_CreatesDirectory() throws IOException {
        // Setup: Kita arahkan uploadDir ke subfolder yang BELUM ada
        Path nonExistentSubDir = tempDir.resolve("sub-uploads");
        ReflectionTestUtils.setField(fileService, "uploadDir", nonExistentSubDir.toString());

        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("photo", "test.txt", "text/plain", "content".getBytes());

        // Act
        // Service harus membuat folder 'sub-uploads' dulu, baru simpan file
        fileService.storeFile(file, id);

        // Assert
        assertTrue(Files.exists(nonExistentSubDir), "Directory harusnya dibuat otomatis");
        assertTrue(Files.exists(nonExistentSubDir.resolve("cover_" + id + ".txt")));
    }

    // ==========================================
    // 2. TEST LOAD & EXISTS
    // ==========================================

    @Test
    void testLoadAndFileExists() throws IOException {
        String filename = "test-load.txt";
        Path filePath = tempDir.resolve(filename);
        Files.createFile(filePath);

        // Test loadFile
        Path loadedPath = fileService.loadFile(filename);
        assertEquals(filePath.toAbsolutePath().toString(), loadedPath.toAbsolutePath().toString());

        // Test fileExists (True)
        assertTrue(fileService.fileExists(filename));

        // Test fileExists (False)
        assertFalse(fileService.fileExists("ghost-file.txt"));
    }

    // ==========================================
    // 3. TEST DELETE FILE (Success & IOException)
    // ==========================================

    @Test
    @DisplayName("Delete File: Success")
    void testDeleteFile_Success() throws IOException {
        String filename = "to-delete.txt";
        Files.createFile(tempDir.resolve(filename));

        // Act
        boolean result = fileService.deleteFile(filename);

        // Assert
        assertTrue(result);
        assertFalse(Files.exists(tempDir.resolve(filename)));
    }

    @Test
    @DisplayName("Delete File: Returns False if not exists")
    void testDeleteFile_NotFound() {
        boolean result = fileService.deleteFile("ghost.txt");
        assertFalse(result); // Files.deleteIfExists return false jika file ga ada
    }

    @Test
    @DisplayName("Delete File: IOException Catch Block")
    void testDeleteFile_IOException() throws IOException {
        // TRICKY PART: Bagaimana memicu IOException saat delete?
        // Cara paling ampuh: Buat sebuah DIRECTORY dengan nama target, 
        // lalu isi direktori itu. 'Files.deleteIfExists' akan gagal menghapus
        // direktori yang tidak kosong dan melempar DirectoryNotEmptyException (turunan IOException).
        
        String folderName = "protected-folder";
        Path folderPath = tempDir.resolve(folderName);
        Files.createDirectory(folderPath);
        
        // Isi folder agar tidak bisa dihapus dengan deleteIfExists biasa
        Files.createFile(folderPath.resolve("child.txt")); 

        // Act
        // Kita minta service menghapus "protected-folder". 
        // Karena itu folder berisi file, Java akan melempar Exception.
        // Blok catch (IOException e) di service akan menangkapnya dan return false.
        boolean result = fileService.deleteFile(folderName);

        // Assert
        assertFalse(result, "Harusnya return false karena terjadi IOException");
    }
}