# Kumbuka

Offline-first Android app that tells you what to revise, out of everything across
your units, and for how long learning how fast you personally forget each topic
instead of using a fixed formula.

Kotlin, Jetpack Compose (Material 3), Room (schema v3), DataStore, Navigation-Compose.
No accounts, no login, no backend. minSdk 26.

## Reviewing this work

The current work is on the branch **`redesign/usiku-na-jua`** (the "Usiku na Jua" redesign);
`main` still shows the earlier state. Start with **[`HANDOFF.md`](HANDOFF.md)**: its first
section lists what changed, what was verified on real devices, and the known gaps.

Source lives under `app/src/main/kotlin/dev/kumbuka/app/`:

| Folder | What is there |
| --- | --- |
| `ui/` | Compose screens, theme (`ui/theme`), navigation shell |
| `domain/` | Scheduler (`domain/scheduler`), analytics and CSV export (`domain/analytics`) |
| `data/` | Room database, migrations, preferences, debug demo-history seeder |
| `pack/` | Course pack import and the outline parser |

Unit tests are in `app/src/test`.

## Build and test

```
./gradlew assembleDebug testDebugUnitTest      # macOS / Linux
.\gradlew.bat assembleDebug testDebugUnitTest  # Windows
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Release signing is not set
up, so only debug builds exist.

## App layout

Four tabs: **Today** (progress hero, recommended next revision with "Why this?", queue,
coming-up dates), **Units** (search and your course library, with unit detail pages),
**Assessments** (dated CATs/assignments/exams and saved results) and **Progress** (weekly
activity, unit coverage, confidence mix). Settings opens from the gear icon and offers
System / Light / Dark themes and English / Kiswahili. Tablets in landscape get a navigation
rail and a two-pane layout.

## Implementation status (read before assuming a claim below is live)

- **No trained model ships in this app.** The "learned" scheduler arm currently runs
  `PlaceholderRecallPredictor` (`domain/scheduler/TodayScheduler.kt`), a hand-set, versioned
  exponential-decay stand-in — not a trained model. Every prediction, log row, and Progress/
  research-controls surface labels it as placeholder. The real model is trained in the separate
  `SemProject_MLEngine` repository; wiring that trained artifact into this app is future work.
- **Research data is opt-in.** Study participation is unticked by default, and personal goals
  never count toward research data or the CSV export.
- **Native splash limitations**: startup uses one platform-owned `core-splashscreen` sequence
  (no app-authored Compose intro). Android itself controls exact cold/warm-start behavior; older
  API levels show a compatible static mark instead of the animated one.
- Lint (`lintDebug`) has pre-existing errors unrelated to the redesign.

See `HANDOFF.md` for the current architecture, migration/archive rules, and the
verified-vs-not-verified checklist.
