package org.delcom.app.configs;

import jakarta.servlet.http.HttpServletRequest; // PENTING: Spring Boot 3 pakai Jakarta
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes; // <--- PERBAIKAN IMPORT (Bukan webmvc)
import org.springframework.boot.web.servlet.error.ErrorController; // <--- PERBAIKAN IMPORT (Bukan webmvc)
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        // Kita konversi HttpServletRequest menjadi WebRequest agar bisa dibaca oleh ErrorAttributes
        WebRequest webRequest = new ServletWebRequest(request);

        // Mengambil atribut error
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(
                webRequest,
                ErrorAttributeOptions.defaults()
        );

        // Mengambil status code, default 500 jika tidak terbaca
        int status = (int) attributes.getOrDefault("status", 500);
        String path = (String) attributes.getOrDefault("path", "unknown");
        String errorMsg = (String) attributes.getOrDefault("error", "Unknown Error");
        Object messageObj = attributes.getOrDefault("message", "Terjadi kesalahan pada server");

        // Menyusun Body Response JSON
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status,
                "error", errorMsg,
                "message", messageObj,
                "path", path
        );

        return new ResponseEntity<>(body, HttpStatus.valueOf(status));
    }
}