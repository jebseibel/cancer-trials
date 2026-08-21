package com.seibel.cancer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Throttles repeated failed logins from one IP address.
 *
 * <p><b>Why this exists.</b> {@code /api/auth/login} has to stay public — the browser cannot
 * carry a token before it has one — so it is the single unauthenticated door into a database
 * holding a real medical record. Without a limit, an attacker can try passwords as fast as the
 * network allows, and a common password falls in seconds. Every other control added today (JWT
 * on all endpoints, the token CRUD removal) is only worth as much as this door.
 *
 * <p><b>Only failures count.</b> A successful login clears the counter, so an ordinary person
 * mistyping a password twice and then getting it right is never locked out. What is being
 * limited is guessing, not using.
 *
 * <p><b>In-memory and per-instance</b>, which is honest about what it is. It resets on restart
 * and does not coordinate across instances — fine for a single-instance deployment, and it
 * raises the cost of an online guessing attack by orders of magnitude. It is not a defence
 * against a distributed attempt from many addresses; a strong password remains the real control.
 * If this ever runs behind more than one instance, move the counter to Redis.
 *
 * <p><b>Keyed on client IP AND username, not IP alone.</b> Per-IP alone is a denial of service:
 * failed guesses against any username lock out every other user from that address. Found exactly
 * that way — probing a fake username locked the real account out of the same machine. Behind a
 * proxy or CGNAT many people share one address, so an attacker could deliberately lock the
 * patient out of her own tool.
 *
 * <p>The IP half still matters: without it, one attacker could lock a known username from
 * anywhere. Honours {@code X-Forwarded-For} because in deployment Nginx sits in front and the
 * socket address would otherwise be the proxy for everyone.
 */
@Slf4j
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    /**
     * Consecutive failures for one IP+username pair before it is locked out.
     *
     * <p>The fallback here only applies when the property is absent entirely; {@code
     * application.yml} sets it, and the two must agree. See the note there for why 5.
     */
    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    /** How long a lockout lasts. */
    @Value("${security.login.lockout-minutes:15}")
    private long lockoutMinutes;

    /**
     * Failure counters by IP+username.
     *
     * <p>Bounded by {@link #MAX_TRACKED_ADDRESSES} so a spray from many forged addresses cannot
     * grow this without limit and exhaust heap — the memory-pressure version of the attack this
     * filter exists to stop.
     */
    private final Map<String, Attempts> attemptsByIp = new ConcurrentHashMap<>();

    private static final int MAX_TRACKED_ADDRESSES = 10_000;

    private static final class Attempts {
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant lockedUntil = Instant.EPOCH;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        boolean isAuthPost = "POST".equalsIgnoreCase(request.getMethod())
                && ("/api/auth/login".equals(path) || "/api/auth/register".equals(path));
        return !isAuthPost;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // The body has to be read here for the username and read again by the controller, and a
        // servlet input stream is single-pass - so it is buffered and replayed downstream.
        CachedBodyRequest cached = new CachedBodyRequest(request);
        String key = clientIp(request) + "|" + usernameFrom(cached.body());
        Attempts attempts = attemptsByIp.get(key);

        if (attempts != null && Instant.now().isBefore(attempts.lockedUntil)) {
            long retryAfter = Duration.between(Instant.now(), attempts.lockedUntil).toSeconds();
            log.warn("login rate limit: {} is locked out for a further {}s", key, retryAfter);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many sign-in attempts. Please wait and try again.\"}");
            return;
        }

        chain.doFilter(cached, response);

        // The controller returns 401 for bad credentials; anything below 400 is a real login.
        if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            recordFailure(key);
        } else if (response.getStatus() < 400) {
            attemptsByIp.remove(key);
        }
    }

    private void recordFailure(String key) {
        if (attemptsByIp.size() >= MAX_TRACKED_ADDRESSES && !attemptsByIp.containsKey(key)) {
            // Full. Dropping the new entry is deliberate: the alternative is unbounded growth,
            // and the addresses already tracked are the ones actively guessing.
            log.warn("login rate limit: tracking table full, not tracking {}", key);
            return;
        }
        Attempts attempts = attemptsByIp.computeIfAbsent(key, k -> new Attempts());
        int failures = attempts.count.incrementAndGet();
        if (failures >= maxAttempts) {
            attempts.lockedUntil = Instant.now().plus(Duration.ofMinutes(lockoutMinutes));
            attempts.count.set(0);
            log.warn("login rate limit: {} locked out for {} minutes after {} failed attempts",
                    key, lockoutMinutes, failures);
        }
    }

    /**
     * The caller's address, preferring the first hop in {@code X-Forwarded-For}.
     *
     * <p>That header is client-controlled and trivially forged, so this is only trustworthy
     * behind a proxy that overwrites it — which is how Nginx should be configured here. Read
     * directly on a public port it lets an attacker rotate the key and evade the limit; that is
     * a reason to terminate at a proxy, not a reason to ignore the header behind one.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * The username from a login body, lower-cased, or {@code "?"} when it cannot be read.
     *
     * <p>Deliberately a small regex rather than a JSON parse: this runs before authentication on
     * a public endpoint, so it must not throw on malformed or hostile input. An unreadable body
     * collapses to one shared bucket, which still throttles - it does not fail open.
     *
     * <p>Truncated because the value is attacker-controlled and becomes a map key; without a cap
     * a caller could send megabyte usernames and blow out the tracking table.
     */
    private String usernameFrom(String body) {
        if (body == null || body.isBlank()) {
            return "?";
        }
        Matcher m = USERNAME_IN_JSON.matcher(body);
        if (!m.find()) {
            return "?";
        }
        String name = m.group(1).trim().toLowerCase();
        return name.length() > 64 ? name.substring(0, 64) : name;
    }

    private static final Pattern USERNAME_IN_JSON =
            Pattern.compile("\"username\"\\s*:\\s*\"([^\"]{0,256})\"");

    /**
     * Replays a buffered request body downstream.
     *
     * <p>A servlet input stream can only be read once. Reading it here to find the username
     * would otherwise leave the controller with an empty body and break every login.
     */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        String body() {
            return new String(body, StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream buffer = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return buffer.read();
                }

                @Override
                public boolean isFinished() {
                    return buffer.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    // Blocking reads only; the container never drives this asynchronously here.
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
