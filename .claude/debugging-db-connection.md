# Debugging DB Connection Issue

## Status
Resolved. (Note: a second, separate schema issue surfaced after this one was fixed — see "Issue 2" below, also resolved.)

## Symptom
Backend fails to start. HikariPool connects successfully (`HikariPool-1 - Start completed`), but Spring context refresh fails shortly after with an UnsatisfiedDependencyException chain: `jwtAuthenticationFilter` → `customUserDetailsService` → `userRepository` → cannot resolve reference to bean `jpaSharedEM_entityManagerFactory`.

This means the JDBC connection itself is fine; the failure is in building the JPA EntityManagerFactory, which happens after Hikari connects but before the app is ready. The Tomcat/UnsatisfiedDependencyException output is a downstream symptom, not the root cause — the real error is expected earlier in the log, around EntityManagerFactory / Liquibase initialization.

## Environment
Local MySQL, not AWS RDS. `.env` values as of this investigation:
- RDS_HOSTNAME: localhost
- RDS_PORT: 3306
- RDS_DB_NAME: jobhunting
- RDS_USERNAME: jobhunting_user

`application.yml` has Liquibase `drop-first: true` (schema is dropped and recreated on every boot — expected in this non-production project).

## Leading hypothesis (superseded — see findings log)
Originally suspected a Liquibase changeset failure during schema rebuild, given `drop-first: true` re-runs the full changelog on every startup. Candidates, in run order, from `database/src/main/resources/db/changelog/changes/`: 001-customer, 002-user, 003-purchase, 004-company, 005-job_posting, 006-skill, 007-application, 008-contact, 009-friend, 010-job_posting_skill, 011-user_skill, 012-friend_skill, 013-friend_company, 014-friend_job_posting, 100-load-init-data. Ruled out by the DEBUG log evidence below — no changeset-level Liquibase output ever appeared, meaning Liquibase never got far enough to touch a changeset.

## Findings log

### DEBUG log added, first restart with `logging.level.liquibase: DEBUG`
Added `logging.level.liquibase: DEBUG` to `src/main/resources/application.yml`. Restarted and captured the full startup log.

Result: HikariPool connects and logs "Start completed" at 09:19:27.834. Then a 30-second gap with **zero** Liquibase changeset/lock-table log lines. At 09:19:57.851 (exactly 30s later) the Tomcat/EntityManagerFactory failure fires — same UnsatisfiedDependencyException chain as before (`jwtAuthenticationFilter` → `customUserDetailsService` → `userRepository` → cannot resolve `jpaSharedEM_entityManagerFactory`).

The 30-second gap exactly matches `spring.datasource.hikari.connection-timeout: 30000` in `application.yml`. Conclusion: Liquibase is not failing on a changeset — it never starts one. It's blocking trying to obtain a connection/lock and then timing out. This rules out the changeset-content hypothesis above.

### Hypothesis: stale DATABASECHANGELOGLOCK
Working theory: a previous run was killed/crashed mid-migration (plausible given the recent large package rename, commit `d5adddd`), leaving `DATABASECHANGELOGLOCK.LOCKED = 1` in the `jobhunting` schema. Liquibase then blocks trying to acquire the lock on every subsequent boot until the connection pool times out.

Given user manages the DB directly (never do this via the assistant — see project conventions), provided the check/fix SQL for the user to run themselves:
- Check: `SELECT * FROM DATABASECHANGELOGLOCK;`
- Clear stale lock: `UPDATE DATABASECHANGELOGLOCK SET LOCKED = 0, LOCKGRANTED = NULL, LOCKEDBY = NULL WHERE ID = 1;`
- Alternative: since `drop-first: true` wipes the schema every boot anyway, the n8n `clear-db` webhook (`http://localhost:5678/webhook/clear-db`, GET) would clear this as a side effect too.

Not yet confirmed which way the user went or what the lock table showed — investigation branched into a separate but likely related issue below before this was confirmed.

### New wrinkle: `jobhunting_user` MySQL account issue
While attempting to fix the DB user, user hit: `Error Code: 1396. Operation CREATE USER failed for 'jobhunting_user'@'localhost'`. This means the MySQL user already exists server-side — `CREATE USER` fails on a duplicate.

This is plausibly connected to the original hang: if `jobhunting_user` exists but is missing privileges (e.g. no DROP/CREATE/ALTER on the `jobhunting` schema needed for the `drop-first` rebuild, or no grants on `jobhunting` at all), Liquibase could stall or fail trying to acquire/verify the lock table or rebuild schema objects, producing the same symptom as the stale-lock theory.

Provided the user with options to run themselves:
- Check existence: `SELECT User, Host FROM mysql.user WHERE User = 'jobhunting_user';`
- Reset existing user (password + full grants on `jobhunting` schema):
  `ALTER USER 'jobhunting_user'@'localhost' IDENTIFIED BY '<password>';`
  `GRANT ALL PRIVILEGES ON jobhunting.* TO 'jobhunting_user'@'localhost';`
  `FLUSH PRIVILEGES;`
- Or drop and recreate cleanly: `DROP USER 'jobhunting_user'@'localhost';` then `CREATE USER ...` + `GRANT ALL PRIVILEGES ...` + `FLUSH PRIVILEGES;`

### Confirmation restart — fixed
After correcting the `jobhunting_user` MySQL account/privileges (see above), restarted again. This time the DEBUG log showed real Liquibase activity from the start — checksum computation and changesets actually running, e.g. `Running Changeset: db/changelog/changes/005-job_posting.yaml::create_job_posting_table::jeb` followed by the real `CREATE TABLE jobhunting.job_posting (...)` statement executing. No 30-second gap this time.

## Root cause
The `jobhunting_user` MySQL account was missing sufficient privileges (or otherwise misconfigured) on the `jobhunting` schema. This caused Liquibase to stall trying to acquire the changelog lock / operate on the schema, which silently ate the full `spring.datasource.hikari.connection-timeout` (30s) before Spring gave up on building the EntityManagerFactory — surfacing as an unrelated-looking `UnsatisfiedDependencyException` on `userRepository`/`jwtAuthenticationFilter` at the Tomcat startup layer. The stale-lock theory was a red herring; there was no `DATABASECHANGELOGLOCK` deadlock, just a privilege problem preventing Liquibase from progressing at all.

## Fix applied
User corrected the `jobhunting_user` MySQL account (existing user, needed privileges reset/granted rather than recreated, since `CREATE USER` failed with Error 1396 — user already existed) via `ALTER USER` / `GRANT ALL PRIVILEGES ON jobhunting.* TO 'jobhunting_user'@'localhost'` / `FLUSH PRIVILEGES`. After this, the backend started cleanly with Liquibase running the full changelog against the local MySQL instance.

## Follow-up (optional)
`logging.level.liquibase: DEBUG` was added to `src/main/resources/application.yml` for this investigation, then removed once Issue 1 was confirmed fixed. Re-added briefly to diagnose Issue 2 below, then should be removed again.

---

# Issue 2 — Liquibase changeset failure: source_url key too long

## Status
Resolved.

## Symptom
With the user/privilege fix from Issue 1 in place, Liquibase now progresses through changesets for real (confirming Issue 1's root cause was correct). It got through `001-customer.yaml` through `004-company.yaml` successfully, then failed on `005-job_posting.yaml::create_job_posting_table`:

`liquibase.exception.DatabaseException: Specified key was too long; max key length is 3072 bytes`, on the `CREATE TABLE jobhunting.job_posting (...)` statement, specifically the `UNIQUE (source_url)` constraint.

## Root cause
`005-job_posting.yaml` defined `source_url` as `varchar(1024)` with a unique constraint. MySQL InnoDB caps index key length at 3072 bytes. With `utf8mb4` (4 bytes/char, MySQL 8 default), `varchar(1024)` needs up to 4096 bytes for the unique index — over the limit. `varchar(255)` or similar ASCII-only assumptions from other projects don't hit this, which is why the older `001`–`004` changesets never surfaced the problem.

## Fix applied
Shrank `source_url` from `varchar(1024)` to `varchar(768)` (768 × 4 = 3072 bytes exactly, at the limit) in:
- `database/src/main/resources/db/changelog/changes/005-job_posting.yaml` (the actual DB column definition)
- `database/src/main/java/com/seibel/jobhunting/database/db/entity/JobPostingDb.java` (`@Column(name = "source_url", length = ...)`, kept in sync with the DB even though Liquibase — not Hibernate DDL — owns schema creation here)
- `src/main/java/com/seibel/jobhunting/web/request/RequestJobPostingCreate.java` and `RequestJobPostingUpdate.java` (`@Size(max = ...)` validation, was still 1024 and would have let invalid-length values reach the DB)
- `_archive/database/table-definitions.md` documentation updated to reflect the 768 cap and why

The unique constraint itself (dedup on `source_url` for re-import handling, per `_archive/database/table-definitions.md`) was kept — this is a length fix, not a constraint removal.

## Next step
Restart the backend once more to confirm `005-job_posting.yaml` and the remaining changesets (`006` through `100-load-init-data`) complete cleanly with no further key-length or other errors.
