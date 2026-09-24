# Kumbuka

Offline-first Android app that tells you what to revise, out of everything across
your units, and for how long — learning how fast you personally forget each topic
instead of using a fixed formula.

## App layout

Four tabs: **Home** (greeting, recommended next revision, today's queue, coming-up dates),
**Units** (search and your course library, with unit detail pages), **Assessments** (dated
CATs/assignments/exams and saved results, filterable by unit) and **Progress** (weekly activity,
unit coverage, confidence mix). Settings opens from the gear icon. Design tokens and component
rules are in `docs/UI-TOKENS.md`.

## Implementation status (read before assuming a claim below is live)

- **No trained model ships in this app.** The "learned" scheduler arm currently runs
  `PlaceholderRecallPredictor` (`domain/scheduler/TodayScheduler.kt`), a hand-set, versioned
  exponential-decay stand-in — not a trained model. Every prediction, log row, and Progress/
  research-controls surface labels it as placeholder. The real model is trained in the separate
  `SemProject_MLEngine` repository; wiring that trained artifact into this app is future work.
- **The pack-authoring wizard is retained by explicit product decision**, not because it's
  unfinished — it was reviewed and kept as-is (styling cleanup only) rather than removed or
  redesigned.
- **Native splash limitations**: startup uses one platform-owned `core-splashscreen` sequence
  (no app-authored Compose intro). Android itself controls exact cold/warm-start behavior; a
  guaranteed absence of all OS icon frames or OS warm-start animation is not promised, and older
  API levels show a compatible static mark instead of the animated one. See
  `docs/UI-TOKENS.md` for the native-startup-ownership contract.

See `HANDOFF.md` for the current architecture, migration/archive rules, and verified-vs-blocked
checklist.
