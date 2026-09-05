package com.seibel.cancer.web.controller;

import com.seibel.cancer.database.db.entity.UserDb;
import com.seibel.cancer.database.db.repository.UserRepository;
import com.seibel.cancer.security.JwtUtil;
import com.seibel.cancer.security.RegistrationPolicyService;
import com.seibel.cancer.web.request.RequestRegister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code AuthController.register()}'s use of {@link RegistrationPolicyService}.
 *
 * <p>The allowlist's own parsing/default behaviour is covered directly in
 * {@code RegistrationPolicyServiceTest} - these tests instead pin how the controller reacts to
 * the policy's verdict: a refusal must short-circuit before any user is created, must not leak
 * whether a username collision exists, and must return the same generic failure shape as any
 * other rejected registration.
 */
class RegistrationAllowlistTest {

    private UserRepository userRepository;
    private RegistrationPolicyService registrationPolicyService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        registrationPolicyService = mock(RegistrationPolicyService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        controller = new AuthController(
                mock(AuthenticationManager.class),
                userDetailsService,
                mock(JwtUtil.class),
                userRepository,
                encoder,
                registrationPolicyService);

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userDetailsService.loadUserByUsername(any())).thenAnswer(invocation ->
                new User(invocation.getArgument(0), "irrelevant", List.of()));
    }

    private ResponseEntity<?> register(String username) {
        RequestRegister request = new RequestRegister();
        request.setUsername(username);
        request.setPassword("a-real-password");
        return controller.register(request);
    }

    @Test
    void shouldRegister_whenThePolicyAllowsIt() {
        when(registrationPolicyService.isRegistrationAllowed("tina")).thenReturn(true);

        ResponseEntity<?> response = register("tina");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userRepository).save(any(UserDb.class));
    }

    @Test
    void shouldRefuse_whenThePolicyDeniesIt() {
        when(registrationPolicyService.isRegistrationAllowed("tom")).thenReturn(false);

        ResponseEntity<?> response = register("tom");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectionMessageMustNotRevealTheAllowlist() {
        // The whole point of using the same generic failure as any other: it must not be
        // distinguishable from, say, a malformed request or a duplicate username.
        when(registrationPolicyService.isRegistrationAllowed("tom")).thenReturn(false);

        ResponseEntity<?> response = register("tom");

        String text = String.valueOf(response.getBody()).toLowerCase();
        org.junit.jupiter.api.Assertions.assertFalse(text.contains("allow"),
                "The rejection body must not mention the allowlist.");
        org.junit.jupiter.api.Assertions.assertFalse(text.contains("list"),
                "The rejection body must not mention the allowlist.");
    }

    @Test
    void shouldNotCheckForADuplicateUsername_beforeCheckingThePolicy() {
        // A caller the policy denies must not learn, via a 409 instead of a 401, that the name
        // they tried is already taken - that is real information about who is a real user here.
        when(registrationPolicyService.isRegistrationAllowed("tom")).thenReturn(false);
        when(userRepository.existsByUsername("tom")).thenReturn(true);

        ResponseEntity<?> response = register("tom");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(userRepository, never()).existsByUsername(any());
    }
}
