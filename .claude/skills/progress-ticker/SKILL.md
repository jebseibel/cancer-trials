---
name: progress-ticker
description: Add a character-per-record console progress bar to a long-running loop in a given class - one glyph per record (* inserted, . skipped, ! error), wrapping with a record-number gutter and trailing elapsed/rate/ETA. Creates the shared ProgressTicker class and its config on first use, then wires the named class's loop to it. Use whenever asked to add a progress bar, progress ticker, or live progress output to an import, ingestion, batch, or normalization loop, or when a long-running job "looks hung" while it runs.
---

# Progress Ticker

## Project Goal

Make a long-running loop visibly alive. One character per record, wrapping every N records
with the line's starting record number in a left gutter, and elapsed / rate / ETA at line end:

```
NORMALIZING
     1 | ****.**!***.*********!**********.***************  2.6s 28.4/s ETA 3.9s
   101 | ***********************!!!!!!!!!****************  5.3s 28.4/s ETA 1.3s
   201 | ***.***.***.***.***                               6.6s 28.4/s
```

Two things this gives you that a summary log line cannot. **The run is not hung** — the
original motivation, on a loop costing ~349ms per record where a full pull is minutes of
silence. And **the shape of the run** — a clump of `!` at record 148 localizes a failure to a
region of the input, which `errors=8` never will.

This composes with the summary pattern rather than replacing it: bar during, summary after.

## Input

- **Target class** — e.g. `TrialNormalizationService`. A file path or a class name.
- **Which loop**, if the class has more than one. See "Choosing the loop" below.
- **Optional: the label** shown above the bar. Defaults to a verb matching the loop's work
  (`STAGING`, `NORMALIZING`, `INDEXING`).

If asked to "add a progress ticker to X" with no further detail, that is sufficient — find
the loop, confirm the choice if ambiguous, and proceed.

## First use vs. later use

Check whether the shared class already exists at
`common/src/main/java/com/seibel/cancer/common/progress/ProgressTicker.java`.

**If it does**, this is a wiring job only. Skip to "Wiring a loop". Do not modify the shared
class to suit one call site — if a call site needs something it lacks, say so and ask.

**If it does not**, create it and its configuration first, per the two sections below. That is
a one-time cost; every later invocation is just wiring.

## Creating the shared class (first use only)

Package `com.seibel.cancer.common.progress`, in the `common` module.

**It must have no framework dependency.** `common` is deliberately Spring-free — a plain
object is what lets every module construct one. Configuration binding lives elsewhere (next
section); do not put `@ConfigurationProperties` on this class or import Spring into `common`.

State it holds: line width, flush interval, an enabled flag, an expected total (0 = unknown),
a start timestamp captured at construction in nanoseconds, and four counters — total count
plus successes, skips, and errors.

Behavior:

- **Three recording methods** — one each for inserted, skipped, and failed, incrementing the
  matching counter and emitting `*`, `.`, or `!` respectively.
- **Gutter** — when the count is at a line boundary, print the line's *first* record number in
  a fixed six-column field followed by a separator, so the left column can be scanned straight
  down and a glyph located by counting across.
- **Wrap** — at a line boundary, terminate the line with the timing suffix and flush.
- **Periodic flush** — otherwise flush every N records. Standard output is line-buffered;
  without this, a whole line of glyphs appears at once when it wraps, defeating the purpose.
- **Timing suffix** — elapsed, rate, and (only when a total was supplied and records remain)
  an ETA.
- **A finish method** that terminates a partial final line, padding it to the full line width
  so the timing column stays aligned with the lines above. On a run landing exactly on the
  line width, report the total instead — the last line already wrapped, and emitting another
  newline leaves a stray blank line.
- **`AutoCloseable`**, delegating to finish, with finish left public. Try-with-resources then
  guarantees the final newline even when the loop throws, without breaking a manual call.
- **When disabled, still count.** Print nothing, but keep the counters accurate so the getters
  remain valid.
- **A label**, printed once at construction when enabled, so two bars in one run are
  distinguishable.

Guard both numeric settings in the constructor: a flush interval below 1 clamps to 1, and a
non-positive line width falls back to the default. Both feed a modulo — a zero throws
ArithmeticException mid-import, which is a nasty way to lose a long run.

### The two timing details that are easy to get wrong

**Compute the rate in nanoseconds, not milliseconds.** A fast loop finishes several records
inside one millisecond; dividing by elapsed-millis hits zero and silently drops both rate and
ETA — precisely when the bar scrolls fastest.

**Show a decimal below ten seconds.** Flooring to whole seconds renders every short run and
every near-done ETA as `0s`, which reads as broken rather than as quick. Above ten seconds,
whole seconds; above a minute, `2m01s`; above an hour, `1h04m`.

The ETA is a flat average over the whole run, not a windowed rate. Say so in a comment on the
method. It is honest where per-record cost is steady and misleading where it is not.

## Creating the configuration (first use only)

A `@ConfigurationProperties` class in the module that *consumes* the ticker — not in `common`,
which cannot host it. For the datafetcher module that means `datafetcher/config/`, beside
`ClinicalTrialsIngestProperties`.

Three settings, each with an environment-variable override in `application.yml`, under a
`progress` key nested inside that module's existing config block:

| Setting | Default | Why |
| --- | --- | --- |
| `enabled` | `true` | Three-state: `true`, `auto`, `false`. See below. |
| `line-width` | 100 | Records per line. Match to terminal width. |
| `flush-interval` | 10 | Flush every N records. |

`enabled` is a string, not a boolean, because `auto` is a third state meaning "only when a
console is attached." Give the properties class a method that resolves it against a
console-attached flag supplied by the caller, treating null/blank as `auto` and tolerating
casing and surrounding whitespace.

**Default it to `true`, not `auto`.** This is the mistake worth not repeating: ingestion is
triggered from the frontend against an already-running backend, so the work happens on an HTTP
request thread with no attached console — `auto` resolves to false and the bar never draws.
Console detection describes how the *process* was launched, not whether anyone is watching.
For a server those are different questions.

A library module has no component scan, so register the properties class explicitly in the
module's `@Configuration` class alongside any existing ones.

## Choosing the loop

Read the target class and list every loop that iterates a collection of records. Then:

- **One obvious candidate** — use it.
- **Several** — prefer the slowest, which is where the bar earns its keep. If that is not
  determinable from the code, ask rather than guessing; naming the candidates in the question
  is usually enough for the user to pick instantly.
- **None** — say so rather than inventing a place to put one.

A loop is a poor fit if it is fast and short (under a second — the bar flickers past and adds
noise), or if each iteration prints its own output (the bar and that output interleave into
mush).

## Wiring a loop

1. **Inject the properties class** into the target via its constructor. These classes use
   Lombok `@RequiredArgsConstructor`, so adding a `private final` field is the whole change.
2. **Wrap the loop in try-with-resources**, constructing the ticker with the label, the three
   configured values, the resolved enabled flag, and the expected record count.
3. **Pass the real total** — the size of the collection being iterated. This is what makes the
   ETA possible; without it the bar still shows elapsed and rate.
4. **Call the matching method on every path through the loop body.** Every branch that
   consumes a record must record exactly one glyph: inserted, skipped, or failed.
5. **Demote in-loop logging to debug.** A log line fired mid-iteration shreds the bar. This is
   safe when the failure is already captured elsewhere — an errors list returned to the
   caller, a summary line, a persisted error column. Verify that before demoting; if a failure
   would otherwise vanish, leave the log and accept the broken lines.

### Watch for a record that reaches no branch

The characteristic bug this surfaces. A `continue` that skips the counters means a record
falls out of *every* total and vanishes from the summary — it produces no glyph, which is what
makes it visible. Wiring the ticker found exactly this in the staging loop: a study missing its
identifier recorded an error, then continued without incrementing either counter.

If you find one, fix it and report it separately from the ticker work.

## Verify

1. **Compile** — `./gradlew :common:build :datafetcher:test`, adjusting modules to match.
2. **Render it once** at realistic scale before declaring it done. A throwaway test that ticks
   a couple hundred records with a small sleep, a deliberate clump of errors, and a total, is
   enough. Read it back from the test XML's `system-out` — Gradle's console truncates lines,
   which makes a correct bar look broken. **Delete the throwaway afterward.**
3. **Confirm by eye**: gutter numbers ascend by the line width, the error clump is visible as a
   clump, the ETA falls as the run converges, the final line is aligned and carries no ETA.
4. **Verify test counts from the XML**, not Gradle's exit code — Gradle reports success when
   zero tests run.

## Tests worth writing

Behavior, not rendering. The class is pure stdout, so capture `System.out` into a buffer in a
setup method and restore it after.

- Counters track each outcome separately, and still count when disabled
- Each outcome emits its own glyph
- Gutter shows each line's starting record number
- Final partial line is terminated, and an exact-width run does not double-newline
- Try-with-resources terminates the line when the loop throws
- Elapsed and rate appear on a wrapped line; ETA appears only with a known total and remaining
  records, and is gone once the count reaches the total
- The partial line's timing column aligns with the full lines above
- Flush interval of 0 and line width of 0 are clamped rather than thrown

For the properties class: the three-state resolution in every combination, casing and
whitespace tolerance, null/blank falling back to `auto`, and the default drawing with no
console attached.

**Do not assert whole rendered lines.** They carry a timing suffix whose text varies with how
fast the test happens to run. Assert on the stable prefix instead. Two related traps: strip
only trailing whitespace when splitting captured output, since a leading strip eats the first
line's gutter padding; and never assert equality between two `indexOf` results without also
asserting they were found — `-1 == -1` passes vacuously and catches nothing.

Put these in the consuming module if `common` has no test source tree. Adding a test framework
to `common` is a build change beyond the scope of this skill; say so rather than doing it.

## Report back

- Which loop was wired, and why that one if there was a choice
- Whether the shared class was created or already existed
- Every file changed
- Any in-loop logging demoted, and where those failures still surface
- Any record path that reached no counter (see above)
- Test counts, and confirmation the bar was rendered and read back once
- That the backend must be restarted, since the settings bind at startup

## Do not

- Put Spring in `common`. The ticker is a plain object; that is the point.
- Default `enabled` to `auto`. It resolves to false on a server request thread.
- Remove the periodic flush. Without it the bar appears a line at a time, which defeats it.
- Demote a log line whose failure is not captured anywhere else.
- Trust Gradle's console rendering of the bar. Read the test XML.
- Add a bar to a sub-second loop, or to a loop whose body already prints per iteration.
