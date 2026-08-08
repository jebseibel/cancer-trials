# Authentication — how login works in this project

Verified against the code 2026-08-08. JWT auth is **live and working** — every class below
exists and the login flow is what the frontend uses today.

> ⚠️ **Enforcement is currently switched OFF.** `SecurityConfig` has
> `.anyRequest().permitAll()`, so no endpoint requires a token. The original JWT-protected
> rule set is preserved commented-out directly beneath it in the same file. See
> *Before deployment* at the bottom — this is a blocker for QA/Prod, not a detail.

## Backend

| Piece | Location |
| --- | --- |
| `UserDb` entity | `database/.../db/entity/UserDb.java` |
| `UserRepository` | `database/.../db/repository/UserRepository.java` |
| `JwtUtil` — token generation/validation | `src/main/java/com/seibel/cancer/security/JwtUtil.java` |
| `CustomUserDetailsService` | `src/main/java/com/seibel/cancer/security/CustomUserDetailsService.java` |
| `JwtAuthenticationFilter` | `src/main/java/com/seibel/cancer/security/JwtAuthenticationFilter.java` |
| `SecurityConfig` | `src/main/java/com/seibel/cancer/config/SecurityConfig.java` |
| `AuthController` | `src/main/java/com/seibel/cancer/web/controller/AuthController.java` |
| Request/response DTOs | `web/request/RequestLogin.java`, `RequestRegister.java`, `web/response/ResponseAuth.java` |
| Users table changeset | `database/src/main/resources/db/changelog/changes/002-user.yaml` |

Dependencies: Spring Security starter + JWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`).

**Endpoints:** `POST /api/auth/login`, `POST /api/auth/register`.

## Frontend

- `frontend/src/pages/Login.tsx` — login/register form
- `frontend/src/components/ProtectedRoute.tsx` — wraps authenticated routes
- `frontend/src/services/api.ts` — `authApi.login()` / `authApi.register()`, an axios request
  interceptor that attaches the bearer token, a response interceptor that clears the token and
  redirects to `/login` on 401/403, and `authHelpers` (`saveToken`, `getToken`, `removeToken`,
  `isAuthenticated`, `saveUsername`, `getUsername`, `removeUsername`)
- `frontend/src/App.tsx` — `/login` public, everything else behind `ProtectedRoute`
- Logout button lives in `Layout.tsx`

Token is stored in `localStorage` under `token`; the username under `username`.

## The `user` table

**Table name is `user`, singular** — defined in `002-user.yaml`:

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint | auto-increment PK |
| `extid` | varchar(36) | unique, defaults to `(UUID())` |
| `username` | varchar(50) | unique, not null |
| `password` | varchar(255) | BCrypt hash |
| `email` | varchar(100) | |
| `role` | varchar(20) | not null, defaults to `USER` |
| `created_at` | datetime | not null, defaults to `CURRENT_TIMESTAMP` |
| `updated_at` / `deleted_at` | datetime | |
| `active` | int | defaults to 1 |

### `User` vs `AppUser` — two different tables

Login identity (`user`, changeset `002`) and personal trial tracking (`app_user`, changeset
`006`) are **separate tables with no foreign key between them**. The frontend matches them **by
username** via the `useCurrentAppUser` hook, which fetches `/api/appuser` and finds the row
whose username equals the logged-in user's.

Consequence: every login account needs an `AppUser` row seeded with a matching username, and
there is no UI to create that link. Without it, Saved Trials, Trial Detail tracking, and the
Diagnosis page all show "no app-user profile linked to your login."

## JWT configuration

There is **no `jwt:` block in `application.yml`**, so `JwtUtil`'s inline defaults are what run:

```java
@Value("${jwt.secret:mySecretKeyForJWTGenerationThatIsLongEnoughFor256BitHS256Algorithm}")
@Value("${jwt.expiration:86400000}")  // 24 hours
```

**The signing secret is currently a hardcoded literal committed to the repo.** Fine for local
single-user dev; not acceptable anywhere else. To override:

```yaml
jwt:
  secret: ${JWT_SECRET}        # min 256 bits
  expiration: 86400000
```

## Before deployment (QA or Prod)

1. **Restore endpoint security.** Remove `.anyRequest().permitAll()` in `SecurityConfig` and
   uncomment the preserved rule set below it. Note `/api/uchealth/callback` must stay
   `permitAll` — Epic's OAuth redirect cannot carry a JWT.
2. **Move the JWT secret to an environment variable** and generate a fresh random value. The
   committed default must not reach a deployed environment.
3. **Serve over HTTPS.** Tokens in `localStorage` over plain HTTP are trivially stolen.
4. Consider: refresh tokens, rate limiting on `/api/auth/login`, and whether
   `POST /api/auth/register` should be open at all on a single-patient app.

Related: `UcHealthOAuthTokenController` exposes full CRUD over the Epic token table, including
reading refresh tokens back over HTTP. Restrict or remove it before any deployment.

## Troubleshooting

**"Unauthorized" on every request** — confirm the JWT secret is identical between token
creation and validation (a changed `JWT_SECRET` invalidates all existing tokens), and that the
`Authorization: Bearer …` header is actually being sent.

**Can't log in after registering** — check the `user` table for the row and confirm the
password stored is a BCrypt hash, not plaintext.

**Redirected to login while logged in** — check `localStorage.token` exists and hasn't expired
(24h default). The axios response interceptor clears the token and redirects on any 401/403,
so a single unrelated 403 will log you out.
