# Handoff — `batch_uuid` → `upload_id` FK Migration

**Branch:** `main` (uncommitted)
**Status:** Code changes complete and compiling. DB rebuild webhook fired. **Backend needs restart + end-to-end verification.**
**Plan file:** `/home/jeb/.claude/plans/write-up-the-migration-crystalline-abelson.md`

---

## What This Migration Did

The 12 retirement-certificate tables (`ret_wregis_*`, `ret_mrets_*`, `ret_nar_*`, `ret_ercot_*`) used to denormalize five batch-level fields onto every row:

| Old column on cert tables | Now lives on `retirement_upload` |
|---|---|
| `batch_uuid` (string) | `batch_uuid` (already there, unchanged) |
| `document_extid` | accessed via `upload.extid` |
| `document_name` | `filename` |
| `customer_extid` | **new column** `customer_extid` (added to retirement_upload) |
| `reporting_year` | `year` (already there) |

Cert tables now carry a single FK column: `upload_id BIGINT NOT NULL REFERENCES retirement_upload(id) ON DELETE RESTRICT`, with an index `idx_ret_<table>_upload_id`.

`ret_ncrets_basic` is **out of scope** — it doesn't extend `BaseRetCertDb` and was left alone.

The `transactions` table is **out of scope** — it intentionally denormalizes as a historical snapshot.

---

## Files Modified

### Liquibase (edits to existing changesets — no new files)
- `database/src/main/resources/db/changelog/changes/008-ret-tables.yaml` — 12 cert-table changesets: dropped 5 columns, added `upload_id` column + index + FK
- `database/src/main/resources/db/changelog/changes/008-create-retirement-upload.yaml` — added `customer_extid VARCHAR(64)` column

### JPA entities & domain
- `database/.../entity/retire/BaseRetCertDb.java` — removed 5 fields, added `Long uploadId`
- `database/.../entity/retire/RetirementUploadDb.java` — added `customerExtid`
- `common/.../domain/retire/BaseRetCertDomain.java` — removed 5 fields, added `Long uploadId`
- `common/.../domain/retire/RetirementUpload.java` — reparented `extends BaseRetCertDomain` → `extends BaseRetDomain`, added `customerExtid`

### Util & helpers
- `src/main/java/com/viro/app/util/RetCertRecordSetter.java` — `populateCommonFields(records, Long uploadId)` (one-arg)
- `src/main/java/com/viro/app/util/RetirementUploadHelper.java` — `createAndSave` gained `customerExtid` param; `recalculateByBatchUuid` → `recalculateByUploadId(Long)`
- `src/main/java/com/viro/app/util/RetCertPromotionHelper.java` — `Supplier<String> batchUuidSupplier` → `Supplier<Long> uploadIdSupplier`; removed per-record customerExtid gate

### 12 cert repositories
Each `Ret*Repository.findByBatchUuid(String) → findByUploadId(Long)`. `RetirementUploadRepository.findByBatchUuid` intentionally kept.

### 12 cert DbServices
- `createAll(items, String batchUuid) → createAll(items, Long uploadId)`
- `findByBatchUuid(String) → findByUploadId(Long)`
- `getBatchUuid(extid) → getUploadId(extid)` returning `Long`

### `RetirementUploadDbService`
- `update()` now sets `customerExtid` from incoming
- `recalculateStatus()` drives counts off `upload.getId()`, helper renamed accordingly

### 12 retire services
- Company lookup moved **before** `createAndSave` so `customerExtid` is written onto the upload row
- `populateCommonFields(records, savedUpload.getId())`
- `dbService.createAll(records, savedUpload.getId())`
- `updateStatus`: resolves upload via `dbService.getUploadId(extid)` then reads `transferUpload.getCustomerExtid()`
- `bulkPromote`: lambda resolves upload from `record.getUploadId()` then reads `bulkUpload.getCustomerExtid()`

### Transfer path
- `RetirementTransactionMapper.mapToTransaction(source, customerExtid, RetirementUploadDb upload)` — interface now takes the upload row
- `RetirementTransferService` — injects `RetirementUploadDbService`; calls `resolveUpload(record)` before each mapper invocation
- 12 mapper classes — read `batchUuid`/`documentName`/`documentExtid`/`reportingYear` off the upload row (null-safe)

### REST surface
- `RetirementController` — `findByBatchUuid(upload.getBatchUuid()) → findByUploadId(upload.getId())`
- `RetMretsCertQuantController`, `RetWregisCertQuantController`, `RetWregisTransDetailsController` — dropped `.batchUuid(item.getBatchUuid())` builder line
- `ResponseRetMretsCertQuant`, `ResponseRetWregisCertQuant`, `ResponseRetWregisTransDetails` — dropped `private String batchUuid` field (FE never read it; confirmed via grep)

---

## Build Status

- ✅ `./gradlew compileJava` — **passes clean**
- ❌ `./gradlew compileTestJava` — 26 errors, **all in AiPrompt tests** (database module), **unrelated to this migration**. Pre-existing drift from earlier AI-prompt work.

---

## What's Done vs Pending

### Done
1. ✅ Liquibase changesets (12 cert tables + retirement_upload)
2. ✅ Entities + domain classes
3. ✅ `RetCertRecordSetter`
4. ✅ 12 repositories + DbServices
5. ✅ 12 retire services
6. ✅ 12 transfer mappers + `RetirementTransactionMapper` interface + `RetirementTransferService`
7. ✅ Promotion helper + `RetirementUploadHelper`
8. ✅ REST converter cleanup (3 response DTOs, 3 controllers)
9. ✅ `compileJava` passes
10. ✅ **DB rebuild webhook fired** (`curl http://localhost:5678/webhook/clear-viro-db` returned HTTP 200 — workflow started async on n8n)

### Pending — user must do
1. **Restart the backend** (per CLAUDE.md, user handles this — Claude must not start/stop the backend).
2. **End-to-end smoke test:**
   - Upload one PDF for each AI-based tracking system (WREGIS TransConfirm, MRETS TransConfirm, NAR CertSubacct/RetireCert/VolComply, ERCOT Screenshot) via the FE.
   - Verify each cert-table row has `upload_id` set and the five old columns are gone.
   - Verify the upload row has `customer_extid` populated.
   - Promote one record and confirm a complete `transactions` row gets created (the transfer mapper now reads from the upload row).
3. **Snapshot tests:** `./gradlew :api-validation:verifySnapshots` once backend is up. If shapes drift, re-capture with `./gradlew :api-validation:captureSnapshots`.

### Known follow-ups (not done in this session)
- **AiPrompt test errors** (26 errors in `database/src/test/...AiPrompt*Test.java`) — pre-existing drift, blocks `compileTestJava`. Not caused by this migration. Address separately.
- **`transactions` table** still denormalizes `batch_uuid`, `customer_extid`, `document_extid`, `document_name`, `reporting_year` — intentional, as it's a historical snapshot. Documented as out of scope.
- **Documentation drift**: `.claude/_archive/database/*` and `.claude/_archive/current_issues/*` may still describe the old field names. Worth a doc pass once you've verified the smoke test passes.

---

## Quick Pick-Up Commands

```bash
# Verify the migration applied
mysql -u root viro -e "DESCRIBE ret_mrets_trans_confirm" | grep -E 'upload_id|batch_uuid|customer_extid'
# Expect: upload_id present, others absent

# Verify retirement_upload has customer_extid
mysql -u root viro -e "DESCRIBE retirement_upload" | grep customer_extid

# Verify FKs exist
mysql -u root viro -e "
SELECT TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA='viro' AND REFERENCED_TABLE_NAME='retirement_upload'"
# Expect: 12 rows, one fk_ret_*_upload per cert table

# Rebuild compile if anything looks off
./gradlew compileJava
```

---

## Notes for Whoever Picks This Up

- The user's preference, stated in this session and worth honoring: **simple-ID FKs over UUID/extid joins** for human-readable joins. That's why `upload_id BIGINT` was chosen over `upload_extid VARCHAR`.
- The user runs the backend start/stop themselves — never start it from Claude.
- Liquibase rule from `.claude/CLAUDE.md`: **edit existing changeset files in place; do NOT create new migration files**. This migration honored that.
- The webhook for "rebuild database" is `GET http://localhost:5678/webhook/clear-viro-db`. It returns HTTP 200 immediately ("Workflow was started"); the actual DB drop+recreate runs async on n8n's side.
