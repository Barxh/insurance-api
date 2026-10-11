package com.fioneer.insurance.auth.service;

import com.fioneer.insurance.auth.dto.AuthResponse;
import com.fioneer.insurance.auth.dto.LoginRequest;
import com.fioneer.insurance.auth.dto.RefreshRequest;
import com.fioneer.insurance.auth.dto.RegisterRequest;
import com.fioneer.insurance.auth.model.RefreshToken;
import com.fioneer.insurance.auth.repository.RefreshTokenRepository;
import com.fioneer.insurance.security.JwtService;
import com.fioneer.insurance.user.model.Role;
import com.fioneer.insurance.user.model.User;
import com.fioneer.insurance.user.repository.RoleRepository;
import com.fioneer.insurance.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String USERNAME = "client1";
    private static final String RAW_REFRESH_TOKEN = "known-refresh-token";

    @Mock private RoleRepository roles;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository users;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService service;
    private Role clientRole;
    private User client;

    @BeforeEach
    void setUp() {
        service = new AuthService(roles, refreshTokens, authenticationManager, users,
                passwordEncoder, jwtService, "PT15M", "P30D");
        clientRole = Role.builder().id(1L).name("ROLE_CLIENT").build();
        client = User.builder().id(7L).username(USERNAME).password("encoded-password")
                .roles(Set.of(clientRole)).build();
        lenient().when(jwtService.generateAccessToken(any(Authentication.class))).thenReturn("access-token");
        lenient().when(refreshTokens.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registerCreatesClientAndReturnsBothTokens() {
        RegisterRequest request = new RegisterRequest("Nikola", "Nikolic", USERNAME,
                "raw-password", "nikola@example.com", "123", "Main Street", "1", "11000", "Belgrade");
        when(users.existsByUsername(USERNAME)).thenReturn(false);
        when(roles.findByName("ROLE_CLIENT")).thenReturn(Optional.of(clientRole));
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = service.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("Nikola", savedUser.getFirstName());
        assertEquals("Nikolic", savedUser.getLastName());
        assertEquals(USERNAME, savedUser.getUsername());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals("nikola@example.com", savedUser.getEmail());
        assertEquals("123", savedUser.getNationalId());
        assertEquals("Main Street", savedUser.getStreet());
        assertEquals("1", savedUser.getStreetNumber());
        assertEquals("11000", savedUser.getZipCode());
        assertEquals("Belgrade", savedUser.getCity());
        assertEquals(Set.of(clientRole), savedUser.getRoles());
        assertTokenPair(response);
        assertEquals(900, response.expiresIn());
        assertEquals(30L * 24 * 60 * 60, response.refreshExpiresIn());
        verify(jwtService).generateAccessToken(argThat(auth -> auth.getName().equals(USERNAME)));
        verify(refreshTokens).save(argThat(token -> token.getUser() == savedUser
                && token.getTokenHash().equals(sha256(response.refreshToken()))
                && token.getExpiresAt().isAfter(token.getCreatedAt())));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(users.existsByUsername(USERNAME)).thenReturn(true);
        RegisterRequest request = registerRequest();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.register(request));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(roles, never()).findByName(any());
        verify(users, never()).save(any());
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void registerFailsWhenDefaultClientRoleIsNotConfigured() {
        when(users.existsByUsername(USERNAME)).thenReturn(false);
        when(roles.findByName("ROLE_CLIENT")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.register(registerRequest()));

        verify(users, never()).save(any());
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void loginAuthenticatesCredentialsAndIssuesTokenPair() {
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                USERNAME, null, Set.of(() -> "ROLE_CLIENT"));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(users.findByUsername(USERNAME)).thenReturn(Optional.of(client));

        AuthResponse response = service.login(new LoginRequest(USERNAME, "secret"));

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        UsernamePasswordAuthenticationToken submitted =
                (UsernamePasswordAuthenticationToken) authenticationCaptor.getValue();
        assertEquals(USERNAME, submitted.getPrincipal());
        assertEquals("secret", submitted.getCredentials());
        assertTokenPair(response);
        verify(users).findByUsername(USERNAME);
        verify(refreshTokens).save(argThat(token -> token.getUser() == client));
    }

    @Test
    void loginPropagatesAuthenticationFailure() {
        var authenticationFailure = new org.springframework.security.authentication.BadCredentialsException("bad credentials");
        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(authenticationFailure);

        assertSame(authenticationFailure, assertThrows(
                org.springframework.security.authentication.BadCredentialsException.class,
                () -> service.login(new LoginRequest(USERNAME, "wrong"))));

        verify(users, never()).findByUsername(any());
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void refreshConsumesOldTokenAndIssuesReplacement() {
        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(client);
        oldToken.setTokenHash(sha256(RAW_REFRESH_TOKEN));
        oldToken.setCreatedAt(Instant.now().minusSeconds(60));
        oldToken.setExpiresAt(Instant.now().plusSeconds(600));
        when(refreshTokens.findByTokenHash(sha256(RAW_REFRESH_TOKEN))).thenReturn(Optional.of(oldToken));
        when(refreshTokens.deleteByTokenHash(sha256(RAW_REFRESH_TOKEN))).thenReturn(1);

        AuthResponse response = service.refresh(new RefreshRequest(RAW_REFRESH_TOKEN));

        verify(refreshTokens).deleteByTokenHash(sha256(RAW_REFRESH_TOKEN));
        assertTokenPair(response);
        assertNotEquals(RAW_REFRESH_TOKEN, response.refreshToken());
        verify(refreshTokens).save(argThat(token -> token.getUser() == client
                && token.getTokenHash().equals(sha256(response.refreshToken()))));
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(refreshTokens.findByTokenHash(sha256(RAW_REFRESH_TOKEN))).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.refresh(new RefreshRequest(RAW_REFRESH_TOKEN)));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verify(refreshTokens, never()).deleteByTokenHash(any());
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void refreshRejectsExpiredTokenWithoutDeletingOrIssuingTokens() {
        RefreshToken expired = new RefreshToken();
        expired.setUser(client);
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(refreshTokens.findByTokenHash(sha256(RAW_REFRESH_TOKEN))).thenReturn(Optional.of(expired));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.refresh(new RefreshRequest(RAW_REFRESH_TOKEN)));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verify(refreshTokens, never()).deleteByTokenHash(any());
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void refreshRejectsTokenWhenConcurrentUseAlreadyConsumedIt() {
        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(client);
        oldToken.setExpiresAt(Instant.now().plusSeconds(600));
        when(refreshTokens.findByTokenHash(sha256(RAW_REFRESH_TOKEN))).thenReturn(Optional.of(oldToken));
        when(refreshTokens.deleteByTokenHash(sha256(RAW_REFRESH_TOKEN))).thenReturn(0);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.refresh(new RefreshRequest(RAW_REFRESH_TOKEN)));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void logoutDeletesHashedRefreshToken() {
        service.logout(new RefreshRequest(RAW_REFRESH_TOKEN));

        verify(refreshTokens).deleteByTokenHash(sha256(RAW_REFRESH_TOKEN));
        verify(refreshTokens, never()).save(any());
    }

    private RegisterRequest registerRequest() {
        return new RegisterRequest("Nikola", "Nikolic", USERNAME, "raw-password",
                "nikola@example.com", null, null, null, null, null);
    }

    private void assertTokenPair(AuthResponse response) {
        assertEquals("access-token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertTrue(response.refreshToken().length() >= 40);
        assertEquals("Bearer", response.tokenType());
    }

    private static String sha256(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
