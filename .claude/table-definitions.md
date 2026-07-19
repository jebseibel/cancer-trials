# Table Definitions

## Purpose
Track job postings found during the search — what they require, where they are, where they came from — as a foundation for the job-search tracking feature. Designed for single-user personal use, not multi-tenant.

## Scope
Covers **Job Posting**, **Company**, **Skill** (many-to-many with Job Posting, User, and Friend), **Application**, **Contact**, and **Friend** (many-to-many with Job Posting, Company, and Skill).

## Conventions
All entity tables extend the project's standard base fields (see `BaseDomain`/`BaseDb`):
- `id` — internal numeric primary key
- `extid` — UUID-style external identifier, used in API paths instead of `id`
- `created_at`, `updated_at`, `deleted_at` — audit timestamps
- `active` — soft-delete flag (`ACTIVE` / `INACTIVE`), records are never hard-deleted

Only business-specific fields are listed per table below; base fields are implied.

## Tables

### company
The hiring company for a job posting.

| Field | Type | Notes |
|---|---|---|
| name | varchar, unique | Company name |
| website | varchar | Nullable |
| industry | varchar | Nullable |
| notes | text | Nullable — free-form personal notes |

### job_posting
The core entity — a single job listing found from any source.

| Field | Type | Notes |
|---|---|---|
| title | varchar | Job title as posted |
| company_id | FK → company | Every job posting resolves to a Company record |
| description | text | Full job description/body |
| city | varchar | Nullable — may be blank for fully remote postings |
| state | varchar | Nullable |
| country | varchar | Nullable |
| work_mode | enum | REMOTE / HYBRID / ONSITE |
| salary_min | integer | Nullable |
| salary_max | integer | Nullable |
| salary_currency | varchar | Nullable, e.g. "USD" |
| source | enum | LINKEDIN / INDEED / COMPANY_SITE / REFERRAL / MANUAL / OTHER |
| source_url | varchar, unique | Natural dedup key — re-imports from the same URL update rather than duplicate |
| posted_at | datetime | Nullable — date the job was originally posted, if known |
| status | enum | NEW / INTERESTED / NOT_INTERESTED / ARCHIVED — triage status; whether you've applied is tracked separately in `application` |
| notes | text | Nullable — free-form personal notes |

### skill
Flat lookup list of tags — technologies, competencies, or free-text search terms (e.g. things you'd plug into a job board's search box). One shared table used across job postings, your own profile, and friends; no category table — kept simple for personal use.

| Field | Type | Notes |
|---|---|---|
| name | varchar, unique | e.g. "Java", "Spring Boot", "Kubernetes", "backend engineer", "401k", "remote" |

### job_posting_skill
Plain many-to-many join between `job_posting` and `skill`. No per-skill metadata (no required/nice-to-have flag, no years-of-experience) — deliberately simple since this isn't something worth querying on for a single-user job search.

| Field | Type | Notes |
|---|---|---|
| job_posting_id | FK → job_posting | |
| skill_id | FK → skill | |

Composite unique constraint on (`job_posting_id`, `skill_id`) to prevent duplicate links.

### user_skill
Many-to-many join between `user` (the job seeker, i.e. you — see the existing `UserDb`/`user` table) and `skill`. Lets the app compare your own skills against a job posting's required skills.

| Field | Type | Notes |
|---|---|---|
| user_id | FK → user | |
| skill_id | FK → skill | |

Composite unique constraint on (`user_id`, `skill_id`).

### friend_skill
Many-to-many join between `friend` and `skill` — e.g. tracking what a friend is known for professionally, useful when deciding who to ask about a given job or technology.

| Field | Type | Notes |
|---|---|---|
| friend_id | FK → friend | |
| skill_id | FK → skill | |

Composite unique constraint on (`friend_id`, `skill_id`).

### application
Tracks that you applied to a job posting, separate from the posting's own triage `status`. Allows history/lifecycle tracking and, in principle, multiple applications to the same posting over time (e.g. reapplying months later).

| Field | Type | Notes |
|---|---|---|
| job_posting_id | FK → job_posting | |
| date_applied | date | |
| resume_version | varchar | Nullable — which resume/version was submitted |
| application_status | enum | APPLIED / INTERVIEWING / OFFER / REJECTED / WITHDRAWN |
| notes | text | Nullable — free-form personal notes |

### contact
A recruiter, hiring manager, or referral. May be tied to a company generally, a specific job posting, or both.

| Field | Type | Notes |
|---|---|---|
| company_id | FK → company | Nullable |
| job_posting_id | FK → job_posting | Nullable |
| name | varchar | |
| role | varchar | Nullable — e.g. "Technical Recruiter", "Hiring Manager" |
| email | varchar | Nullable |
| phone | varchar | Nullable |
| notes | text | Nullable — free-form personal notes |

### friend
A personal network contact — someone you know (former colleague, college friend, LinkedIn connection, etc.), independent of any specific company or job posting. Distinct from `contact`, which is scoped to a recruiter/hiring-manager tied to a company or posting. A friend may optionally be linked to one or more companies and/or job postings via the join tables below.

| Field | Type | Notes |
|---|---|---|
| name | varchar | |
| relationship | varchar | Nullable — how you know them, e.g. "Former coworker at Acme", "College friend" |
| email | varchar | Nullable |
| phone | varchar | Nullable |
| linkedin_url | varchar | Nullable |
| last_contacted_at | date | Nullable — helps prompt follow-ups |
| notes | text | Nullable — free-form personal notes |

### friend_company
Many-to-many join between `friend` and `company` — e.g. a friend currently works at, or has worked at, a company you're tracking.

| Field | Type | Notes |
|---|---|---|
| friend_id | FK → friend | |
| company_id | FK → company | |

Composite unique constraint on (`friend_id`, `company_id`).

### friend_job_posting
Many-to-many join between `friend` and `job_posting` — e.g. a friend referred you to a posting, or you plan to ask them about it.

| Field | Type | Notes |
|---|---|---|
| friend_id | FK → friend | |
| job_posting_id | FK → job_posting | |

Composite unique constraint on (`friend_id`, `job_posting_id`).

## Deferred (not built yet)
- **Skill categories** — if the flat skill list gets large enough to want grouping (Language/Framework/Cloud/Soft Skill), add a `skill_category` table later.
