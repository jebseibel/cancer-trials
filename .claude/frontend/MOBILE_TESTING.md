# Testing the Frontend as a Phone

How to see the app at phone size without a phone, and what that does and does not prove.
Companion to `MOBILE_PLAN.md` (what was changed and why).

---

## Start the dev server so a phone can reach it

In `frontend/`:

```
npm run dev -- --host
```

The `-- --host` is the only difference from the usual `npm run dev`. Without it Vite listens on
localhost only and no other device can connect. With it, the startup output gains a second line:

```
Local:   http://localhost:5173/
Network: http://192.168.1.42:5173/
```

**The Network line is the URL to type into a phone's browser**, on the same wifi. If that line
is missing, `--host` did not take effect.

The backend must be running as well — the Vite proxy forwards `/api` to `localhost:8080`, which
resolves on the dev machine regardless of which device is looking at the page.

---

## Emulate a phone on the computer

**Do not test by dragging the browser window narrow.** It approximately works and is misleading
in one specific way: a resized desktop window carries browser chrome, enforces a minimum width,
and keeps the desktop's device pixel ratio, so the effective viewport can land at a width no
real phone has. Breakpoints then flip in the wrong place.

Use device emulation instead, which sets the real viewport width the CSS sees:

1. **F12** — open DevTools
2. **Ctrl+Shift+M** — toggle the device toolbar
3. Choose a device from the dropdown, or type an exact size

Two sizes worth checking:

| Size | Why |
| --- | --- |
| **360 × 640** | Small Android — the tightest realistic case, and where things break first |
| **390 × 844** | iPhone 14 — the common case |

Tailwind's `sm:` breakpoint is **640px**, which is the line every change in `MOBILE_PLAN.md`
turns on. Both sizes above are below it, so both show the mobile layout.

---

## ⚠️ What emulation cannot test

**Device emulation still gives you a mouse pointer.** A mouse hits a 16px target accurately
every time, so any control that is too small for a thumb will look perfectly fine.

That is precisely what item 5 of the mobile plan is about — the `why?` toggles, `and N more`,
and `What matched` were all around 16-18px against a 24px accessibility minimum. **Emulation
will not show a problem there even if one exists.**

Those controls are the ones the whole no-verdicts design depends on: a reader is meant to be
able to check the reasoning behind every flag. If the control that reveals it is hard to hit,
the reasoning is effectively hidden.

**So: layout on the computer, touch on a real phone.** That is the whole reason to bother with
`--host`.

---

## What to check, in order

| Where | What to look for |
| --- | --- |
| DevTools 360px | Hamburger appears; panel opens; all six links present |
| DevTools 360px | Tapping a link navigates **and closes the panel** |
| DevTools 360px | Nav title fits beside the hamburger without squashing |
| DevTools 360px | Patient Record — all three tabs reachable, icons gone |
| DevTools 360px | Trials for You — cards have edge padding; counts wrap below the title |
| DevTools 360px | Process Trials — buttons stack full-width, not ragged |
| **Real phone** | Can a thumb actually hit `why?` and `and N more` |
| Desktop, full width | **Nothing changed.** Should be pixel-identical to before |

That last row is a real check, not a formality. Every change was either scoped below `sm` or a
padding move that nets out, so a visible difference at desktop width means something is wrong.

---

## Testing against production

**Prod is for confirming, not discovering.** Three reasons it is the wrong place to iterate:

- **Its corpus was empty as of 2026-08-11**, so "Trials for You" renders its empty state and the
  card layout — the most substantial part of the mobile work — is not visible there at all.
  ⚠️ *Unverified since; if the corpus has been pulled, this reason no longer applies.*
- **Each iteration is a `buildDeployment`, an scp, and a service restart.** Minutes per attempt
  to learn whether a CSS class was right.
- **It holds a real medical record.** Wrong risk profile for checking whether a button wraps.

What prod does genuinely test, once the layout is already right locally: HTTPS, the real domain,
the Nginx proxy, and the actual phone and browser the patient uses.

⚠️ **A deploy needs `./gradlew buildDeployment`, never a plain `build`** — the frontend is
bundled inside the jar, and a plain build ships whatever stale copy is sitting in
`src/main/resources/static/`. See `../hosting/DEPLOY_RUNBOOK.md`.
