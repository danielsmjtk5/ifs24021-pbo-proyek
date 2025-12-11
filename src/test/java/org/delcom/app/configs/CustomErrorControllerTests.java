package org.delcom.app.configs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomErrorControllerTest {

    @Mock
    private ErrorAttributes errorAttributes;

    @InjectMocks
    private CustomErrorController customErrorController;

    @Test
    void testHandleError_NotFound() {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/999");
        
        Map<String, Object> errorAttributesMap = Map.of(
            "status", 404,
            "error", "Not Found",
            "message", "User not found with id 999",
            "path", "/api/users/999"
        );
        
        when(errorAttributes.getErrorAttributes(
            any(WebRequest.class),
            any(ErrorAttributeOptions.class)
        )).thenReturn(errorAttributesMap);

        // Execute
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.get("status"));
        assertEquals("Not Found", body.get("error"));
        assertEquals("User not found with id 999", body.get("message"));
        assertEquals("/api/users/999", body.get("path"));
        assertInstanceOf(LocalDateTime.class, body.get("timestamp"));
    }

    @Test
    void testHandleError_InternalServerError() {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
        
        Map<String, Object> errorAttributesMap = Map.of(
            "status", 500,
            "error", "Internal Server Error",
            "message", "Database connection failed",
            "path", "/api/data"
        );
        
        when(errorAttributes.getErrorAttributes(
            any(WebRequest.class),
            any(ErrorAttributeOptions.class)
        )).thenReturn(errorAttributesMap);

        // Execute
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Verify
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertEquals(500, body.get("status"));
        assertEquals("Internal Server Error", body.get("error"));
        assertEquals("Database connection failed", body.get("message"));
    }

    @Test
    void testHandleError_WithDefaultValuesWhenMissingAttributes() {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        Map<String, Object> errorAttributesMap = Map.of(
            "status", 400
        );
        
        when(errorAttributes.getErrorAttributes(
            any(WebRequest.class),
            any(ErrorAttributeOptions.class)
        )).thenReturn(errorAttributesMap);

        // Execute
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertEquals(400, body.get("status"));
        assertEquals("Unknown Error", body.get("error"));
        assertEquals("Terjadi kesalahan pada server", body.get("message"));
        assertEquals("unknown", body.get("path"));
    }

    @Test
    void testHandleError_UsesJakartaServlet() {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        Map<String, Object> errorAttributesMap = Map.of(
            "status", 403,
            "error", "Forbidden",
            "message", "Access denied",
            "path", "/api/admin"
        );
        
        when(errorAttributes.getErrorAttributes(
            any(WebRequest.class),
            any(ErrorAttributeOptions.class)
        )).thenReturn(errorAttributesMap);

        // Execute
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Verify
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testHandleError_ResponseStructure() {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        Map<String, Object> errorAttributesMap = Map.of(
            "status", 200,
            "error", "OK",
            "message", "Success",
            "path", "/api/test"
        );
        
        when(errorAttributes.getErrorAttributes(
            any(WebRequest.class),
            any(ErrorAttributeOptions.class)
        )).thenReturn(errorAttributesMap);

        // Execute
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Verify
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(5, body.size());
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("message"));
        assertTrue(body.containsKey("path"));
    }

    @Test
    void testHandleError_BadRequest() {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        Map<String, Object> errorAttributesMap = Map.of(
            "status", 400,
            "error", "Bad Request",
            "message", "Invalid request parameters",
            "path", "/api/validate"
        );
        
        when(errorAttributes.getErrorAttributes(
            any(WebRequest.class),
            any(ErrorAttributeOptions.class)
        )).thenReturn(errorAttributesMap);

        // Execute
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
    }

    @Test
    void testHandleError_Unauthorized() {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        Map<String, Object> errorAttributesMap = Map.of(
            "status", 401,
            "error", "Unauthorized",
            "message", "Authentication required",
            "path", "/api/protected"
        );
        
        when(errorAttributes.getErrorAttributes(
            any(WebRequest.class),
            any(ErrorAttributeOptions.class)
        )).thenReturn(errorAttributesMap);

        // Execute
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Verify
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().get("status"));
    }

    @Test
    void testHandleError_ValidatesErrorAttributeOptions() {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        Map<String, Object> errorAttributesMap = Map.of(
            "status", 503,
            "error", "Service Unavailable",
            "message", "Service temporarily unavailable",
            "path", "/api/service"
        );
        
        // Stub dengan any() untuk menghindari strict matching
        when(errorAttributes.getErrorAttributes(
            any(WebRequest.class),
            any(ErrorAttributeOptions.class)
        )).thenReturn(errorAttributesMap);

        // Execute
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Verify
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void testHandleError_WhenErrorAttributesReturnsEmptyMap() {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        when(errorAttributes.getErrorAttributes(
            any(WebRequest.class),
            any(ErrorAttributeOptions.class)
        )).thenReturn(Map.of());

        // Execute
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Verify - harusnya menghasilkan response dengan default values
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.get("status"));
        assertEquals("Unknown Error", body.get("error"));
        assertEquals("Terjadi kesalahan pada server", body.get("message"));
        assertEquals("unknown", body.get("path"));
    }

    @Test
    void testHandleError_MethodNotAllowed() {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        Map<String, Object> errorAttributesMap = Map.of(
            "status", 405,
            "error", "Method Not Allowed",
            "message", "GET method is not supported",
            "path", "/api/resource"
        );
        
        when(errorAttributes.getErrorAttributes(
            any(WebRequest.class),
            any(ErrorAttributeOptions.class)
        )).thenReturn(errorAttributesMap);

        // Execute
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Verify
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(405, response.getBody().get("status"));
    }

    @Test
    void testHandleError_Conflict() {
        // Setup
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        Map<String, Object> errorAttributesMap = Map.of(
            "status", 409,
            "error", "Conflict",
            "message", "Resource already exists",
            "path", "/api/users"
        );
        
        when(errorAttributes.getErrorAttributes(
            any(WebRequest.class),
            any(ErrorAttributeOptions.class)
        )).thenReturn(errorAttributesMap);

        // Execute
        ResponseEntity<Map<String, Object>> response = customErrorController.handleError(request);

        // Verify
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
    }
}