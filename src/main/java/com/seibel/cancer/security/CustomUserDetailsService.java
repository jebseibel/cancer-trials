package com.seibel.cancer.security;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.UserDb;
import com.seibel.cancer.database.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user for authentication, refusing soft-deleted accounts.
     *
     * <p><b>The active check is the point.</b> This schema soft-deletes: {@code DELETE
     * /api/user/{extid}} sets {@code active} to INACTIVE and leaves the row. Without the check
     * below, {@code findByUsername} still returned that row and the deleted account kept
     * logging in and receiving valid tokens — verified on 2026-08-11 by deleting an account
     * (HTTP 204) and then authenticating as it (HTTP 200).
     *
     * <p>Revoking access is the entire reason to delete a user, so a delete that does not
     * revoke is worse than no delete: it reports success and changes nothing.
     *
     * <p>Existing tokens still work until they expire — JWTs are stateless and nothing here can
     * recall one. Deleting an account stops new logins, not a token already issued.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDb user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.getActive() != ActiveEnum.ACTIVE) {
            log.warn("loadUserByUsername(): refusing inactive account '{}'", username);
            throw new UsernameNotFoundException("User not found: " + username);
        }

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
