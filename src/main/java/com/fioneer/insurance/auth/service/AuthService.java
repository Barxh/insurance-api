package com.fioneer.insurance.auth.service;

import com.fioneer.insurance.auth.dto.AuthResponse;
import com.fioneer.insurance.auth.dto.LoginRequest;
import com.fioneer.insurance.auth.dto.RefreshRequest;
import com.fioneer.insurance.auth.dto.RegisterRequest;
import com.fioneer.insurance.auth.model.RefreshToken;
import com.fioneer.insurance.auth.repository.RefreshTokenRepository;
import com.fioneer.insurance.security.JwtService;
import com.fioneer.insurance.user.model.User;
import com.fioneer.insurance.user.model.Role;
import com.fioneer.insurance.user.repository.RoleRepository;
import com.fioneer.insurance.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expiresInSeconds;
    private final Duration refreshTokenTtl;


    public AuthService(
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${security.jwt.access-token-ttl}") String ttl,
            @Value("${security.jwt.refresh-token-ttl}") String refreshTokenTtl
    ) {
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.expiresInSeconds =
                java.time.Duration.parse(ttl).toSeconds();
        this.refreshTokenTtl = Duration.parse(refreshTokenTtl);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username is already taken"
            );
        }

        Role defaultRole = roleRepository.findByName("ROLE_CLIENT")
                .orElseThrow(() -> new IllegalStateException(
                        "Default role ROLE_CLIENT is not configured"
                ));

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .nationalId(request.nationalId())
                .street(request.street())
                .streetNumber(request.streetNumber())
                .zipCode(request.zipCode())
                .city(request.city())
                .roles(Set.of(defaultRole))
                .build();

        User savedUser = userRepository.save(user);

        return issueTokenPair(createAuthentication(savedUser), savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.username(),
                                request.password()
                        )
                );

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user was not found"
                ));

        return issueTokenPair(authentication, user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(this::invalidRefreshToken);

        if (!currentToken.getExpiresAt().isAfter(Instant.now())) {
            throw invalidRefreshToken();
        }

        if (refreshTokenRepository.deleteByTokenHash(tokenHash) != 1) {
            throw invalidRefreshToken();
        }

        User user = currentToken.getUser();
        return issueTokenPair(createAuthentication(user), user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.deleteByTokenHash(hashToken(request.refreshToken()));
    }

    private AuthResponse issueTokenPair(Authentication authentication, User user) {
        Instant now = Instant.now();
        String accessToken = jwtService.generateAccessToken(authentication);
        String refreshToken = generateRefreshToken();

        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setUser(user);
        tokenEntity.setTokenHash(hashToken(refreshToken));
        tokenEntity.setCreatedAt(now);
        tokenEntity.setExpiresAt(now.plus(refreshTokenTtl));
        refreshTokenRepository.save(tokenEntity);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresInSeconds,
                refreshTokenTtl.toSeconds()
        );
    }

    private Authentication createAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                user.getRoles().stream()
                        .map(Role::getName)
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
    }

    private String generateRefreshToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token is invalid, expired, or already used"
        );
    }
}
