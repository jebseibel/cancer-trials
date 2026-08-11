package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.UserDb;
import com.seibel.cancer.database.db.repository.UserRepository;
import com.seibel.cancer.security.JwtUtil;
import com.seibel.cancer.web.request.RequestLogin;
import com.seibel.cancer.web.request.RequestRegister;
import com.seibel.cancer.web.response.ResponseAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Validated
@Tag(name = "Authentication", description = "Authentication endpoints")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public ResponseEntity<?> login(@Valid @RequestBody RequestLogin request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        final String token = jwtUtil.generateToken(userDetails);

        UserDb user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ResponseAuth response = ResponseAuth.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Creates a user. <b>Requires an existing ADMIN.</b>
     *
     * <p>This was anonymous, and that was the most serious hole in the application: a stranger
     * could POST here, receive a valid JWT, and read everything — because no endpoint checks
     * <em>whose</em> data it is serving (see the authorization gap in CURRENT_STATE.md).
     * Authentication was restored on 2026-08-11 and bought nothing while anyone could mint
     * themselves an account. Verified by creating one against the running app.
     *
     * <p>This is a single-patient tool. Open registration is a liability with no upside, so the
     * endpoint is kept for creating accounts deliberately rather than deleted outright.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> register(@Valid @RequestBody RequestRegister request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }

        UserDb user = new UserDb();
        user.setExtid(UUID.randomUUID().toString());
        user.setUsername(request.getUsername());
        // The bcrypt hash was logged at WARN here to capture it for CSV seeding. A password
        // hash in a log file is a credential sitting in plaintext on disk and in any log
        // shipper - removed rather than downgraded to debug.
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);
        user.setEmail(request.getEmail());
        user.setRole("USER");
        user.setActive(ActiveEnum.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        final String token = jwtUtil.generateToken(userDetails);

        ResponseAuth response = ResponseAuth.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
