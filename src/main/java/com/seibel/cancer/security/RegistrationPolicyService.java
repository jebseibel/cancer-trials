package com.seibel.cancer.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Decides who may create their own account via {@code POST /api/auth/register}.
 *
 * <p>Lives beside {@link CustomUserDetailsService}, not in the business service layer, because
 * this is the same kind of concern that class owns for login: a security policy check with
 * nothing to persist, not CRUD over an entity. Deliberately its own class rather than a method
 * added to a user CRUD service, so the two allowlists (this one and the login allowlist) stay
 * structurally parallel and neither pulls a security policy into a class about database rows.
 *
 * <p><b>The empty-list default is the opposite of the login allowlist's.</b>
 * {@code CustomUserDetailsService.isAllowed()} treats a blank list as "no restriction" - a safe
 * default for a convenience check that gates who may authenticate. Registration is different:
 * it used to be anonymous, and that was "the most serious hole in the application" (see
 * {@code AuthController.register()}'s Javadoc) until it was locked down on 2026-08-11. A blank
 * list here must mean nobody may self-register, or a deployment that forgets to configure this
 * property would silently reopen that hole. When genuinely open registration is wanted - local
 * development only - the {@code dev} section of {@code application-profiles.yml} sets the list
 * to the literal {@code "*"} instead of leaving it blank, so "wide open" stays an explicit choice
 * rather than a default nobody meant to rely on.
 */
@Service
@Slf4j
public class RegistrationPolicyService {

    @Value("${security.registration.allowed-usernames:}")
    private String allowedUsernames;

    /**
     * A single designated value, checked literally - not a glob or regex wildcard. The dev
     * profile is the only place this is set (see application-profiles.yml's dev section); this
     * project has no other use for "any username matching a pattern", so one exact sentinel is
     * all that's needed.
     */
    private static final String ALLOW_ANY = "*";

    /**
     * Same comma-separated, trimmed, case-insensitive parsing as
     * {@code CustomUserDetailsService.isAllowed()} - only the empty-list branch differs.
     */
    public boolean isRegistrationAllowed(String username) {
        if (allowedUsernames == null || allowedUsernames.isBlank()) {
            return false;
        }
        if (ALLOW_ANY.equals(allowedUsernames.trim())) {
            return true;
        }
        return Arrays.stream(allowedUsernames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(allowed -> allowed.equalsIgnoreCase(username));
    }
}
