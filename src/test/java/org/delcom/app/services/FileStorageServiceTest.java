package org.delcom.app.services;

import java.io.ByteArrayInputStream;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class FileStorageServiceTest {

    private FileStorageService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileService = new FileStorageService();
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
    }

    // ==========================================
    // 1. TEST STORE FILE (Fokus Coverage)
    // ==========================================

    @Test
    @DisplayName("Store File: Normal (Ada Ekstensi) -> Masuk blok IF")
    void testStoreFile_WithExtension() throws IOException {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "photo", "avatar.png", "image/png", "content".getBytes());

        String result = fileService.storeFile(file, id);

        assertEquals("cover_" + id + ".png", result);
        assertTrue(Files.exists(tempDir.resolve(result)));
    }

    @Test
    @DisplayName("Store File: Tanpa Ekstensi -> Tidak masuk blok IF (Kanan False)")
    void testStoreFile_NoExtension() throws IOException {
        UUID id = UUID.randomUUID();
        // "avatar" tidak punya titik
        MockMultipartFile file = new MockMultipartFile(
                "photo", "avatar", "image/png", "content".getBytes());

        String result = fileService.storeFile(file, id);

        // Ekstensi kosong
        assertEquals("cover_" + id, result);
        assertTrue(Files.exists(tempDir.resolve(result)));
    }

    @Test
    @DisplayName("Store File: Filename NULL -> Tidak masuk blok IF (Kiri False)")
    void testStoreFile_NullFilename() throws IOException {
        // --- PERBAIKAN UTAMA DI SINI ---
        // Kita TIDAK BISA pakai MockMultipartFile karena dia mengubah null jadi ""
        // Kita harus pakai Mockito untuk memaksa return null asli.
        
        UUID id = UUID.randomUUID();
        MultipartFile mockFile = mock(MultipartFile.class);
        
        // Paksa return null
        when(mockFile.getOriginalFilename()).thenReturn(null);
        // Mock getInputStream supaya tidak Error NullPointerException saat copy
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("dummy".getBytes()));

        // Act
        String result = fileService.storeFile(mockFile, id);

        // Assert
        // Karena null, ekstensi defaultnya "" (kosong)
        assertEquals("cover_" + id, result);
        assertTrue(Files.exists(tempDir.resolve(result)));
    }

    @Test
    @DisplayName("Store File: Auto Create Directory")
    void testStoreFile_CreatesDirectory() throws IOException {
        Path subDir = tempDir.resolve("new-folder");
        ReflectionTestUtils.setField(fileService, "uploadDir", subDir.toString());

        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("photo", "t.txt", "text/plain", "b".getBytes());

        fileService.storeFile(file, id);

        assertTrue(Files.exists(subDir));
        assertTrue(Files.exists(subDir.resolve("cover_" + id + ".txt")));
    }

    // ==========================================
    // 2. TEST LOAD & EXISTS
    // ==========================================

    @Test
    void testLoadAndFileExists() throws IOException {
        String filename = "data.txt";
        Files.createFile(tempDir.resolve(filename));

        Path path = fileService.loadFile(filename);
        assertEquals(tempDir.resolve(filename).toAbsolutePath().toString(), path.toAbsolutePath().toString());

        assertTrue(fileService.fileExists(filename));
        assertFalse(fileService.fileExists("missing.txt"));
    }

    // ==========================================
    // 3. TEST DELETE FILE
    // ==========================================

    @Test
    void testDeleteFile_Success() throws IOException {
        String filename = "del.txt";
        Files.createFile(tempDir.resolve(filename));

        assertTrue(fileService.deleteFile(filename));
        assertFalse(Files.exists(tempDir.resolve(filename)));
    }

    @Test
    void testDeleteFile_NotFound() {
        assertFalse(fileService.deleteFile("ghost.txt"));
    }

    @Test
    void testDeleteFile_IOException() throws IOException {
        String folderName = "protected";
        Path p = tempDir.resolve(folderName);
        Files.createDirectory(p);
        Files.createFile(p.resolve("child.txt")); 

        assertFalse(fileService.deleteFile(folderName));
    }
}