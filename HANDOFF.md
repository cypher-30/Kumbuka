# Kumbuka — session handoff

## What this covers

This handoff replaces all prior phase-numbered notes below the "Superseded history" section.
It reflects the production-readiness pass done on branch `whynot` starting from baseline
commit `80b5ff2`, driven by a 14-todo plan covering: removing login, no fake sample data, a
four-tab shell (Today/Units/Marks/Settings), reworked Today and Session screens, an expanded
Marks tab (CATs + assignments + exams + past papers), dual-arm scheduler logging with a
placeholder model, an Insights screen replacing Progress, a simpler Settings screen, a simpler
import flow, styling polish, and this doc pass.

Three decisions were explicitly re-litigated and resolved mid-project (overriding older
locked-in notes below):
- **Login is removed entirely** — no accounts, no guest mode, no standalone consent screen.
- **The pack-authoring wizard is kept**, styling cleanup only — its validation/step-back/
  discard-confirmation behavior is unchanged.
- **The "learned" scheduler arm ships with a placeholder model now**, not a null slot. It is a
  hand-set, versioned decay stand-in, clearly labeled as not-trained everywhere it surfaces. The
  real trained model lives in the separate `SemProject_MLEngine` repository and is wired in
  later — see "Real-model adapter contract" below.

## Current architecture

- Kotlin, Jetpack Compose, Navigation-Compose, Room, DataStore, kotlinx.serialization,
  repository pattern. No DI framework.
- Single-Activity (`MainActivity.kt`): native `androidx.core:core-splashscreen` startup
  (`Theme.Kumbuka.Starting` in `themes.xml`) owns the first frame, then `KbRoute.SPLASH`
  (`ui/screens/splash/SplashScreen.kt`) plays a ~1.5s Compose icon/wordmark animation before
  routing on — see "Native startup" below.
- `ui/screens/home/HomeScreen.kt` is the one shell Scaffold: owns the toolbar, bottom nav
  (`ui/components/BottomNavBar.kt`, tabs Today/Units/Marks/Settings in that order) and
  tab-content switching. Tab bodies live in `TodayTabContent.kt`, `UnitsTabContent.kt`,
  `MarksTabContent.kt`, `SettingsTabContent.kt` under `ui/screens/home/`. Insights
  (`InsightsScreen.kt`) is a separate route reached from Today and Settings, not a fifth tab.
- Import/export, pack authoring, topic detail, and Session are secondary/full-screen routes off
  the shell, wired in `ui/navigation/KumbukaNavGraph.kt`.
- `domain/scheduler/TodayScheduler.kt` computes both scheduler arms (`SchedulerArmKind.BASELINE`,
  `SchedulerArmKind.PLACEHOLDER`) against one snapshot/clock; `SchedulerLogRepository` persists
  every evaluation (plan + per-topic/per-arm rows) for later comparison in Insights.
- `domain/analytics/InsightsReducer.kt` is the pure, tested analytics layer behind
  `InsightsScreen.kt` — no UI-side math.

## Migration / archive rules

- Room is at schema **version 2** (`KumbukaDatabase.kt`), with an additive `MIGRATION_1_2`
  registered in the Room builder — no destructive fallback. The v1 export remains the migration
  test source (`app/schemas/dev.kumbuka.app.data.local.KumbukaDatabase/1.json`).
- Topics gained an **archive flag**. Re-importing a pack that removes a topic offers
  Keep (default) or **Archive**, never physical delete; archiving preserves the topic's ID,
  created time, local-edit flags, and all history (sessions, marks). Re-introducing a topic
  with the same ID restores it in place instead of duplicating it.
- Archived topics are excluded from active scheduling, ordinary search, and new mark topic
  selection, but remain resolvable in historic sessions, existing marks, and Insights.
  `SessionScreens.kt` blocks starting a **new** session against an archived topic while letting
  an already-active session on a topic archived mid-session finish normally.
- Scheduler plan/evaluation records are transactional — a partial write can never look like a
  completed pair of arms — and are keyed by a stable input fingerprint so repeated
  recompositions don't duplicate logs.

## Placeholder scheduler arm and the real-model adapter contract

- `PlaceholderRecallPredictor` (`domain/scheduler/TodayScheduler.kt`, `VERSION = "placeholder-v1"`)
  is a deterministic, hand-set exponential-decay stand-in seeded from the student's last rated
  after-confidence. It requires `MIN_RATED_REVIEWS_FOR_PLACEHOLDER` (3) completed, non-deferred,
  rated reviews before it will produce a prediction; below that (or on invalid output) the
  baseline result is used with an explicit fallback reason recorded.
- Baseline stays the default **displayed** arm everywhere. The placeholder/"model" arm can only
  be requested through the hidden research controls (Settings → tap the About row 7 times within
  1.5s of each tap — see `SettingsTabContent.kt`'s `aboutTapCount` gesture). This gesture is an
  organizational affordance, not a security boundary.
- Every persisted evaluation row distinguishes **requested** arm from **actual** source
  (baseline/placeholder/trained) and predictor version, so nothing can silently claim a trained
  result. Today's UI, Insights, and research controls all surface this via the
  `comparison_no_model` / `settings_research_controls_body` strings.
- **To wire in the real trained model from `SemProject_MLEngine` later**: implement the
  `RecallPredictor` interface (`fun predictRecall(topic, history, nowMillis): RecallPrediction?`)
  with the trained artifact, give it its own `SchedulerArmKind` value and version string
  (do not reuse `"placeholder-v1"`), and pass it into `evaluateBothArms` alongside — not instead
  of — the existing baseline/placeholder arms so historical placeholder comparisons remain valid.
  No network/runtime dependency exists yet for loading such a model; that plumbing is future work.

## Native startup

Startup is a deliberate two-beat sequence, restored by explicit product request after the
single-native-only version shipped:

1. A platform-owned `core-splashscreen` sequence (`Theme.Kumbuka.Starting`, installed in
   `MainActivity` before `super.onCreate`) shows the static amber mark (`splash_icon_static.xml`,
   no motion) and owns only the very first frame, gated until the real routing decision
   (onboarding vs. Today) is ready — no fixed delay here.
2. Once that decision resolves, `KumbukaNavGraph` always starts at `KbRoute.SPLASH`, which plays
   `ui/screens/splash/SplashScreen.kt`'s ~1.5s Compose choreography (icon pop-in, curve draw,
   wordmark fade-up, tagline fade-up) before navigating on to `KbRoute.ONBOARDING`/`KbRoute.HOME`.
   This is the one and only animated launch beat — see `SplashAnimationDurationMs`.

See `docs/UI-TOKENS.md`'s "Native startup ownership" section for the platform limitations that
still apply to the first beat (older API levels show the static compat mark; Android itself owns
cold/warm-start replay behavior for that first frame only).

## Import flow

`ui/screens/packs/PackScreens.kt`'s `ImportPackScreen` has one primary "Choose file" action and a
secondary "Paste text instead" toggle. A brand-new pack (`PackDiff.existingUnitId == null`)
applies immediately after parsing — no diff screen. An update to an existing unit still shows the
diff/review screen only when there's a genuine conflict, missing topic, or missing deadline
(`needsReview()` in `PackScreens.kt`). External file opening is wired end-to-end:
`AndroidManifest.xml` declares narrow `ACTION_VIEW` intent filters (JSON/plain-text MIME +
`.kbpack`/`.coursepack`/`.json` path patterns, not a broad `*/*`), `MainActivity.onNewIntent` + a
`KumbukaApplication` pending-URI field carry the URI into the nav graph, which auto-navigates to
Import and consumes the URI once (`onPendingImportUriHandled`). Reads happen off the main thread
via `ContentResolver` with explicit permission/IO/parse failure handling; nothing reports success
on malformed content.

## What's done (all 14 planning todos)

1. `execution-baseline` — confirmed green `compileDebugKotlin`/`testDebugUnitTest` at `80b5ff2`.
2. `history-research-storage` — topic archive flag, additive v1→v2 migration, plan/evaluation
   persistence schema (`c8daf11`).
3. `dual-arm-scheduler` — both-arm planning + `PlaceholderRecallPredictor` (`c8daf11`).
4. `optional-samples` — removed automatic seeding; opt-in "Try a sample pack" action only
   (`c8daf11`).
5. `startup-onboarding` — native splash, merged single-page onboarding, removed Consent/Login
   routes (`b3cc6b5`).
6. `four-tab-shell` — `HomeScreen.kt` shell + Today/Units/Marks/Settings extraction, Insights
   route, removed the now-unreachable standalone Units route (`0461085`, `a3e08cc`).
7. `today-reasons` — quiet fact-based reason rows, "Why this?" sheet, no raw scores in ordinary
   UI (`6972c9a`).
8. `focused-session` — archived-topic guard, double-tap write guard, large accessible
   `ConfidenceChoice` picker (`6a382a1`).
9. `marks-tab` — CAT/assignment/exam/past-paper support, transactional upsert, full mark list
   (`71dc5ea`).
10. `honest-insights` — deterministic `InsightsReducer.kt` replacing Progress's mixed UI math
    (`defcf5c`).
11. `simple-settings` — one grouped list, hidden research-controls gesture (`cc738d7`).
12. `simple-import` — file-first import, skip-diff for new packs, `ACTION_VIEW` wiring
    (`1ce40ed`, dead-string cleanup in `6a6a125`).
13. `polish-docs` — this document, `README.md` status note, `docs/UI-TOKENS.md` additions
    (added `titleSmall` — it was referenced across ~15 call sites but never defined in
    `KumbukaTypography`, silently falling back to Material3's default Roboto style).
14. `regression-acceptance` — see "Verified vs. blocked" below.

## Verified vs. blocked

**Verified in this sandboxed environment:**
- `compileDebugKotlin` + full `testDebugUnitTest` suite green after every commit above.
- Manual code review of both background-agent-authored batches (four-tab-shell/content batch and
  simple-import) caught and fixed real defects a green build alone did not surface: a dead
  `KbRoute.UNITS` route + unused parameter, and dead step-numbered import strings in both locales.
- English/Swahili string parity (374/374 keys, no diff) confirmed after every string-touching
  commit.

**Not verifiable — no device/emulator was available in this environment:**
- Cold-launch recordings confirming a single native splash sequence with no duplicate intro,
  flash, clipping, or stuck overlay.
- TalkBack, rotation, process-recreation, large-font, and light/dark theme walkthroughs.
- Actual `ACTION_VIEW` external launches from a file manager/chat app (manifest intent-filter
  shape was reviewed for correctness but not exercised against a real Intent).
- Fresh-install → onboarding → empty Today → import → session → mark → Insights device
  walkthrough end to end.

Anyone picking this up with a device/emulator available should run the Todo 14 device
walkthrough from the plan before shipping, specifically the external-intent and cold-launch
checks above.

## Decisions locked in (don't re-litigate)

- Stack fixed: Kotlin/Compose/Room/kotlinx.serialization/Navigation/Repository, no DI framework.
- No login/guest/accounts, ever, per this pass's explicit decision.
- No flashcards, ever (absolute rule from the original design brief).
- Release signing deferred — ship debug-signed only, for now.
- Model training/integration, research export, cloud services, and dependency upgrades unrelated
  to this pass are out of scope.

## Known landmines (still relevant)

- Always build via `scripts\build.ps1 <task>` (thin `gradlew.bat` wrapper); see its header for
  historical UNC-path/build-tools workarounds if you hit environment issues again.
- **Never delete+reinsert a Topic/Unit/Deadline on re-import** — history FKs cascade; always
  `@Upsert`/archive in place.
- Background-agent-authored large refactors reported green builds while still leaving dead
  code/routes behind twice in this pass — always grep for now-unused routes/params/strings after
  a big generated diff, don't just trust the build.
- adb isn't necessarily on PATH in a fresh shell — locate it at
  `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` if bare `adb` isn't found.

## Resume prompt

"Continue Kumbuka from HANDOFF.md — all 14 planning todos are implemented and unit-tested on
`whynot`. Next step is device acceptance: run the Todo 14 walkthrough (cold launch recording,
external ACTION_VIEW intent, TalkBack, rotation, light/dark, English/Swahili) on a real
device/emulator, since none was available during this pass."

---

## Superseded history (pre-`80b5ff2`, kept for archaeology only)

The sections below describe an earlier phase-numbered build (Login/Consent/Onboarding pager,
`TodayScreen.kt` as a single monolith, Progress/Exams placeholder tabs). All of it has since been
replaced by the architecture described above; do not use file/route names below as current fact.

- **Phase 0** (`9572e96`) — Gradle/Compose/Room scaffold, AVD `Kumbuka_Test`, `scripts/build.ps1`.
- **Phase 1** (`639b57b`) — Room entities/DAOs/repositories, 4 passing instrumented tests.
- **Phase 2** (`0ee8f9c`) — Design system, Splash/Login/Consent/Onboarding screens, DataStore
  prefs, nav graph, i18n scaffold.
- **Phase 5** — Original session flow in `SessionScreens.kt`.
- **Phase 6** — Re-import diff engine and review flow (superseded by the skip-diff-for-new-packs
  behavior in `simple-import`).
- **Phase 7** — Pack authoring flow (`PackAuthoringScreen.kt`) — retained, styling-only cleanup
  in this pass.
- **Design polish pass** — shared shape tokens, button microinteractions, 8pt spacing scale,
  typography hierarchy (`docs/UI-TOKENS.md` originated here).
- **Phase 8 "Exams & marks"** — original EXAM-only tab, since expanded into the full Marks tab
  (CAT/assignment/exam/past-paper) in this pass.
- **2026-09-08/09 audit pass** — fixed re-import delete bug, duplicate no-units cards,
  `PackAuthoringScreen` missing `BackHandler`, a stray brace blocking compile, `DeadlineDao`
  REPLACE→`@Upsert`, and a missing save-confirmation state. Superseded by the fuller history/
  archive rework in this pass, but the underlying lessons (never REPLACE over FK'd rows, always
  add `BackHandler` to multi-step wizards) still apply.
- Toolchain notes (AGP 8.6.1→9.4.0, Gradle 8.9→9.7.1, Kotlin 2.0.21→2.4.20, Room 2.6.1→2.8.4,
  compileSdk/targetSdk 36→37) remain accurate as of that pass and were not touched in this one.
