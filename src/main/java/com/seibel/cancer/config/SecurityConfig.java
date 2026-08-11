package com.seibel.cancer.config;

import com.seibel.cancer.security.CustomUserDetailsService;
import com.seibel.cancer.security.JwtAuthenticationFilter;
import com.seibel.cancer.security.LoginRateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
// Required for @PreAuthorize to be honoured. Without it the annotation is silently ignored -
// the method still runs, and the endpoint looks protected while being wide open.
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configure(http)) // Enable CORS
                // Every API endpoint requires a JWT. This database holds a real medical record,
                // so an open endpoint is a disclosure of one person's diagnosis, genomic
                // variants and treatment history - not merely a misconfiguration.
                .authorizeHttpRequests(auth -> auth
                        // The React SPA and its assets are served unauthenticated: the browser
                        // has to load the login page before it can have a token.
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/vite.svg").permitAll()
                        .requestMatchers("/assets/**", "/*.js", "/*.css").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // Client-side routes. A SPA route is not a file - the server returns
                        // index.html and the router takes over - so each has to be reachable
                        // without a token or the app 403s at its own front door. Found exactly
                        // that way: /login returned 403 the first time security was switched on.
                        //
                        // Listed explicitly rather than opened with a wildcard. Guarding the app
                        // is what /api/** authentication does; these paths serve one static HTML
                        // shell and no data.
                        .requestMatchers("/login", "/trials/**", "/ranked-trials", "/saved-trials",
                                "/diagnosis", "/variants", "/prior-treatment", "/ingestion")
                        .permitAll()

                        // Login must be public: the browser cannot present a token before it
                        // has one. Registration must NOT be - it was anonymous, and a stranger
                        // could mint an account and read everything. Belt and braces with the
                        // @PreAuthorize on the method itself.
                        .requestMatchers("/api/auth/register").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**").permitAll()

                        // Epic's OAuth redirect lands here from the patient's browser and
                        // cannot carry a JWT, so this one path must stay open. It is scoped to
                        // the callback alone - never the whole /api/uchealth/** tree.
                        .requestMatchers("/api/uchealth/callback").permitAll()

                        // Swagger is deliberately NOT public. On a host reachable from the
                        // internet it publishes a complete map of every endpoint and payload
                        // shape over a database holding a real patient record. Authenticate to
                        // read it, or set springdoc.swagger-ui.enabled=false in prod.
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").authenticated()

                        // Everything else - all of /api/**, and any path not matched above.
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Rate limit first: a locked-out address must be rejected before any password
                // work happens, or the throttle still pays the cost of every guess.
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
