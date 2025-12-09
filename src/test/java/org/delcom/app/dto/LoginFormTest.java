package org.delcom.app.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LoginFormTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        // Kita membangun Validator manual agar bisa mengetes anotasi @NotBlank dan @Email
        // tanpa perlu menjalankan seluruh Spring Boot (Unit Test murni -> Cepat)
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Harus VALID jika email dan password diisi dengan benar")
    void testValidLoginForm() {
        LoginForm form = new LoginForm();
        form.setEmail("user@example.com");
        form.setPassword("rahasia123");
        form.setRememberMe(true);

        // Lakukan validasi
        Set<ConstraintViolation<LoginForm>> violations = validator.validate(form);

        // Harusnya tidak ada error (violations kosong)
        assertTrue(violations.isEmpty(), "Form harusnya valid, tapi ditemukan error");
    }

    @Test
    @DisplayName("Harus ERROR jika email kosong")
    void testEmailBlank() {
        LoginForm form = new LoginForm();
        form.setEmail(""); // Kosong
        form.setPassword("pass");

        Set<ConstraintViolation<LoginForm>> violations = validator.validate(form);

        // Harusnya ada error
        assertFalse(violations.isEmpty(), "Harusnya error karena email kosong");
        
        // Cek pesan error spesifik
        boolean messageExists = violations.stream()
                .anyMatch(v -> v.getMessage().equals("Email harus diisi"));
        assertTrue(messageExists, "Pesan error 'Email harus diisi' tidak ditemukan");
    }

    @Test
    @DisplayName("Harus ERROR jika format email salah")
    void testInvalidEmailFormat() {
        LoginForm form = new LoginForm();
        form.setEmail("bukan-email"); // Format salah
        form.setPassword("pass");

        Set<ConstraintViolation<LoginForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
        
        // Cek pesan error dari @Email
        boolean messageExists = violations.stream()
                .anyMatch(v -> v.getMessage().equals("Format email tidak valid"));
        assertTrue(messageExists, "Validasi format email gagal terdeteksi");
    }

    @Test
    @DisplayName("Harus ERROR jika password kosong")
    void testPasswordBlank() {
        LoginForm form = new LoginForm();
        form.setEmail("user@example.com");
        form.setPassword(""); // Kosong

        Set<ConstraintViolation<LoginForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
        boolean messageExists = violations.stream()
                .anyMatch(v -> v.getMessage().equals("Kata sandi harus diisi"));
        assertTrue(messageExists);
    }

    @Test
    @DisplayName("Cek Getter dan Setter sederhana")
    void testGettersSetters() {
        LoginForm form = new LoginForm();
        form.setEmail("tes@mail.com");
        form.setPassword("123");
        form.setRememberMe(true);

        assertEquals("tes@mail.com", form.getEmail());
        assertEquals("123", form.getPassword());
        assertTrue(form.isRememberMe());
    }
}