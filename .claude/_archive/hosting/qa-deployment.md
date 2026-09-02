# QA Deployment

> **Rewritten 2026-09-02 from a real deploy, per `qa-setup.md`'s own instruction not to adapt
> another project's runbook.** The previous version of this file (checked in briefly on
> `docs-and-build-cleanup`) was inherited from another project unchanged — wrong schema name
> (`jobhunting` instead of `cancer`), a `DROP DATABASE jobhunting` that would have hit the wrong
> database entirely, and a Liquibase checksum-clear command naming changesets that don't exist
> in this project. `qa-setup.md` had already deleted that exact content once, for the same
> reasons, before it reappeared. **Every value below marked TODO is unverified — fill it in from
> the box, don't guess.**

Routine deploy of **cancer** to the provisioned QA host. First-time provisioning is in
`qa-setup.md`.

Assumes `cancer` is a working `~/.ssh/config` host alias (confirmed working this session).

---

## Confirmed this session (2026-09-02)

- **SSH alias**: `cancer` — `ssh cancer` connects.
- **Database schema name**: `cancer`, not `jobhunting`. Confirm with `SHOW DATABASES;` before
  running anything destructive — don't trust a doc, including this one, over what's actually on
  the box.
- **Port 8080 is already taken on this box by another app.** Starting this app with the default
  port fails. Set `PORT=<a free port>` in `/opt/cancer/.env` — TODO: confirm and record the
  actual port once chosen; `CURRENT_STATE.md` used 8081 on the production box for the same
  reason, but that is a different box and may not be free here too.
- **`ANTHROPIC_API_KEY` is not set in this box's `.env`.** AI features (trial check, friendly
  titles, diagnosis intake) stay disabled until it's added — the app boots fine without it, by
  design, so this is only worth fixing if AI features are actually wanted on QA.
- **Liquibase checksum failures are a live risk on this box**, not a hypothetical — see
  "Checksum failures" below. Hit this session on `011-trial-status.yaml`,
  `023-patient-diagnosis.yaml`, `024-trial-match.yaml`, `026-patient-variant.yaml`, and
  `027-patient-prior-treatment.yaml`.

## TODO — not yet verified, confirm on the box before trusting

- Deployed jar path and filename convention (`/opt/cancer/cancer.jar`? something else?)
- Database username in `.env`
- Whether `/opt/cancer/`, `/var/log/cancer/`, and the systemd unit name (`cancer.service`) from
  `qa-setup.md` actually match what's running on this box
- Whether this is the same box `qa-setup.md` provisioned, or a different one

---

## Before you start

- Confirm the schema name and SSH alias against the box itself (`SHOW DATABASES;`, `ssh cancer`)
  rather than trusting either doc — this file has been wrong once already.
- Bump `version` in `../../../build.gradle` if this is a release rather than a rebuild of the
  same code.
- Confirm nothing in `SecurityConfig` has been loosened for local convenience — a permit-all
  left in place is invisible until it is on a public URL. `qa-setup.md`'s security checklist has
  more on this.
- Have the server `.env` values to hand if the deploy adds a new required variable. A missing
  `JWT_SECRET` fails at startup with no fallback, deliberately.

---

## The deploy

**1. Build the jar.** From the repo root on the build machine:

```bash
./gradlew clean buildDeployment
```

`buildDeployment`, not `build`. It chains `npmInstall` → `npmBuild` → `cleanStatic` →
`copyFrontend` → `build`, so the React app is compiled and copied into the jar's static
resources. Plain `build` produces a jar that serves whatever stale frontend was last left in
`../../../src/main/resources/static` — or a blank page if that directory is empty.

There is no separate frontend upload step. One artifact ships.

**2. Upload.** TODO: confirm the real deployed path/filename on this box, then:

```bash
scp -O build/libs/<jar-name>.jar cancer:<confirmed-deploy-path>
```

**3. Restart.** `ssh cancer sudo systemctl restart cancer` (TODO: confirm this is the actual
unit name), then tail the logs until Spring reports the application started.

**4. Verify.** Load the site, log in, and open one page that reads from the database. A
successful systemd start only proves the process is alive — Liquibase, the datasource, and the
bundled frontend are all still capable of being wrong at that point.

---

## Checksum failures

**Hit this session.** Liquibase refuses to start with something like:

```
Validation Failed:
     5 changesets check sum
          db/changelog/changes/011-trial-status.yaml::create_trial_status_table::jeb was: ... but is now: ...
```

This means the changeset file on disk no longer matches what Liquibase recorded when it first
ran against this database — editing an already-applied changeset, per the project's own rule
(root `CLAUDE.md`): **`drop-first` is `false`, so an edit does not simply re-run.**

Two fixes, per `CLAUDE.md` / the `database-column-change` skill:

- **Clear the checksum for just the affected changesets**, so Liquibase re-stamps them from the
  current file content without re-running them (data-preserving):

  ```sql
  UPDATE DATABASECHANGELOG SET MD5SUM = NULL
  WHERE ID IN ('create_trial_status_table', 'create_patient_diagnosis_table', /* ... */);
  ```

  Use the exact `ID` values from the failure message, not filenames — they differ.

- **Full rebuild** (destroys all data in that database): drop and recreate the schema, let
  Liquibase run every changeset fresh. Reasonable for a QA box with disposable/re-seedable data;
  confirm the schema name first.

  ```sql
  DROP DATABASE cancer;
  CREATE DATABASE cancer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  GRANT ALL PRIVILEGES ON cancer.* TO '<db_user>'@'localhost';
  FLUSH PRIVILEGES;
  ```

  Back up first even on a QA box you consider disposable — `mysqldump` costs nothing next to
  wanting the dump later.

**Never guess the schema name from an old doc.** Confirm with `SHOW DATABASES;` on the box
first — this file named the wrong one (`jobhunting`) for a while.

---

## Port conflicts

If the app fails to bind, or something else responds on the port you expect: another process
may already be listening. Check with `ssh cancer sudo ss -tlnp | grep <port>` before assuming
the app is broken. Set `PORT=<free port>` in `/opt/cancer/.env` and restart. If Nginx sits in
front of this app, its proxy_pass target has to match whatever port is actually chosen.

---

## When it does not come up

| Symptom | Where to look |
|---------|---------------|
| Service restarts in a loop | The app's error log — usually a missing required env var |
| `JWT_SECRET` error at startup | Absent or too short in `.env`. There is no fallback, on purpose |
| Liquibase checksum failure | A changeset was edited after it ran, or the schema is shared with a database that ran an older version of the changelog. See "Checksum failures" above |
| Blank page, API works | Built with `build` instead of `buildDeployment` — no frontend in the jar |
| AI features silently unavailable | `ANTHROPIC_API_KEY` unset in `.env`. By design the app boots without it |
| Bind failure / wrong app responds on the expected port | Another process already owns that port on this box. Check before assuming the app is broken; set `PORT` in `.env` |
