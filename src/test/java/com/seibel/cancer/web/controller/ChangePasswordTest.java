package com.seibel.cancer.web.controller;

import com.seibel.cancer.database.db.entity.UserDb;
import com.seibel.cancer.database.db.repository.UserRepository;
import com.seibel.cancer.web.request.RequestChangePassword;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Password change rules.
 *
 * <p>Each test here pins a way the endpoint could hand someone else's account over, so they are
 * written as attacks rather than as feature coverage: a wrong current password must not succeed,
 * an unauthenticated caller must not reach the user lookup, and the stored value must never be
 * the plaintext that was submitted.
 *
 * <p>Uses a real {@link BCryptPasswordEncoder} rather than a mock. The encoder is the security
 * property under test - a stubbed {@code matches} returning true would make every assertion here
 * pass while proving nothing.
 */
class ChangePasswordTest {

    private static final String USERNAME = "jeb";
    private static final String CURRENT = "current-password";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private UserRepository userRepository;
    private AuthController controller;
    private UserDb user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        controller = new AuthController(
                mock(org.springframework.security.authentication.AuthenticationManager.class),
                mock(org.springframework.security.core.userdetails.UserDetailsService.class),
                mock(com.seibel.cancer.security.JwtUtil.class),
                userRepository,
                encoder);

        user = new UserDb();
        user.setUsername(USERNAME);
        user.setPassword(encoder.encode(CURRENT));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearContext() {
        // Leaks between tests would let one test's identity authorise another's request.
        SecurityContextHolder.clearContext();
    }

    private void signIn(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    private ResponseEntity<?> change(String current, String next) {
        return controller.changePassword(new RequestChangePassword(current, next));
    }

    @Test
    void shouldChangePassword_whenCurrentPasswordIsCorrect() {
        signIn(USERNAME);

        ResponseEntity<?> response = change(CURRENT, "a-new-password");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userRepository).save(user);
        assertTrue(encoder.matches("a-new-password", user.getPassword()),
                "The new password must be what is now stored.");
    }

    @Test
    void shouldStoreAHash_neverThePlaintext() {
        signIn(USERNAME);

        change(CURRENT, "a-new-password");

        // The whole point of the encoder. A regression here is not a failing feature, it is a
        // plaintext password column.
        assertFalse(user.getPassword().equals("a-new-password"),
                "The plaintext password must never be stored.");
        assertTrue(user.getPassword().startsWith("$2"), "Expected a bcrypt hash.");
    }

    @Test
    void shouldRefuse_whenCurrentPasswordIsWrong() {
        signIn(USERNAME);
        String before = user.getPassword();

        ResponseEntity<?> response = change("not-the-right-password", "a-new-password");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(userRepository, never()).save(any());
        assertEquals(before, user.getPassword(), "A failed attempt must not alter the password.");
    }

    @Test
    void shouldRefuse_whenNotSignedIn() {
        // No authentication in the context at all.
        ResponseEntity<?> response = change(CURRENT, "a-new-password");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        // Must not even look the user up - there is no user to act on.
        verify(userRepository, never()).findByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRefuse_whenTheTokenNamesAUserThatNoLongerExists() {
        signIn("deleted-account");
        when(userRepository.findByUsername("deleted-account")).thenReturn(Optional.empty());

        ResponseEntity<?> response = change(CURRENT, "a-new-password");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldChangeOnlyTheSignedInUsersPassword() {
        // The request body carries no username, so a signed-in user cannot name a different
        // account. This pins that the identity comes from the security context.
        signIn(USERNAME);

        change(CURRENT, "a-new-password");

        verify(userRepository).findByUsername(USERNAME);
    }

    @Test
    void shouldRefuse_whenNewPasswordMatchesTheOldOne() {
        signIn(USERNAME);

        ResponseEntity<?> response = change(CURRENT, CURRENT);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userRepository, never()).save(any());
    }
}
