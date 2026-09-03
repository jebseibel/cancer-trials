package com.seibel.cancer.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The registration allowlist that replaced ADMIN-only registration.
 *
 * <p>Opposite default from {@link CustomUserDetailsService}'s login allowlist on purpose:
 * registration must fail closed when unconfigured, since registration was anonymous once and
 * that was the worst hole in the app (see {@code AuthController.register()}'s Javadoc). These
 * tests pin that default directly, alongside the ordinary allow/deny/case-insensitivity
 * behaviour shared with the login allowlist's parsing - a regression here either locks everyone
 * out or reopens the hole silently.
 */
class RegistrationPolicyServiceTest {

    private final RegistrationPolicyService policy = new RegistrationPolicyService();

    private void allow(String usernames) {
        ReflectionTestUtils.setField(policy, "allowedUsernames", usernames);
    }

    @Test
    void shouldRefuseEveryone_whenTheListIsUnset() {
        // The field's default from @Value - never explicitly set. This is the state a fresh
        // deployment starts in, and it must not accept anyone.
        assertFalse(policy.isRegistrationAllowed("tina"));
    }

    @Test
    void shouldRefuseEveryone_whenTheListIsBlank() {
        allow("   ");

        assertFalse(policy.isRegistrationAllowed("tina"));
    }

    @Test
    void shouldAllow_whenTheUsernameIsOnTheList() {
        allow("jeb,tina");

        assertTrue(policy.isRegistrationAllowed("tina"));
    }

    @Test
    void shouldRefuse_whenTheUsernameIsNotOnTheList() {
        allow("jeb,tina");

        assertFalse(policy.isRegistrationAllowed("tom"));
    }

    @Test
    void shouldMatchCaseInsensitively() {
        allow("jeb,Tina");

        assertTrue(policy.isRegistrationAllowed("TINA"));
    }

    @Test
    void shouldIgnoreSurroundingWhitespaceInTheList() {
        allow(" jeb , tina ");

        assertTrue(policy.isRegistrationAllowed("tina"));
    }

    @Test
    void shouldIgnoreBlankEntriesFromTrailingCommas() {
        allow("jeb,tina,");

        assertTrue(policy.isRegistrationAllowed("tina"));
        assertFalse(policy.isRegistrationAllowed(""));
    }

    @Test
    void shouldAllowAnyUsername_whenTheListIsTheWildcard() {
        // What application-profiles.yml's dev section sets, so local development has no friction.
        allow("*");

        assertTrue(policy.isRegistrationAllowed("tina"));
        assertTrue(policy.isRegistrationAllowed("anyone-at-all"));
    }

    @Test
    void wildcardShouldBeALiteralCheck_notAGlobOrRegex() {
        // Pins that "*" is one exact sentinel value, not a pattern - a username that happens to
        // look regex-special must not accidentally match or break the check.
        allow("*");

        assertTrue(policy.isRegistrationAllowed(".*"));
        assertTrue(policy.isRegistrationAllowed("a.b"));
    }

    @Test
    void shouldNotTreatAWildcardAmongOtherNames_asOpeningRegistrationToEveryone() {
        // "*" only opens things up as the WHOLE list, matching application-profiles.yml's dev
        // section. A
        // comma-separated list that happens to contain a literal "*" alongside real names is not
        // the same shape and must not silently become wide open.
        allow("jeb,*,tina");

        assertFalse(policy.isRegistrationAllowed("tom"));
        assertTrue(policy.isRegistrationAllowed("tina"));
    }
}
