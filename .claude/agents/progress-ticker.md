# Progress Ticker — character-per-record visual progress

**Status: built, running, and promoted to a skill (2026-08-09).** Wired into both
ClinicalTrials.gov ingestion loops and proven on a real 250-trial run.

**To add one to another loop, use the `progress-ticker` skill** — it covers creating the
shared class where it does not exist, the configuration, choosing the loop, wiring, and the
traps. This document is the design record: why the pattern looks the way it does, and what
changed once it met real runs. Read it when deciding *whether* to use the pattern; read the
skill when *applying* it.

A visual cue for long-running imports: one character per record, wrapping to a new line
every N records, with the starting record number in a left gutter. Different glyphs for
different outcomes, so the *shape* of the run is visible at a glance.

```
     1 | ****.**!***.*********!**********.***************  2.6s 28.4/s ETA 3.9s
   101 | ***********************!!!!!!!!!****************  5.3s 28.4/s ETA 1.3s
   201 | ***.***.***.***.***                               6.6s 28.4/s
```

A clump of `!` at record 148 tells you something a final summary line never will. This is
distinct from — and composes with — the log-line summary pattern, which reports *what
happened* after the fact. This one reports *where you are*, live.

**What it is actually for, confirmed in use:** knowing the run is not hung. Normalization
is ~349ms per trial and 99.3% of a run's wall-clock, so a full pull is minutes of total
silence otherwise. The user's words on first seeing it work: *"I now can see the progress
and I know it is not hung."*

## Glyphs

| Glyph | Meaning   | Method     |
|-------|-----------|------------|
| `*`   | inserted  | `tick()`   |
| `.`   | skipped   | `skip()`   |
| `!`   | error     | `error()`  |

## Where it lives in this project

The class is in the `common` module, under a `progress` package alongside `util`. It is a
plain object with no framework dependency — `common` is deliberately Spring-free, and the
ticker keeps it that way, so any module can construct one.

Its settings bind from `application.yml` under the ingestion config, via a properties class
that lives in `datafetcher/config` beside the ClinicalTrials ingest properties. The
properties class cannot sit next to the ticker precisely because binding requires Spring.
That module is a library with no component scan, so the properties class needs explicit
registration in the module's configuration class — the same pattern the rag module follows.

Wired into two loops:

- **The staging loop** in the ingest job — `*` on insert, `*` on refresh-and-requeue, `.` on
  skip-as-duplicate, `!` on error. Fast (250 trials in ~9.5s), so the bar mostly flickers by.
- **The normalization loop** in the normalization service — the slow one, and the reason the
  pattern exists here. No skip path exists in that loop, so `.` never appears; that is
  accurate rather than a gap.

## Configuration

Three settings, each with an environment-variable override:

- **enabled** — `true` (default), `auto`, or `false`. See the hard-won note below on why the
  default is not `auto`.
- **line-width** — records per line before wrapping. Default 100; match it to your terminal.
- **flush-interval** — flush stdout every N records. Default 10.

Both numeric values are guarded in the constructor, because exposing them to YAML made bad
values reachable from config: a flush interval below 1 clamps to 1 (a zero would throw
ArithmeticException on the modulo, mid-import), and a non-positive line width falls back to
the default (it would break the gutter modulo the same way).

## The three things that go wrong

**1. Buffering.** Standard output is line-buffered. Printing a glyph with no newline can sit
in the buffer, so 100 glyphs appear at once when the line wraps — which defeats the entire
purpose. Hence the flush interval. Flushing every single record is a real syscall cost on a
large import; every 10 is the compromise. Tune it, but do not remove it.

**2. The final partial line.** A run ending at 1,247 leaves 47 glyphs with no newline, and
whatever prints next lands on the same line. **Resolved:** the class implements
`AutoCloseable` and the finish method stays public, so try-with-resources guarantees the
newline even when the loop exits by exception, without breaking a manual call. Both call
sites use try-with-resources.

**3. Standard output vs. the logger.** SLF4J writes through its own appender, so a log line
fired mid-import shreds the bar. **Resolved for the ingestion loops:** both catch blocks were
demoted from error to debug. Nothing is lost — every failure still lands in the errors list
returned to the caller, in the summary log line, and (in normalization) persisted on the
staging row.

This only covers the loops themselves. Other components logging during a run — Hibernate,
Spring's PageImpl serialization warning — still land between bar lines. In practice this
reads fine and was accepted rather than fixed. The only real fix is routing the bar through
SLF4J, which trades away the live-flush behavior that makes it a live bar at all.

## Design decisions (and why)

- **Leading gutter, not a trailing count.** A fixed-width number at line start gives an
  aligned left column you can scan straight down. The number is the line's *starting* record,
  so you locate a glyph by counting across from the gutter.
- **Outcome-varying glyphs, not uniform `*`.** Costs nothing, and turns the bar from a
  liveness signal into a diagnostic.
- **A label argument.** Both bars print in a single ingestion run, so they need
  distinguishing. Printed once at construction, only when enabled.
- **Counters on the ticker duplicate the import result.** Intentional. The ticker's are for
  the bar; the result object remains the real record. The ticker's skip count fills a genuine
  gap — in the summary pattern, filtered rows never reach the processed total, so they vanish
  from the summary entirely.

## The default that had to change

`enabled` originally defaulted to `auto`, meaning "draw only when a console is attached." The
reasoning was sound — a star bar is useless-to-harmful in a captured log or CI output — and it
was wrong in practice.

**Ingestion is triggered from the frontend against an already-running backend.** That work
happens on an HTTP request thread, which has no attached console, so `auto` resolved to false
and the bar never drew on the first real run. The default is now `true`; `auto` remains
available for a launch that genuinely has a console, and `false` disables it outright. When
disabled the ticker still counts, so the getters stay valid.

The lesson generalizes past this class: console detection describes how the *process* was
launched, not whether anyone is watching. For a server, those are different questions.

## Bug found while wiring it up

The staging loop's missing-identifier branch recorded an error and then continued without
incrementing anything, so those studies fell out of *both* the staged and skipped counts and
vanished from the summary entirely. Adding the ticker made the gap obvious, since the record
produced no glyph. It now ticks as an error.

This is the same class of gap noted in the counters decision above — a record that reaches
neither the success nor the skip path silently disappears from the accounting.

## Elapsed, rate, and ETA

Appended at line end rather than in the gutter — the gutter's fixed-width record number is
what makes the left column scannable, and a variable-width duration there would ruin it. The
suffix prints when a line wraps and again on the final line, which is padded to the full line
width so the timing column stays aligned.

The ETA needs an expected record count, passed in at construction; both ingestion loops know
theirs upfront. Without one the bar still shows elapsed and rate but projects nothing, which
is the right behavior for a loop of unknown length. The ETA also disappears once the count
reaches the total, so the last line reports only what the run actually took.

**The projection is a flat average over the whole run, not a windowed rate.** That is honest
for these loops, where per-record cost is roughly steady — the ETA visibly converges as the
run proceeds. It would mislead on a loop whose per-record work varies wildly, and anyone
reusing this elsewhere should know that before trusting the number.

Two formatting details that came from watching it run rather than from design:

- **Rate must be computed in nanoseconds, not milliseconds.** A fast loop finishes several
  records inside a single millisecond, so dividing by elapsed-millis hits zero and silently
  drops both rate and ETA — exactly when the bar scrolls fastest, which is the staging loop.
- **Durations under ten seconds need a decimal.** Flooring to whole seconds renders every
  short run and every near-done ETA as `0s`, which reads as broken rather than as quick.

## Still open

- [ ] Non-TTY detection — the console check exists but is no longer the default; revisit only
      if a captured-log run starts collecting asterisks.
- [ ] Gutter overflows six columns past 999,999 records — widen, or accept the ragged wrap.
- [ ] A windowed rate instead of a flat average, if this is ever used on a loop with uneven
      per-record cost.
- [ ] The skill has been written but not yet run against a second loop. Its first real use
      elsewhere is what will show whether the pattern generalizes or was shaped by these two
      loops — revise it after that, not before.

## Provenance

Written from a snippet the user supplied plus four design choices made in conversation, then
implemented and corrected against real runs. A search of this repo at drafting time found no
existing implementation. The user believes they wrote something like it long ago in another
project — if that original surfaces, fold in whatever it learned (non-TTY handling, rate
display, large-count behavior).
