package com.fioneer.insurance.auth.controller;

import com.fioneer.insurance.api.error.GlobalExceptionHandler;
import com.fioneer.insurance.auth.dto.AuthResponse;
import com.fioneer.insurance.auth.dto.LoginRequest;
import com.fioneer.insurance.auth.dto.RefreshRequest;
import com.fioneer.insurance.auth.dto.RegisterRequest;
import com.fioneer.insurance.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerReturnsCreatedAndTokenPair() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(tokenResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void registerRejectsInvalidRequestAndReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"", "lastName":"N", "username":"abc", "password":"short", "email":"invalid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void loginReturnsTokenPair() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nikola\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void refreshReturnsUnauthorizedErrorForInvalidToken() throws Exception {
        when(authService.refresh(any(RefreshRequest.class))).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"expired\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Refresh token is invalid"))
                .andExpect(jsonPath("$.path").value("/api/auth/refresh"));
    }

    @Test
    void logoutReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isNoContent());

        verify(authService).logout(any(RefreshRequest.class));
    }

    @Test
    void malformedJsonUsesConsistentApiErrorResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Request body is missing, malformed, or contains an invalid value"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    private static AuthResponse tokenResponse() {
        return new AuthResponse("access", "refresh", "Bearer", 900, 2_592_000);
    }

    private static String validRegisterJson() {
        return """
                {"firstName":"Nikola", "lastName":"Nikolic", "username":"nikola", "password":"password123", "email":"nikola@example.com", "nationalId":"123", "street":"Main Street", "streetNumber":"1", "zipCode":"11000", "city":"Belgrade"}
                """;
    }
}
