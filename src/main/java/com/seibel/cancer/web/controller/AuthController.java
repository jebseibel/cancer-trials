package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.UserDb;
import com.seibel.cancer.database.db.repository.UserRepository;
import com.seibel.cancer.security.JwtUtil;
import com.seibel.cancer.web.request.RequestChangePassword;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * Changes the signed-in user's password.
     *
     * <p>The user comes from the security context, never from the request body - taking a
     * username here would turn this into "change anyone's password" for whoever could guess one.
     *
     * <p>Re-checking the current password is not redundant with holding a token. A JWT lives
     * longer than the tab it was issued to, so without this an unattended session is enough to
     * take over the account permanently.
     *
     * <p>Existing tokens stay valid afterwards. This app signs stateless JWTs with no server-side
     * revocation list, so a changed password cannot retroactively invalidate them - an honest
     * limitation, called out here rather than implied away, and the reason the response tells the
     * user to sign out elsewhere if they are worried.
     */
    @PostMapping("/change-password")
    @Operation(summary = "Change the signed-in user's password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody RequestChangePassword request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not signed in.");
        }

        UserDb user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            // A valid token naming a user who no longer exists - a deleted account, or a token
            // issued before a database rebuild.
            log.warn("Password change for '{}' but no such user exists", auth.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not signed in.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            // Logged because repeated failures here are what an account takeover attempt looks
            // like. The submitted password is never logged.
            log.warn("Password change refused for '{}': current password did not match",
                    user.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Your current password is not correct.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body("The new password is the same as your current one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed for '{}'", user.getUsername());
        return ResponseEntity.ok("Password changed. Existing sessions on other devices stay "
                + "signed in until their token expires.");
    }
}
