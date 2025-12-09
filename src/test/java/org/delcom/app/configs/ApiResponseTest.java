package org.delcom.app.configs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Pastikan class ini public
public class ApiResponseTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange
        String status = "success";
        String message = "Data fetched successfully";
        String data = "Test Data";

        // Act
        ApiResponse<String> response = new ApiResponse<>(status, message, data);

        // Assert
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("Data fetched successfully", response.getMessage());
        assertEquals("Test Data", response.getData());
    }

    @Test
    void testWithNullData() {
        // Test kasus jika data null (misal error response)
        ApiResponse<Object> response = new ApiResponse<>("error", "Not Found", null);

        assertEquals("error", response.getStatus());
        assertNull(response.getData());
    }

    @Test
    void testWithIntegerData() {
        // Test generic dengan tipe Integer
        ApiResponse<Integer> response = new ApiResponse<>("ok", "Number", 12345);

        assertEquals(12345, response.getData());
    }
}
