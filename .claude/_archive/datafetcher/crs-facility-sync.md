# CRS Facility Sync

**Status:** Planning
**Created:** 2025-11-20

## Overview

Daily batch process that propagates changes from CRS tables into the Facility table based on business rules.

Note: Client will probably change his mind about this process.

## Data Flow

```
CRS Pending  ──┐
               ├──→ [Business Rules] ──→ Facility
CRS Approved ──┘
```

**Direction:** One-way (CRS → Facility)
**Frequency:** Daily batch or On demand

## Source Tables

- `crs_pending` - Pending renewable energy certificates
- `crs_approved` - Approved renewable energy certificates

## Target Table

- `facility` - Facility master data

## Business Rules

### UniqueId Construction

Both CRS tables use the same formula:
```
UniqueId = Tracking System + " " + Tracking System Id Number
```
This matches against `Facility.UniqueId` (already exists in table).

---

### CRS Approved → Facility

```
For each CRS Approved row:
├─ Lookup Facility where Approved.UniqueId = Facility.UniqueId
│
├─ NO MATCH:
│     • Create new Facility row
│     • Set Facility.CreateDate = now()
│     • Set NERC region (TBD - source unclear)
│     • Ensure lookup is formulated
│     → Move to next row
│
└─ MATCH FOUND (1 row):
      • Compare records field by field
      ├─ SAME → Move to next row (no changes)
      └─ DIFFERENT:
           • Auto-update fields: Update Facility directly
           • Review-required fields: Add to CHANGE TABLE
           → Move to next row
```

**Note:** CRS makes data entry mistakes several times a month/year. Review-required fields catch these for human approval.

---

### CRS Pending → Facility

```
For each CRS Pending row:
├─ Lookup Facility where Pending.UniqueId = Facility.UniqueId
│
├─ NO MATCH + Previous Attestation Date is BLANK:
│     • Create new Facility row
│     • Set Facility.crsTrackingAttestationStatus = 'NEW_APPLICATION'  ← name() constant, CrsTrackingAttestationStatus, NOT FacStatus
│     → Move to next row
│
├─ NO MATCH + Previous Attestation Date is POPULATED:
│     • Create new Facility row
│     • Set Facility.crsTrackingAttestationStatus = 'APPROVED_PENDING_RENEWAL'  ← name() constant, CrsTrackingAttestationStatus, NOT FacStatus
│     → Move to next row
│
└─ MATCH FOUND:
      • Set Facility.previous_attestation_date = Pending.previous_attestation_date
      • Set Facility.crsTrackingAttestationStatus = 'APPROVED_PENDING_RENEWAL'  ← name() constant, CrsTrackingAttestationStatus, NOT FacStatus
      → Move to next row

Note: CRS load tables store raw display values as received. The transform step resolves
these to name() constants via fromDisplayValue() before writing to production tables.
```

---

### Field Categories

Fields are categorized into two types:

| Category | Behavior | Examples |
|----------|----------|----------|
| **Auto-update** | Sync directly to Facility | TBD |
| **Review-required** | Add to CHANGE TABLE for approval | Likely data entry error fields |

**Note:** Compare all fields except BaseDb fields (id, created_at, updated_at, etc.). Detailed mapping TBD.

---

### CHANGE TABLE

A review/approval workflow table for mismatched data:

| Column | Description |
|--------|-------------|
| id | Primary key |
| facility_id | Reference to Facility |
| crs_source | 'approved' or 'pending' |
| crs_record_id | Source CRS record ID |
| field_name | Which field changed |
| old_value | Current Facility value |
| new_value | Incoming CRS value |
| status | pending / approved / rejected |
| reviewed_by | User who reviewed |
| reviewed_at | Review timestamp |
| created_at | When change detected |

**Workflow:** User reviews changes → approves (apply to Facility) or rejects (discard).

**Note:** This is significant work. Client may simplify requirements.

---

### Mistake Process

When CRS makes the same data entry error across multiple days, track these for reporting back to CRS.

**Purpose:** Send list of repeated errors to CRS so they can fix their data.

**Potential Structure:**

| Column | Description |
|--------|-------------|
| id | Primary key |
| facility_id | Reference to Facility |
| field_name | Field with repeated error |
| expected_value | Correct value (from Facility) |
| crs_value | Wrong value CRS keeps sending |
| first_seen | First date error appeared |
| last_seen | Most recent date |
| occurrence_count | How many times seen |
| reported_to_crs | Boolean - has this been sent? |
| reported_at | When it was reported |

**Open questions:** See questions 9-12 in Open Questions section.

### Field Mappings

| CRS Field | Facility Field | Category | Notes |
|-----------|----------------|----------|-------|
| TBD | TBD | TBD | Working on this |

## Audit Trail

Requirements TBD - client wants audit trail but details unclear.

Potential tracking:
- What changed
- When it changed
- Which CRS record triggered the change
- Previous vs new values

## Implementation Design

<!-- To be determined after rules are defined -->

## Open Questions

### Resolved
- ✅ CHANGE TABLE purpose - Review/approval workflow for CRS data entry errors
- ✅ What happens with differences - Auto-update some fields, review others
- ✅ Which fields to compare - All except BaseDb fields
- ✅ Multiple Facility matches - Client says "won't happen". Add error handling later when proven wrong.
- ✅ Execution order - **Approved first, then Pending**
- ✅ CRS Pending fields - Only 4 fields come from CRS Pending, only 2 need to sync (attestation_date, status). Current logic is complete.
- ✅ NERC region - Will come from future rules (lookup/derivation). Should exist 99.99% of the time.

### Pending - Mistake Process
9. How many days/occurrences before it's flagged as a reportable mistake?
10. Automatic detection or manually flagged from CHANGE TABLE?
11. What format does CRS want for the report? (CSV, email, specific template?)
12. Does rejecting a CHANGE TABLE item automatically add it to Mistake tracking?

### Deferred
- Audit trail specifics (beyond CHANGE TABLE)
- Error handling strategy
- Notification on failures?
- Facility deactivation

