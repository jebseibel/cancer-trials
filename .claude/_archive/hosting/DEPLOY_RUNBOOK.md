# Production Deploy Runbook

> **DEPLOYED 2026-08-11.** https://breastcancertrialfinder.com is live, HTTPS, with her record
> seeded. What follows is the corrected procedure — every correction below came from doing it,
> not from planning it. The corpus (Phase 4) had not been run as of that date.
>
> ⚠️ **Reviewed 2026-08-14: the authorization model is NOT on `main`, and therefore not
> deployable yet.** `main`'s tip is `0544c6a`, this deploy's own commit. The access model
> (`c9cb30d`), the mobile work (`14aadff`) and the current docs (`5836a0b`) live only on
> `frontend-mobile` and have not been merged. **A deploy from `main` today ships the app exactly
> as it was on 2026-08-11** — no `user_patient` grants, no access levels.
>
> `1b663cb` (password hashing on create/update, plus the login allowlist) **is** on `main` and
> therefore deployable; the `.env` block in Phase 3 has gained `LOGIN_ALLOWED_USERNAMES` for it.
>
> Whether Phase 4 has since been run is not recorded anywhere — check the server, not this file.

First deploy of this app to a public host, holding **one real person's medical record**.

Written 2026-08-11, after the security work of that day. Supersedes
`_archive/hosting/qa-setup.md` for anything security- or architecture-related — that guide was
written 2026-08-08 against sample data and is wrong in three ways that matter (see
"What the archived guide gets wrong"). Its infrastructure steps are still sound and are folded
in here.

Companion docs: `../CURRENT_STATE.md` (what is built), `../_archive/ingestion/QDRANT_SETUP.md` (the
collection), `../_archive/ingestion/DEPLOYMENT_SEEDING.md` (why the corpus is the hard part).

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
| `breastcancertrialfinder.com` A record -> the server IP, propagated | Let's Encrypt will not issue for a bare IP, and certbot fails if DNS has not resolved yet |
| A **new** strong password for `jeb` | `password123` is local-only and must not reach prod |
| A fresh 512-bit `JWT_SECRET` | Never reuse the local value |
| A MySQL password | Not the local one |

**Time budget: 3-4 hours**, nearly all of it the corpus rebuild (see Phase 4).

⚠️ **This box is not empty, and that changed three decisions.** It runs `/opt/cpss/cpss.jar`
**on port 8080** (a separate long-running app - do not stop it), MySQL, and two Ghost containers
on 2369/2371. Nginx is already installed and fronting them.

- **The app runs on 8081**, set as `PORT=8081` in `.env`. Deploying to 8080 fails with
  "port already in use", and `Restart=on-failure` turns that into a crash loop that looks like
  an application fault.
- **Heap is `-Xmx640m`**, not 1536m. With 1 vCPU and ~3.9GB shared across MySQL (626MB), cpss
  (449MB) and two Ghost containers, a large heap starves the off-heap ONNX model.
- **Nginx is an ADD-A-SERVER-BLOCK job, not an install.** Read the existing config first; it
  serves live sites.

**The server is reachable as `ssh cancer`** (an SSH config alias on the dev machine); every
`scp` below uses it. Its hostname is `cpss`, a leftover from the project this repo was copied
from — cosmetic, not a sign you are on the wrong box.

⚠️ **That alias sets `RemoteCommand` and `RequestTTY yes`, which silently breaks `scp` and any
scripted `ssh`.** Symptom: `Cannot execute command-line and remote command`, or an scp that
appears to succeed while transferring nothing. Every transfer below needs the override:

```bash
scp -o RemoteCommand=none -o RequestTTY=no ...
ssh -o RemoteCommand=none -o RequestTTY=no cancer '...'
```

The patient CSVs looked transferred and were not, which is a bad thing to be wrong about.

### Phase 0 — DNS, first, because it has to propagate

Do this before anything else; certbot in Phase 6 fails outright if the name does not yet resolve
to the server, and propagation can take minutes to hours.

At the registrar, two A records pointing at the server's IPv4:

| Type | Host | Value |
| --- | --- | --- |
| A | `@` | the server IP |
| A | `www` | the server IP |

Check from somewhere that is not your own machine's cache:

```bash
dig +short breastcancertrialfinder.com @8.8.8.8
dig +short www.breastcancertrialfinder.com @8.8.8.8
```

Both must print the server IP before you run certbot.

⚠️ **If the registrar offers "domain privacy" or WHOIS redaction, turn it on.** This domain is
about to be publicly associated with a tool built for one named person's medical care; the
registration record should not carry your home address.

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

## Phase 1.5 — Swap, before anything memory-hungry

The box had **no swap**, and with none the OOM killer terminates a process outright rather than
degrading. That is the likeliest explanation for a backfill dying at ~58% with no error.

```bash
fallocate -l 4G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
sysctl -w vm.swappiness=10 && echo "vm.swappiness=10" >> /etc/sysctl.conf
```

`swappiness=10` makes swap a safety net rather than routine overflow — on 1 vCPU, eager
swapping puts disk I/O in competition with the CPU-bound embedding.

---

## Phase 2 — Qdrant

Docker, plus the compose file from the repo. Two settings there are load-bearing and were each
found the hard way:

- `ulimits: nofile: 65536` — Qdrant memory-maps every segment. On Docker's default 1024 it
  crash-loops with `Too many open files` at around 40,000 points, and `restart: unless-stopped`
  turns that into an infinite recover-and-die cycle.
- The bind address stays `127.0.0.1`.

Create the collection **before** backfilling — 384 dimensions, Cosine. Exact command and the
REST-6333-vs-gRPC-6334 trap are in `../_archive/ingestion/QDRANT_SETUP.md`. The app logs
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
scp build/libs/cancer-0.0.2-SNAPSHOT.jar cancer:/opt/cancer/cancer.jar
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
CORS_ALLOWED_ORIGINS=https://breastcancertrialfinder.com,https://www.breastcancertrialfinder.com
PORT=8081
SPRING_PROFILES_ACTIVE=prod       # added when profiles were introduced; see the warning below
LOGIN_ALLOWED_USERNAMES=jeb      # added 2026-08-14; see the warning below
REGISTRATION_ALLOWED_USERNAMES=jeb,tina   # who may self-register; empty means nobody can
```

⚠️ **`LOGIN_ALLOWED_USERNAMES` was added to the app after this runbook's first deploy** and is
easy to miss. It defaults to **empty, which means no allowlist is applied** — every account in
the database can log in, including the unused `admin`. Setting it to a comma-separated list is
what closes that. The code shipped in `1b663cb`; **whether the property is set on the server is a
separate question, and as of 2026-08-11 it was not.**

⚠️ **`SPRING_PROFILES_ACTIVE` is the first time this project's profiles have actually done
anything.** A prior deploy (`_archive/hosting/qa-setup.md`) set `SPRING_PROFILES_ACTIVE=qa` with
no matching `application-qa.yml` ever created — Spring silently activated a profile with nothing
in it, so it had zero effect. `application-profiles.yml` now exists (one multi-document file,
`---`-separated `dev`/`qa`/`prod` sections, pulled in via `application.yml`'s
`spring.config.import`) and scopes only the two allowlists above
(`security.login.allowed-usernames`, `security.registration.allowed-usernames`); everything else
in `application.yml` is unaffected by which profile is active and keeps resolving from `.env` the
same way it always has. **Unset here means no profile section applies and the app falls back to
`application.yml`'s own defaults** (login unrestricted if `LOGIN_ALLOWED_USERNAMES` is also
unset, registration closed if `REGISTRATION_ALLOWED_USERNAMES` is also unset) — the base file's
behavior, not a broken state.

⚠️ **`REGISTRATION_ALLOWED_USERNAMES` gates who may create their own account**, not who may log
in — a separate list from `LOGIN_ALLOWED_USERNAMES` on purpose, since being allowed to register
does not imply being allowed to log in if the login allowlist is ever tightened independently.
Unlike the login allowlist, **its default is closed, not open** — an unset value means nobody can
self-register, so leaving this blank is the safe failure mode rather than the dangerous one.

Optional, both with working defaults: `LOGIN_MAX_ATTEMPTS` (8) and `LOGIN_LOCKOUT_MINUTES` (15)
tune the login rate limiter.

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
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8081/api/auth/login \
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
curl -X POST 'http://localhost:8081/api/ingestion/clinicaltrials' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"condition":"breast cancer","maxStudies":5000}'

# 2. Backfill  (~25 min HERE; expect 2-3 HOURS on a small VPS)
curl -X POST 'http://localhost:8081/api/rag/backfill' -H "Authorization: Bearer $TOKEN"
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
turn this phase into a restore — see `../_archive/ingestion/DEPLOYMENT_SEEDING.md`. It is a chunking
change, so it costs a full re-index whenever it is adopted; cheaper now than later.

---

## Phase 5 — Her record

**Three files only. 4.4 KB.**

```bash
scp .claude/patient-data/patient-diagnosis.csv \
    .claude/patient-data/patient-variant.csv \
    .claude/patient-data/patient-prior-treatment.csv \
    cancer:/opt/cancer/.claude/patient-data/
```

On the server: `chmod 700` the directory, `chmod 600` the files.

⚠️ **`my-health-summary.pdf` (21 MB), `mri-scan.md` and `pet-scan-2026-03-16.md` never go to the
server.** Decided permanently 2026-08-11. `PatientSeedLoader` does not read them — they were
source documents for the CSVs. They are the largest concentration of her medical data and would
sit there for no functional reason.

Restart; `PatientSeedLoader` creates `AppUser` + the three patient rows. Then:

```bash
curl -s -H "Authorization: Bearer $TOKEN" 'http://localhost:8081/api/appuser?page=0&size=5'
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
    server_name breastcancertrialfinder.com www.breastcancertrialfinder.com;

    location / {
        proxy_pass http://localhost:8081;
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
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_read_timeout 4h;
        proxy_send_timeout 4h;
    }
}
```

Note `X-Forwarded-For $remote_addr` rather than `$proxy_add_x_forwarded_for`: the latter appends
to a client-supplied header, and the rate limiter reads the first hop. Overwriting means a
caller cannot spoof it.

Then TLS. **certbot was not installed** — none of the eight existing sites used HTTPS:

```bash
apt-get install -y certbot python3-certbot-nginx
certbot --nginx -d breastcancertrialfinder.com -d www.breastcancertrialfinder.com \
  --non-interactive --agree-tos -m jeb.seibel@yahoo.com --redirect
```

certbot rewrites the site file in place, adding the 443 block and the HTTP→HTTPS redirect, and
arms `certbot.timer` for renewal. Verify with `systemctl is-active certbot.timer`.

⚠️ **Do not test the vhost with `curl -H 'Host: ...' http://127.0.0.1`.** That matches nginx's
*default* server block, not yours, and returns a 502 from whatever the default proxies to —
which reads as "my app is broken" when nothing is wrong. Test against the real domain from
off-box.

⚠️ **Until TLS is live, do not log in over the public IP.** The JWT crosses the network in
cleartext on every request; anyone in path can lift and replay it. TLS is what makes the auth
work mean anything.

---

## Phase 6.5 — Change the password, with the script

**Use `hosting/change-password.sh`** (copy it to the server, `chmod 700`, run it there). It
prompts rather than taking the password as an argument — an argument lands in shell history and
in `ps` output — and it verifies the stored value is a real 60-character BCrypt hash before
reporting success.

⚠️ **That verification exists because of a real bug.** `UserService` did not hash passwords on
create or update until 2026-08-11; only `AuthController.register` did. Changing the production
password returned HTTP 200 with a normal-looking payload, wrote **plaintext** to the column, and
the new password then failed with 401 — with `password123` also dead, and `admin`'s password
unknown. Recovery took a full database rebuild.

Two lessons worth keeping:

- **Verify the stored hash, not the HTTP status.** A 200 from an update endpoint says the write
  was accepted, not that it was correct.
- **A rebuild is free before the corpus exists and expensive after.** If something needs
  resetting, do it now rather than after hours of embedding.

Also worth knowing: an interactive `mysql>` session holds an open transaction, so a `SELECT`
there shows your own uncommitted write. Root on another connection saw the old value while the
session showed the new one — an hour went into that. Use `mysql -e '...'` for verification, or
`COMMIT;` explicitly.

---

## Phase 7 — Verify before telling her it is ready

```bash
D=https://breastcancertrialfinder.com

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

- ~~**No authorization model.**~~ ✅ **Closed 2026-08-14** (`c9cb30d`), and no longer an accepted
  gap. `user_patient` grants plus a ranked `AccessLevel` (`VIEW_TRIALS < VIEW_RECORD <
  EDIT_RECORD < OWNER`) are enforced by `CurrentUserService` — endpoints still take the target
  extid from the URL, but now check it against the caller's grants and return **404, never 403**,
  so a probe cannot confirm a record exists. ⚠️ **It is on `frontend-mobile`, not `main`** — so
  it is neither deployed nor deployable until that branch is merged. Until then, this gap is
  still live in production exactly as originally written.
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
