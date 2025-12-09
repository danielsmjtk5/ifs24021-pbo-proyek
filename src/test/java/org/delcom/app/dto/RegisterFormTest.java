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

class RegisterFormTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        // Menyiapkan validator manual untuk Unit Test
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Validasi SUKSES jika semua data diisi dengan benar")
    void testValidRegisterForm() {
        RegisterForm form = new RegisterForm();
        form.setName("Budi Santoso");
        form.setEmail("budi@example.com");
        form.setPassword("passwordKuat123");

        Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);

        // Harapannya tidak ada error (set kosong)
        assertTrue(violations.isEmpty(), "Form harusnya valid, tapi ada error");
    }

    @Test
    @DisplayName("Validasi GAGAL jika Nama kosong")
    void testNameBlank() {
        RegisterForm form = new RegisterForm();
        form.setName(""); // Kosong
        form.setEmail("valid@email.com");
        form.setPassword("pass");

        Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Nama harus diisi")));
    }

    @Test
    @DisplayName("Validasi GAGAL jika Email kosong")
    void testEmailBlank() {
        RegisterForm form = new RegisterForm();
        form.setName("Budi");
        form.setEmail(""); // Kosong
        form.setPassword("pass");

        Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Email harus diisi")));
    }

    @Test
    @DisplayName("Validasi GAGAL jika Format Email salah")
    void testInvalidEmailFormat() {
        RegisterForm form = new RegisterForm();
        form.setName("Budi");
        form.setEmail("budi-bukan-email"); // Format salah
        form.setPassword("pass");

        Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Format email tidak valid")));
    }

    @Test
    @DisplayName("Validasi GAGAL jika Password kosong")
    void testPasswordBlank() {
        RegisterForm form = new RegisterForm();
        form.setName("Budi");
        form.setEmail("budi@email.com");
        form.setPassword(""); // Kosong

        Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Kata sandi harus diisi")));
    }

    @Test
    @DisplayName("Test Getter dan Setter berfungsi normal")
    void testGettersSetters() {
        RegisterForm form = new RegisterForm();
        form.setName("Tes Nama");
        form.setEmail("tes@mail.com");
        form.setPassword("rahasia");

        assertEquals("Tes Nama", form.getName());
        assertEquals("tes@mail.com", form.getEmail());
        assertEquals("rahasia", form.getPassword());
    }
}