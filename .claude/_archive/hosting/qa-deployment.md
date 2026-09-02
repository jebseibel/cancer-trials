# QA Deployment

Routine deploy of **jobhunting** to a provisioned QA host. First-time provisioning is in
`qa-setup.md`.

Assumes `cancer` is a working `~/.ssh/config` host alias and that the server layout
from the setup guide exists.

---

**Follow these steps**
1. Upgrade gradle.build
2. build the deployment
3. tail -f /var/log/cancer/app.log
4. systemctl stop cancer
5. scp -O build/libs/cancer-1.0.3-SNAPSHOT.jar jobhunting:/opt/cancer/cancer.jar
6. cp cancer.jar releases/cancer-1.0.X.jar
7. use cancer
8. UPDATE DATABASECHANGELOG SET MD5SUM = NULL WHERE ID IN ('load-job-source-data', 'load-init-data');
9. systemctl start cancer


## Before you start

- Bump `version` in `../../../build.gradle` (line 9) if this is a release rather than a rebuild of
  the same code.
- Confirm nothing in `SecurityConfig` has been loosened for local convenience — a
  permit-all left in place is invisible until it is on a public URL.
- Have the server `../../../.env` values to hand if the deploy adds a new required variable. A
  missing `JWT_SECRET` fails at startup with no fallback, deliberately.

---

## The deploy

**1. Watch the logs in a second terminal.** Tail `tail -f /var/log/cancer/app.log` on the
server so you see the startup succeed or fail as it happens rather than after the fact.

**2. Stop the service.** `systemctl stop cancer`.

**3. Back up the database.** `mysqldump --no-tablespaces` the `jobhunting` schema into
`/opt/cancer/db_backup/` with a timestamped filename, sourcing `/opt/cancer/.env`
so the password never appears in shell history or the process list.

Take this backup even on a deploy you expect to be trivial. Liquibase runs on startup, and
the cost of having the dump is zero next to the cost of wanting one.

**4. Build the jar.** From the repo root on the build machine:



```
./gradlew clean buildDeployment
```

`buildDeployment`, not `build`. It chains `npmInstall` → `npmBuild` → `cleanStatic` →
`copyFrontend` → `build`, so the React app is compiled and copied into the jar's static
resources. Plain `build` produces a jar that serves whatever stale frontend was last left
in `../../../src/main/resources/static` — or a blank page if that directory is empty.

There is no separate frontend upload step. One artifact ships.

**5. Upload.** `scp build/libs/jobhunting-*.jar` (renamed from `cancer-*` — the bootJar
artifact name changed 2026-08-28, see `build.gradle`) to `/opt/cancer/cancer.jar`. The
systemd unit points at that fixed filename, unchanged by the artifact rename:

```
scp -O build/libs/jobhunting-1.0.7-SNAPSHOT.jar cancer:/opt/cancer/cancer.jar
```

**6. Archive the release.** `scp` the same versioned jar to `/opt/cancer/releases/`.
This is the rollback path: on a bad deploy, copy a known-good jar from `releases/` over
`cancer.jar` and restart, no rebuild required.

**7. Restart.** `systemctl restart cancer`, then watch the tail from step 1 until
Spring reports the application started.

**8. Verify.** Load the site over HTTPS, log in, and open one page that reads from the
database. A successful systemd start only proves the process is alive — Liquibase, the
datasource, and the bundled frontend are all still capable of being wrong at that point.

---

## Rebuilding the database

Only when you actually intend to lose the QA data.

`spring.liquibase.drop-first` is `false`, so a restart preserves whatever is there. That
also means an already-applied changeset cannot be edited in place — Liquibase validates
checksums and refuses to start on a modified file. If you hit that, the fix is either a new
changeset or a deliberate rebuild.

To rebuild: stop the service and **back up first** —

```bash
sudo systemctl stop cancer
mysqldump -u jobhunting_user -p jobhunting > /opt/cancer/db_backup/pre-rebuild-$(date +%F-%H%M).sql
```

Then, as root (`sudo mysql`):

```sql
DROP DATABASE jobhunting;

CREATE DATABASE jobhunting CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Dropping the schema drops its grants with it. The user still exists, so this is a
-- re-grant rather than a CREATE USER — running CREATE USER again would error.
GRANT ALL PRIVILEGES ON jobhunting.* TO 'jobhunting_user'@'localhost';

FLUSH PRIVILEGES;
```

Then start the service and let Liquibase recreate everything:

```bash
sudo systemctl start cancer
```

The character set matters on the recreate as much as on the first create — a rebuild that
silently drops back to `utf8` will mangle every em dash and curly quote in a job
description from that point on.

Note this differs from local development, where "rebuild the database" means hitting the
n8n webhook at `http://localhost:5678/webhook/clear-db`. That webhook is local-only and
does not exist on the QA host.

---

## Rollback

1. `systemctl stop cancer`
2. Copy the previous jar from `/opt/cancer/releases/` over
   `/opt/cancer/cancer.jar`
3. `systemctl start cancer`

If the bad deploy included a Liquibase changeset, the schema is already migrated and the
older jar may not run against it. That is when the step-3 dump earns its place: restore it
into a freshly dropped schema before starting the old jar.

---

## When it does not come up

| Symptom | Where to look |
|---------|---------------|
| Service restarts in a loop | `/var/log/cancer/error.log` — usually a missing required env var |
| `JWT_SECRET` error at startup | Absent or too short in `/opt/cancer/.env`. There is no fallback, on purpose |
| Liquibase checksum failure | A changeset was edited after it ran. New changeset, or rebuild |
| Blank page, API works | Built with `build` instead of `buildDeployment` — no frontend in the jar |
| 413 on import upload | `client_max_body_size` in nginx below the app's 32MB multipart limit |
| 502 from nginx | App not listening on 8080. Check `systemctl status cancer` first |
| AI features silently unavailable | `ANTHROPIC_API_KEY` unset. By design the app boots without it |
