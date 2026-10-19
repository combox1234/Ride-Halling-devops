package com.ridehailing.auth.service;

import com.ridehailing.auth.dto.AuthResponse;
import com.ridehailing.auth.dto.LoginRequest;
import com.ridehailing.auth.dto.RegisterRequest;
import com.ridehailing.auth.dto.TokenValidationResponse;
import com.ridehailing.auth.model.UserAccount;
import com.ridehailing.auth.model.UserRole;
import com.ridehailing.auth.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        UserRole assignedRole = resolvePublicRole(request.role());

        String passwordHash = passwordEncoder.encode(request.password());
        UserAccount newUser = new UserAccount(
            null,
            request.name(),
            request.email().toLowerCase().trim(),
            passwordHash,
            assignedRole,
            Instant.now()
        );

        UserAccount saved = userRepository.save(newUser);
        String token = jwtTokenService.generateToken(saved);

        return AuthResponse.bearer(
            token,
            saved.id(),
            saved.email(),
            saved.role(),
            jwtTokenService.getExpirationMs()
        );
    }

    /**
     * Resolves the user role for public registration requests.
     * Prevents privilege escalation by strictly forbidding client assignment of ROLE_ADMIN.
     * If no role is requested, defaults to ROLE_PASSENGER.
     */
    private UserRole resolvePublicRole(UserRole requestedRole) {
        if (requestedRole == UserRole.ROLE_ADMIN) {
            throw new IllegalArgumentException("ROLE_ADMIN cannot be self-assigned through public registration");
        }
        if (requestedRole == null) {
            return UserRole.ROLE_PASSENGER;
        }
        if (!requestedRole.isPubliclyRegistrable()) {
            throw new IllegalArgumentException("Invalid role for public registration: " + requestedRole);
        }
        return requestedRole;
    }

    /**
     * Trusted administrative provisioning mechanism.
     * Administrative accounts can only be provisioned through this internal mechanism
     * (e.g., system bootstrapping, secure administrative CLI, or internal database seeders),
     * never through the public registration endpoint.
     */
    @Transactional
    public UserAccount provisionAdmin(String name, String email, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Admin email must not be blank");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Admin password must be at least 6 characters");
        }
        String normalizedEmail = email.toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered: " + normalizedEmail);
        }

        String passwordHash = passwordEncoder.encode(password);
        UserAccount adminUser = new UserAccount(
            null,
            name != null && !name.isBlank() ? name.trim() : "Administrator",
            normalizedEmail,
            passwordHash,
            UserRole.ROLE_ADMIN,
            Instant.now()
        );

        return userRepository.save(adminUser);
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByEmail(request.email().toLowerCase().trim())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtTokenService.generateToken(user);
        return AuthResponse.bearer(
            token,
            user.id(),
            user.email(),
            user.role(),
            jwtTokenService.getExpirationMs()
        );
    }

    public TokenValidationResponse validateToken(String authHeader) {
        return jwtTokenService.validateAndExtractClaims(authHeader)
            .map(claims -> {
                Long userId = Long.valueOf(claims.get("sub").toString());
                String email = (String) claims.get("email");
                UserRole role = UserRole.valueOf((String) claims.get("role"));
                return TokenValidationResponse.valid(userId, email, role);
            })
            .orElseGet(TokenValidationResponse::invalid);
    }
}
