# UCHealth (University of Colorado Health) — Patient Portal & Epic API Access

*Notes from prior research conversation*

> **Reviewed 2026-08-14 — still accurate.** This is external research about Epic and UCHealth,
> not about this codebase, so it does not drift with the code.
>
> **It has since been acted on.** The app is registered with Epic, the PKCE flow described here
> is built (`UcHealthOAuthClient`, `UcHealthAuthController`), and it works against Epic's
> sandbox. What the research did *not* predict is where the real friction turned out to be:
> Epic granted `patient/*.read fhirUser launch/patient openid` but **silently dropped
> `offline_access`**, so tokens die in ~1 hour with no refresh path.
>
> See `UCHEALTH_INGESTION_PLAN.md` for the build, and `../CURRENT_STATE.md` for the grants
> still outstanding on Epic's side.

## Short Answer

**Yes to the portal, qualified yes to the APIs.**

## The Patient Portal

- UCHealth (University of Colorado Health, Aurora-based, `uchealth.org`) runs **My Health Connection** at `mychart.uchealth.org`, plus the UCHealth mobile app.
- It is **Epic's MyChart under the hood** — UCHealth describes My Health Connection as an application supported by Epic's MyChart patient portal.

### Disambiguation Warning

- `uchealth.org` = **UCHealth Colorado** (University of Colorado Health)
- `uchealth.com` = **UC Health Cincinnati** (University of Cincinnati) — a completely separate system with its own "My UC Health" portal. Don't cross the wires when searching.

## The API Situation

There is **no UCHealth-branded developer portal or public API program**. However, there *is* a real, standards-based FHIR API — it lives in **Epic's ecosystem**, not UCHealth's.

### Patient-Facing Access (the accessible path)

- Because UCHealth runs ONC-certified Epic, they are **required under the 21st Century Cures Act** to expose a **FHIR R4 endpoint** supporting USCDI data.
- UCHealth confirms third-party apps and websites can connect to your health record:
  1. In the third-party app, search for **"University of Colorado Health"** (or "UCHealth").
  2. Authenticate with your **My Health Connection username and password**.
- Under the hood this is **SMART on FHIR + OAuth 2.0**.
- **Apple Health Records** connects the same way — search "UCHealth" in the Health app.

## How to Connect as a Developer

1. **Register at Epic's developer program**: [https://fhir.epic.com](https://fhir.epic.com) (Epic on FHIR). Create a free developer account and register your app (patient-facing apps use the OAuth 2.0 authorization-code flow with PKCE).
2. **Find UCHealth's FHIR base URL**: Epic publishes an open endpoint directory of all Epic customer FHIR R4 endpoints (available through fhir.epic.com / Epic's open endpoints list). Look up University of Colorado Health / UCHealth in that directory.
3. **OAuth flow**: Your app redirects the patient to UCHealth's authorization server; the patient logs in with My Health Connection credentials and consents; your app receives an access token scoped to that patient's record.
4. **Query FHIR R4 resources**: Patient, Observation (labs/vitals), Condition, MedicationRequest, AllergyIntolerance, Immunization, DocumentReference, and other USCDI-aligned resources.
5. **Sandbox first**: Epic provides sandbox endpoints and test patients through fhir.epic.com so you can build and test before touching production.

### Key Points

| Item | Detail |
|---|---|
| Portal | My Health Connection (`mychart.uchealth.org`) |
| EHR | Epic (MyChart) |
| API standard | FHIR R4, SMART on FHIR, OAuth 2.0 |
| Developer registration | fhir.epic.com (Epic on FHIR, free) |
| Access type | Patient-authorized (patient logs in and consents) |
| Regulatory basis | 21st Century Cures Act / ONC certification |
| UCHealth-specific dev portal | None — everything goes through Epic |

## Bottom Line

You can't get bulk or system-level "open API" access from UCHealth directly, but any developer can build a **patient-facing SMART on FHIR app** through Epic's program, and any UCHealth patient can authorize that app against their own record using My Health Connection credentials.
