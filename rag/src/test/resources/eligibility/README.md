# Real eligibility-criteria fixtures

Captured from **the project's own pipeline**, not from the CT.gov API directly: ingested
via `POST /api/ingestion/clinicaltrials` (condition "breast cancer", 50 studies) and read
back through `GET /api/trial`. So this is post-`ClinicalTrialsGovParser` text — exactly
what the chunker reads out of `trial.eligibility_criteria`.

Captured 2026-08-07. Each file exercises a shape found in that 50-trial sample; the
percentages below are from that sample, so treat them as indicative, not authoritative.

| Fixture | Shape it pins down |
| --- | --- |
| `clean-baseline-NCT06649565.txt` | Well-formed: both headers, flat `*` bullets. The happy path. |
| `escaped-markdown-NCT01303679.txt` | Literal `\*` / `\<` escaping survives ingestion — **46% of the sample**. Must be unescaped before header matching or bullet stripping. |
| `nested-numbered-NCT04942054.txt` | Indented sub-bullets and numbered sub-items — **24% of the sample**. Drives the parent-prefixing rule. |
| `longest-nested-NCT04244552.txt` | 13,771 chars, the sample's longest. Also has `Inclusion Criteria` **with no colon**. |
| `prose-NCT05076266.txt` | No bullet markers at all — **10% of the sample**. |
| `legacy-caps-NCT00003680.txt` | Older CT.gov convention: no inclusion/exclusion headers, uses `DISEASE CHARACTERISTICS:` / `PATIENT CHARACTERISTICS:` caps sections in one prose block. The §5 fallback case. |

## Why these, and not the hand-built sample

`datafetcher/src/test/resources/sample-clinicaltrials-study.json` uses `- ` hyphen bullets
and tidy `Inclusion Criteria:` headers. **Neither occurs in real data** — the 50-trial
sample had 632 asterisk-bullet lines, 163 numbered, and zero hyphens. A parser written
against that fixture matches nothing in production. These files exist so that can't happen
again.

## Measured shape frequencies (n=50)

- Escaped markdown present: **23/50**
- Bullet markers: `*` 632 lines, numbered 163 lines, `-` **0 lines**
- Headers: both 48/50, neither **2/50**
- Nested/indented sub-bullets: **12/50**
- Prose, no bullets: **5/50**
- Criteria length: min 95, median 1,046, max 13,771 chars

That max matters: the local embedding model's window is ~256 word pieces (roughly 1,000
chars), so the largest block is ~14x the window. Per-criterion chunking is what keeps that
text searchable instead of silently truncated.

## Refreshing these

Re-run the ingestion endpoint and re-select. Keep the awkward shapes — the point is
coverage of failure modes, not a representative average.
