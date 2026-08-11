# Production Deploy Runbook

First deploy of this app to a public host, holding **one real person's medical record**.

Written 2026-08-11, after the security work of that day. Supersedes
`_archive/hosting/qa-setup.md` for anything security- or architecture-related — that guide was
written 2026-08-08 against sample data and is wrong in three ways that matter (see
"What the archived guide gets wrong"). Its infrastructure steps are still sound and are folded
in here.

Companion docs: `../CURRENT_STATE.md` (what is built), `../ingestion/QDRANT_SETUP.md` (the
collection), `../ingestion/DEPLOYMENT_SEEDING.md` (why the corpus is the hard part).

---

## What the archived guide gets wrong

Read this before following any step from `_archive/hosting/qa-setup.md`.

1. **It serves the frontend from Nginx at `/var/www/cancer`.** That is not this architecture.
   `./gradlew buildDeployment` copies `frontend/dist` into `src/main/resources/static` and
   bundles it **inside the jar** — Spring serves the SPA. Nginx should proxy everything to
   :8080 and serve no files of its own. Following the old guide gives two copies of the
   frontend, and the one Nginx serves will be whichever was last copied by hand.
2. **The jar is `cancer-0.0.2-SNAPSHOT.jar`, not `cancer-server.jar`.**
3. **It curls `/actuator/health`. There is no actuator dependency in this project** — that check
   returns 404 (or 403 now) and is not a health check. Use the login endpoint instead.
4. Its Step 15 is truncated mid-sentence, and its security checklist describes the
   pre-2026-08-11 state: JWT secret in source, endpoint security disabled, token CRUD exposed.
   All three are fixed; do not "restore" anything from it.
5. It exposes Swagger publicly. Swagger is now `authenticated()`.

---

## Before you start

| Prerequisite | Why |
| --- | --- |
| A domain name pointing at the server | Let's Encrypt will not issue for a bare IP |
| A **new** strong password for `jeb` | `password123` is local-only and must not reach prod |
| A fresh 512-bit `JWT_SECRET` | Never reuse the local value |
| A MySQL password | Not the local one |

**Time budget: 3-4 hours**, nearly all of it the corpus rebuild (see Phase 4).

---

## Phase 1 — Server, Java, MySQL, Nginx

Follow `_archive/hosting/qa-setup.md` steps 1-8 as written; they are correct. In summary:
Ubuntu 24.04, `openjdk-21-jdk`, `mysql-server`, `nginx`, and directories `/opt/cancer` and
`/var/log/cancer`. **Skip `/var/www/cancer`** — nothing will be served from it.

Create the database and user (run these yourself; never from an assistant session):

```sql
CREATE DATABASE cancer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cancer_user'@'localhost' IDENTIFIED BY 'A_REAL_PASSWORD';
GRANT ALL PRIVILEGES ON cancer.* TO 'cancer_user'@'localhost';
FLUSH PRIVILEGES;
```

⚠️ `_archive/hosting/setup-n8n-user.md` recreates this user with the `'%'` wildcard so a Docker
n8n container can reach it. **Do not do that here.** That is a local convenience for the
`clear-db` webhook, and n8n is not part of this deployment. On a public host, keep the account
scoped to `localhost`.

Firewall — do this before the app is reachable:

```bash
ufw allow 22/tcp && ufw allow 80/tcp && ufw allow 443/tcp && ufw enable
```

MySQL (3306) and Qdrant (6333/6334) must never be open. Qdrant ships with **no authentication
of any kind**; `docker-compose.yml` already binds it to `127.0.0.1` via `QDRANT_BIND`.

---

## Phase 2 — Qdrant

Docker, plus the compose file from the repo. Two settings there are load-bearing and were each
found the hard way:

- `ulimits: nofile: 65536` — Qdrant memory-maps every segment. On Docker's default 1024 it
  crash-loops with `Too many open files` at around 40,000 points, and `restart: unless-stopped`
  turns that into an infinite recover-and-die cycle.
- The bind address stays `127.0.0.1`.

Create the collection **before** backfilling — 384 dimensions, Cosine. Exact command and the
REST-6333-vs-gRPC-6334 trap are in `../ingestion/QDRANT_SETUP.md`. The app logs
`SEARCH UNAVAILABLE` at boot if the collection is missing, which is the check that it worked.

---

## Phase 3 — Build, transfer, run

On the dev machine:

```bash
./gradlew buildDeployment          # npmInstall -> npmBuild -> cleanStatic -> copyFrontend -> build
```

⚠️ **`buildDeployment` is not optional and not the same as `build`.** The bundled
`src/main/resources/static/` in the repo was last written 2025-12-13 — months stale, with no
Trials for You page and no trial locations. A plain `build` ships that.

```bash
scp build/libs/cancer-0.0.2-SNAPSHOT.jar root@HOST:/opt/cancer/cancer.jar
```

`/opt/cancer/.env` on the server, `chmod 600`:

```
RDS_HOSTNAME=localhost
RDS_PORT=3306
RDS_DB_NAME=cancer
RDS_USERNAME=cancer_user
RDS_PASSWORD=...                  # the real one
JWT_SECRET=...                    # fresh 512-bit; the app will NOT boot without it
QDRANT_BIND=127.0.0.1
CORS_ALLOWED_ORIGINS=https://yourdomain.com
SERVER_PORT=8080
```

⚠️ **`JWT_SECRET` has no default, deliberately.** A default would silently re-enable the literal
that is public in git history. Unset means the app fails to start, which is the failure you want.

⚠️ **`CORS_ALLOWED_ORIGINS` must be the real origin.** It defaults to the Vite dev server; left
unset, the browser blocks every API call from the real domain and the app appears broken with no
server-side error to explain it. (The `cors.allowed.origins` property was missing from
`application.yml` entirely until 2026-08-11 — only the `@Value` default existed, so the env var
would not have bound. Added while writing this runbook.)

Systemd unit — as `_archive/hosting/qa-setup.md` step 10, with the jar name corrected and heap
raised. `-Xmx512m` is too small: embedding runs at ~240% CPU and the ONNX model plus batches need
room.

```ini
ExecStart=/usr/bin/java -Xms512m -Xmx1536m -jar /opt/cancer/cancer.jar
```

Then `systemctl daemon-reload && systemctl enable --now cancer`.

**Health check** (there is no actuator):

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{}'
# 400 = up and validating. 000 = not up.
```

---

## Phase 4 — The corpus (the long part)

**Do this before the patient data.** Her record on a box with no trials demonstrates nothing.

⚠️ **Run it from a shell on the server inside `tmux`, never from the frontend button.** Both
hold one HTTP request open for the entire run; Nginx will cut it, and a browser tab will not
survive it. A long-held request already failed at 58% once locally.

```bash
tmux new -s corpus
# 1. Pull  (~14 min here)
curl -X POST 'http://localhost:8080/api/ingestion/clinicaltrials' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"condition":"breast cancer","maxStudies":5000}'

# 2. Backfill  (~25 min HERE; expect 2-3 HOURS on a small VPS)
curl -X POST 'http://localhost:8080/api/rag/backfill' -H "Authorization: Bearer $TOKEN"
```

Get `$TOKEN` from `/api/auth/login` — **every endpoint now requires a JWT.**

**Why the backfill is so much slower there.** It is ~136,000 local ONNX inferences, CPU-bound.
The dev machine is a 16-core Ryzen AI 7 PRO; a Hostinger KVM is 2-4 shared vCPUs. There is no
tuning around it.

**It is resumable, which is what saves you.** `TrialIndexService.isIndexed` skips trials already
in the store, so a run that dies at 60% picks up where it left off — re-running 50 indexed trials
was measured at 0.4s versus 14.9s. **Do not pass `?force=true`** unless the chunking or the
embedding model changed.

**Why a snapshot from the dev machine will not work:** chunk payloads key on `trialExtid`, and
extids regenerate on every database rebuild, so a snapshot is only valid against the exact MySQL
rows it was built from. Making chunks key on `nctId` instead would make snapshots portable and
turn this phase into a restore — see `../ingestion/DEPLOYMENT_SEEDING.md`. It is a chunking
change, so it costs a full re-index whenever it is adopted; cheaper now than later.

---

## Phase 5 — Her record

**Three files only. 4.4 KB.**

```bash
scp .claude/patient-data/patient-diagnosis.csv \
    .claude/patient-data/patient-variant.csv \
    .claude/patient-data/patient-prior-treatment.csv \
    root@HOST:/opt/cancer/.claude/patient-data/
```

On the server: `chmod 700` the directory, `chmod 600` the files.

⚠️ **`my-health-summary.pdf` (21 MB), `mri-scan.md` and `pet-scan-2026-03-16.md` never go to the
server.** Decided permanently 2026-08-11. `PatientSeedLoader` does not read them — they were
source documents for the CSVs. They are the largest concentration of her medical data and would
sit there for no functional reason.

Restart; `PatientSeedLoader` creates `AppUser` + the three patient rows. Then:

```bash
curl -s -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/appuser?page=0&size=5'
```

⚠️ **Extids regenerate.** The prod AppUser extid differs from local — every Rank Trials call is
keyed on it, so fetch the real one.

⚠️ **Her UI edits will silently revert.** `PatientSeedLoader` seeds-if-absent and never syncs, so
after she edits anything through the app, the CSVs are stale and the next rebuild reverts her
changes. Fine for a demo; a real data-loss path if she starts relying on it.

---

## Phase 6 — Nginx and HTTPS

Nginx proxies **everything** to :8080 and serves no files. The SPA lives in the jar.

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        # LoginRateLimitFilter reads this. Nginx OVERWRITES rather than appends, so a client
        # cannot forge an address to evade the login throttle.
        proxy_set_header X-Forwarded-For   $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Ingestion and backfill run for minutes to hours. Nginx's 60s default kills them.
    # Raised only on these paths, so a genuinely hung request elsewhere still fails fast.
    location ~ ^/api/(ingestion|rag/backfill) {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_read_timeout 4h;
        proxy_send_timeout 4h;
    }
}
```

Note `X-Forwarded-For $remote_addr` rather than `$proxy_add_x_forwarded_for`: the latter appends
to a client-supplied header, and the rate limiter reads the first hop. Overwriting means a
caller cannot spoof it.

Then TLS:

```bash
apt install -y certbot python3-certbot-nginx
certbot --nginx -d yourdomain.com
certbot renew --dry-run
```

⚠️ **Until TLS is live, do not log in over the public IP.** The JWT crosses the network in
cleartext on every request; anyone in path can lift and replay it. TLS is what makes the auth
work mean anything.

---

## Phase 7 — Verify before telling her it is ready

```bash
D=https://yourdomain.com

# Unauthenticated: her record must be closed
for p in /api/patientdiagnosis /api/patientvariant /api/trial /api/uchealthoauthtoken /v3/api-docs; do
  echo -n "$p -> "; curl -s -o /dev/null -w '%{http_code}\n' "$D$p"
done                                     # all 401/403

# The SPA must load
curl -s -o /dev/null -w 'login page -> %{http_code}\n' "$D/login"          # 200

# Registration must be closed
curl -s -o /dev/null -w 'register -> %{http_code}\n' -X POST "$D/api/auth/register" \
  -H 'Content-Type: application/json' -d '{"username":"x","password":"y","email":"a@b.c"}'   # 403

# Login, then the endpoint she uses
TOKEN=$(curl -s -X POST "$D/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"jeb","password":"THE_NEW_ONE"}' | python3 -c 'import json,sys;print(json.load(sys.stdin)["token"])')
curl -s -o /dev/null -w 'rank -> %{http_code} %{time_total}s\n' \
  -H "Authorization: Bearer $TOKEN" "$D/api/matching/rank/PROD_EXTID?breastOnly=true&limit=5"
```

Expect ranking around 4-5s on the dev machine; slower on a small VPS.

**Then open it in a browser and press the button.** Every failure that mattered today — the
403 on `/login`, the IP-only lockout — was found by using the thing, not by testing a part.

---

## Known gaps being accepted

Written down so they are decisions, not discoveries.

- **No authorization model.** Any authenticated user can read any patient's record by passing
  their extid — endpoints take the target from the URL and never compare it to the caller.
  Acceptable while there is exactly one account; a live problem the moment there are two. See
  `../CURRENT_STATE.md`.
- **No encryption at rest, no access log.** Accepted knowingly by the user for a single-patient
  tool on his own host.
- **Existing JWTs survive account deletion** — tokens are stateless and cannot be recalled.
- **The Epic token dies in ~1 hour** with no refresh grant, so FHIR ingestion needs an
  interactive browser login each time. Unrelated to trial matching, which is what she will use.
- **Login lockout is in-memory**, so `systemctl restart cancer` clears any lockout instantly.
  That is the recovery path if the account gets locked.

---

## Rollback

The jar is the whole application. Keep the previous one:

```bash
cp /opt/cancer/cancer.jar /opt/cancer/cancer.jar.prev     # before each deploy
systemctl stop cancer && mv /opt/cancer/cancer.jar.prev /opt/cancer/cancer.jar && systemctl start cancer
```

MySQL and Qdrant are untouched by a jar swap. A schema change is a different matter —
`spring.liquibase.drop-first` is `false`, so an edit to an already-applied changeset does not
take effect on restart, and rolling back a *new* changeset is manual.
