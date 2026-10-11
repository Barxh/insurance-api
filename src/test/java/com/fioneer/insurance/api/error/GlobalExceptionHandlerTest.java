package com.fioneer.insurance.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "/api/test");
        mockRequest.setRequestURI("/api/test");
        request = mockRequest;
    }

    @Test
    void authenticationFailuresMapToUnauthorized() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAuthenticationException(
                new BadCredentialsException("password details must not leak"), request);

        assertError(response, HttpStatus.UNAUTHORIZED,
                "Authentication is required or the supplied credentials are invalid");
    }

    @Test
    void accessDeniedMapsToForbidden() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("private detail"), request);

        assertError(response, HttpStatus.FORBIDDEN,
                "You are not allowed to perform this action");
    }

    @Test
    void databaseConstraintConflictMapsToConflict() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("database internals"), request);

        assertError(response, HttpStatus.CONFLICT,
                "Request conflicts with existing data or a database constraint");
    }

    @Test
    void unexpectedErrorsAreSanitized() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpectedException(
                new IllegalStateException("sensitive internal details"), request);

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }

    private static void assertError(
            ResponseEntity<ApiErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedMessage
    ) {
        assertEquals(expectedStatus, response.getStatusCode());
        ApiErrorResponse body = response.getBody();
        assertEquals(expectedStatus.value(), body.status());
        assertEquals(expectedStatus.getReasonPhrase(), body.error());
        assertEquals(expectedMessage, body.message());
        assertEquals("/api/test", body.path());
        assertEquals(0, body.fieldErrors().size());
        assertEquals(false, body.timestamp() == null);
    }
}
