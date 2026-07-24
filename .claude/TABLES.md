# Clinical Trials Finder — Database Tables

Companion document to `PROJECT_PLAN.md`, broken out for iterating on the schema
independently. MySQL, managed via Liquibase changelogs.

## Conventions

Every core table (everything except pure join tables) carries the same base fields,
matching the `BaseDb` pattern used in the cpss reference project:

```
id              bigint          not null, primary key, auto_increment
extid           varchar(36)     not null, unique                  -- UUID, external-facing id
created_at      datetime        not null, default CURRENT_TIMESTAMP
updated_at      datetime
deleted_at      datetime                                          -- soft delete
active          int             default 1                          -- ACTIVE/INACTIVE
```

Below, each table lists only its *own* fields beyond these base fields. Join tables
(pure many-to-many link tables with no independent identity) skip the base fields
entirely and are just FK pairs, noted as such.

Types shown are MySQL column types as they'll appear in the Liquibase changeset
(`bigint`, `varchar(n)`, `text`, `datetime`, `date`, `tinyint(1)` for boolean, `int`).

---

## Staging / raw ingestion

### `trial_source`
Registry of data sources.

```
code            varchar(32)     not null, unique     -- e.g. CLINICALTRIALS_GOV, EU_CTR
name            varchar(128)    not null
base_url        varchar(255)
```

### `staging_raw_trial`
One row per fetch of a trial from a source, before normalization.

```
trial_source_id       bigint      not null    -- FK -> trial_source.id
source_trial_id       varchar(64) not null    -- e.g. NCT number, or scraper's own key
raw_payload            longtext    not null    -- JSON as returned by the source
fetched_at             datetime    not null
normalized_at          datetime                -- null = pending normalization
normalization_error    text                    -- populated if mapping failed
```

---

## Core normalized schema

### `trial`
One row per trial, deduplicated across sources by matching identifier (primarily NCT
number when present).

```
nct_id                        varchar(16)     unique     -- null for non-CT.gov-only trials
brief_title                   varchar(500)    not null
official_title                varchar(500)
overall_status                varchar(32)                -- RECRUITING, COMPLETED, TERMINATED, etc.
study_type                    varchar(32)
brief_summary                 text
detailed_description           text
start_date                    date
primary_completion_date        date
completion_date                date
last_update_posted_date        date
enrollment_count               int
enrollment_type                varchar(16)
healthy_volunteers             tinyint(1)
sex                            varchar(8)                 -- ALL, MALE, FEMALE
minimum_age                    varchar(32)                -- free text as returned, e.g. "18 Years"
maximum_age                    varchar(32)
eligibility_criteria            text                       -- raw narrative, inclusion/exclusion text
is_paid_study                   tinyint(1)                 -- not from CT.gov API, see note
paid_amount                     decimal(10,2)              -- not from CT.gov API, see note
primary_trial_source_id         bigint          not null   -- FK -> trial_source.id
```

Note: `healthy_volunteers` already answers "does this trial accept healthy
volunteers" (from CT.gov's `eligibilityModule.healthyVolunteers`) — no separate flag
needed for that.

`is_paid_study` and `paid_amount` are **not present in the ClinicalTrials.gov API** —
compensation/payment isn't part of its schema. These are populated either manually
(editable on the Trial Detail screen) or by a future Playwright scraper targeting a
source that lists compensation; the CT.gov normalizer always leaves them null on
initial ingestion. Since both paths can write these columns, treat manual edits as
authoritative — a re-normalization pass from CT.gov should never overwrite them once
set.

Note: no single `phase` column — a trial can list multiple phases, so phase lives
only in the `trial_phase` join table below to avoid redundant/conflicting data.

### `trial_phase`
Join table — a trial can have multiple phases.

```
trial_id        bigint          not null    -- FK -> trial.id
phase           varchar(16)     not null    -- PHASE1, PHASE2, PHASE3, PHASE4, NA
```

### `trial_std_age`
Join table — standardized age groups.

```
trial_id        bigint          not null    -- FK -> trial.id
std_age         varchar(16)     not null    -- CHILD, ADULT, OLDER_ADULT
```

### `sponsor`
Deduplicated sponsor/collaborator organizations.

```
name            varchar(255)    not null, unique
org_class       varchar(32)
```

### `trial_sponsor`
Join table.

```
trial_id        bigint          not null    -- FK -> trial.id
sponsor_id      bigint          not null    -- FK -> sponsor.id
role            varchar(16)     not null    -- LEAD, COLLABORATOR
```

### `condition`
Deduplicated condition/disease names.

```
name            varchar(255)    not null, unique
```

### `trial_condition`
Join table. Describes what the trial *studies* (its subject matter) — not a
requirement a patient must meet. See `trial_eligibility_condition` below for
eligibility requirements.

```
trial_id        bigint          not null    -- FK -> trial.id
condition_id    bigint          not null    -- FK -> condition.id
```

### `medication`
Deduplicated medication/drug names, used for eligibility requirements (distinct from
`intervention`, which describes what the trial itself administers).

```
name            varchar(255)    not null, unique
```

### `eligibility_rule`
Self-referencing expression tree capturing a trial's eligibility logic as structured
boolean rules, so code can evaluate them against a patient profile instead of just
displaying free text for a human to read. Populated manually while reading a trial's
free-text `eligibility_criteria`.

Each row is one node in the tree — either a **group node** (a boolean operator with
children) or a **leaf node** (a single testable fact). A group node's children are
the other `eligibility_rule` rows whose `parent_rule_id` points back to it; their
order among siblings is given by `sort_order`. A trial's rule tree has exactly one
root row (`parent_rule_id is null`) per trial — typically an AND group.

```
trial_id            bigint          not null    -- FK -> trial.id
parent_rule_id      bigint                      -- FK -> eligibility_rule.id (self); null = root node
node_type            varchar(8)      not null    -- GROUP, LEAF
operator             varchar(8)                  -- AND, OR, NOT — set only when node_type = GROUP
criterion_type        varchar(16)                 -- CONDITION, MEDICATION — set only when node_type = LEAF
criterion_id          bigint                      -- FK -> condition.id or medication.id (per criterion_type) when node_type = LEAF
requirement_type       varchar(16)                 -- REQUIRED, EXCLUDED, PRIOR_FAILED — set only when node_type = LEAF
sort_order             int             not null    -- ordering among sibling nodes, for display and deterministic tree walks
notes                  text                        -- free-form nuance, e.g. "stage III or IV only", "must have failed first-line chemo"
```

Example — "(condition A AND medication B) OR condition C":
```
id  parent_rule_id  node_type  operator  criterion_type  criterion_id  requirement_type  sort_order
1   null            GROUP      OR        -                -             -                 0
2   1               GROUP      AND       -                -             -                 0
3   2               LEAF       -         CONDITION        <A.id>        REQUIRED          0
4   2               LEAF       -         MEDICATION        <B.id>        REQUIRED          1
5   1               LEAF       -         CONDITION        <C.id>        REQUIRED          1
```

Root node (id 1) is an OR of: [AND-group (id 2)] and [leaf condition C (id 5)].
The AND-group (id 2) has children: [leaf condition A (id 3)] and [leaf medication B
(id 4)]. Evaluation code walks the tree from the root, recursing into GROUP children
and testing LEAF nodes against the patient profile.

### `keyword`
Deduplicated keywords.

```
name            varchar(255)    not null, unique
```

### `trial_keyword`
Join table.

```
trial_id        bigint          not null    -- FK -> trial.id
keyword_id      bigint          not null    -- FK -> keyword.id
```

### `arm_group`
Per-trial arm/group.

```
trial_id        bigint          not null    -- FK -> trial.id
label           varchar(128)    not null
type            varchar(32)
description     text
```

### `intervention`
Per-trial intervention.

```
trial_id        bigint          not null    -- FK -> trial.id
type            varchar(32)                 -- DRUG, DEVICE, OTHER, etc.
name            varchar(255)    not null
description     text
```

### `intervention_arm_group`
Join table linking interventions to arm groups.

```
intervention_id     bigint      not null    -- FK -> intervention.id
arm_group_id        bigint      not null    -- FK -> arm_group.id
```

### `outcome`
Primary/secondary outcomes.

```
trial_id        bigint          not null    -- FK -> trial.id
outcome_type    varchar(16)     not null    -- PRIMARY, SECONDARY
measure         varchar(500)    not null
description     text
time_frame      varchar(255)
```

### `location`
Per-trial site.

```
trial_id        bigint          not null    -- FK -> trial.id
facility        varchar(255)
city            varchar(128)
state           varchar(128)
zip             varchar(16)
country         varchar(128)
status          varchar(32)                 -- site-level recruiting status, when available
latitude        decimal(9,6)
longitude       decimal(9,6)
```

### `overall_official`
Per-trial contacts.

```
trial_id        bigint          not null    -- FK -> trial.id
name            varchar(255)    not null
affiliation     varchar(255)
role            varchar(64)
```

---

## Personal tracking

The main daily-use feature beyond raw storage.

### `app_user`
Seeded users (you / your wife).

```
username         varchar(64)     not null, unique
password_hash    varchar(255)    not null
display_name     varchar(128)
```

### `trial_status`
Links a user's relationship to a trial. Unique on (`trial_id`, `app_user_id`).

```
trial_id            bigint          not null    -- FK -> trial.id
app_user_id         bigint          not null    -- FK -> app_user.id
status              varchar(16)     not null    -- SAVED, INTERESTED, CONTACTED, RULED_OUT, ENROLLED
notes               text
status_changed_at   datetime
```

---

## Why fully normalized (vs. JSON blob)

Chose full normalization up front. The tradeoff: more Liquibase changesets and mapper
code now, but clean SQL querying/filtering later (by condition, location, phase,
sponsor) without JSON functions, and a schema that's self-documenting for anyone
(including future-you) reading it without the API docs open. The
`staging_raw_trial.raw_payload` column is the safety net — nothing from the source is
ever discarded even if a field isn't modeled yet.

## Source field mapping reference (ClinicalTrials.gov v2)

For when normalizing staging rows into the core tables above:

| Core table / column | Source JSON path |
|---|---|
| `trial.nct_id` | `protocolSection.identificationModule.nctId` |
| `trial.brief_title` | `protocolSection.identificationModule.briefTitle` |
| `trial.official_title` | `protocolSection.identificationModule.officialTitle` |
| `trial.overall_status` | `protocolSection.statusModule.overallStatus` |
| `trial.start_date` | `protocolSection.statusModule.startDateStruct.date` |
| `trial.primary_completion_date` | `protocolSection.statusModule.primaryCompletionDateStruct.date` |
| `trial.completion_date` | `protocolSection.statusModule.completionDateStruct.date` |
| `trial.last_update_posted_date` | `protocolSection.statusModule.lastUpdatePostDateStruct.date` |
| `trial.brief_summary` | `protocolSection.descriptionModule.briefSummary` |
| `trial.detailed_description` | `protocolSection.descriptionModule.detailedDescription` |
| `trial.study_type` | `protocolSection.designModule.studyType` |
| `trial_phase` (rows) | `protocolSection.designModule.phases[]` |
| `trial.enrollment_count` | `protocolSection.designModule.enrollmentInfo.count` |
| `trial.enrollment_type` | `protocolSection.designModule.enrollmentInfo.type` |
| `arm_group` (rows) | `protocolSection.armsInterventionsModule.armGroups[]` |
| `intervention` (rows) | `protocolSection.armsInterventionsModule.interventions[]` |
| `outcome` (PRIMARY rows) | `protocolSection.outcomesModule.primaryOutcomes[]` |
| `outcome` (SECONDARY rows) | `protocolSection.outcomesModule.secondaryOutcomes[]` |
| `trial.eligibility_criteria` | `protocolSection.eligibilityModule.eligibilityCriteria` |
| `trial.healthy_volunteers` | `protocolSection.eligibilityModule.healthyVolunteers` |
| `trial.sex` | `protocolSection.eligibilityModule.sex` |
| `trial.minimum_age` / `maximum_age` | `protocolSection.eligibilityModule.minimumAge` / `maximumAge` |
| `trial_std_age` (rows) | `protocolSection.eligibilityModule.stdAges[]` |
| `sponsor` / `trial_sponsor` | `protocolSection.sponsorCollaboratorsModule.leadSponsor`, `.collaborators[]` |
| `location` (rows) | `protocolSection.contactsLocationsModule.locations[]` |
| `overall_official` (rows) | `protocolSection.contactsLocationsModule.overallOfficials[]` |
| `condition` / `trial_condition` | `protocolSection.conditionsModule.conditions[]` |
| `keyword` / `trial_keyword` | `protocolSection.conditionsModule.keywords[]` |

## Open questions to resolve while iterating on this schema

- `eligibility_rule` doesn't fit the standard `BaseDb` convention (no `extid`/soft
  delete per tree node makes sense the same way it does for top-level entities) — plan
  is to give it its own minimal base (`id`, `created_at`, `updated_at`) and skip
  `extid`/`active`/`deleted_at`; confirm this before writing the Liquibase changeset.
- Validation invariants for `eligibility_rule` worth enforcing in code (not easily
  expressible as plain SQL constraints): a GROUP node must have `operator` set and
  `criterion_type`/`criterion_id`/`requirement_type` null; a LEAF node is the reverse;
  a NOT-operator GROUP should have exactly one child; every trial should have exactly
  one root row (`parent_rule_id is null`).
- Whether `criterion_type`/`criterion_id` should stay a loose polymorphic pair (as
  designed) or become two nullable FK columns (`condition_id`, `medication_id`) with a
  check constraint that exactly one is set — the latter gets real FK enforcement from
  MySQL at the cost of a slightly wider table.
- `location.status` — confirm whether per-site recruiting status is reliably present
  in the API response or only sometimes populated.
- Indexing strategy: at minimum `trial.nct_id`, `trial.overall_status`,
  `condition.name`, `medication.name`, `location.city`/`state`/`country` should be
  indexed for search performance.
- Should `trial_status.status` support multiple simultaneous values (e.g. tags) rather
  than a single enum column, if tracking gets more nuanced later?
