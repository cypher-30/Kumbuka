# Kumbuka — session handoff

## Latest: "Usiku na Jua" redesign (read this first)

**Branch:** `redesign/usiku-na-jua` (pushed to `origin`, no PR opened). Commit `f90965d` sits on
`fe3e5e7`. `main` is untouched. Nothing is merged.

This pass replaced the Campus Workspace look with the Claude Design redesign (Claude Design project
`19535662-7f6e-4a0b-afb3-b545a907af3d`, files `Kumbuka Prototype.dc.html` and `Kumbuka Brand.dc.html`),
tested on a real phone (OPPO CPH2159) and a real tablet (Galaxy Tab S7+). The user said "close to
Claude Design" is good enough, not a pixel replica.

### What changed in the app

- **Theme** (`ui/theme/Color.kt`, `Theme.kt`, `Type.kt`): Midnight `#172238`, Clay `#B4442A`,
  Saffron `#F0B23E`. The light palette was deliberately calmed (paper `#EFE9DD`, surface `#FBF9F4`,
  hairline borders) after the user found the design's brighter light mode "too bright". Newsreader
  headings (`res/font/newsreader.ttf`), Plus Jakarta Sans body. **Light is the default theme**;
  Settings still offers System / Light / Dark (`AppPreferences.themeMode`).
- **System bars:** transparent in `themes.xml` + `enableEdgeToEdge(SystemBarStyle.auto(...))` in a
  `DisposableEffect(darkTheme)` in `MainActivity`. This fixes the grey status-bar strip (rule is also
  in the user's global `~/.claude/CLAUDE.md`). Always check light and dark on a device.
- **Splash:** native animated icon only (Android 12+). The Compose `SplashScreen.kt` and
  `KbRoute.SPLASH` are deleted. Older APIs show the static mark.
- **Shell:** tabs are now **Today / Units / Assessments / Progress**. Rail + two-pane layout at
  >= 840dp (tablet landscape); rotation follows the device (no lock). `kbContentWidth()` caps content
  at 720dp and centres it (it had a modifier-order bug that never capped; fixed).
- **Onboarding** (`OnboardingScreen.kt`): 3 steps (language + how it works, consent with one
  required and one optional box, first unit or Skip). Optional study participation is **unticked by
  default** (the design pre-ticks it).
- **Today:** Midnight hero with progress, next-date chip, due banner, featured card with Why this?,
  queue rows with play buttons, Coming up.
- **Session:** restyled; defer is an `AlertDialog`; new **Done screen** (confidence change, time,
  Next up with "Start next topic", reminder nudge). "Start next topic" sets
  `shell.pendingStartTopicId` and Today starts it through its normal logged path. The reminder nudge
  asks for the Android 13 notification permission in context and enables the reminder.
- **Create unit** (`CreateUnitScreen.kt`, replaces the deleted `PackAuthoringScreen.kt`): Course
  (code, topics with Low/Medium/High weight, dates, share toggle) or Personal goal (title +
  objectives). Route is still `KbRoute.AUTHOR_PACK`.
- **Paste outline** (`PasteOutlineScreen.kt`, parser in `pack/OutlineParser.kt`, ported from the
  prototype's `parseOutline`). Add-unit sheet: Import / Paste outline / Create unit.
- **Import** restyled (dashed file picker, paste link, sample card).
- **Settings:** grouped list with icon tiles; Session length / Appearance / Language / Reminder open
  choice sheets; Privacy and Credits open info sheets; Version tapped 7x unlocks Research
  diagnostics. **Data group:** research participation (join/withdraw dialogs) and **Export review
  log (CSV)** via `CreateDocument("text/csv")`, enabled only when participating
  (`domain/analytics/ReviewLogExporter.kt`; ids only, excludes sample and personal units).
- **Units tab:** reviewed-progress bars; goal pill on personal goals; share icon hidden for goals.
  **Assessments:** pill Dates/Results control plus an "Add mark" FAB.
- **Debug-only demo history** (`data/bootstrap/DemoHistorySeeder.kt`): 5 units, ~150 reviews over
  ~90 days, dates and marks. Settings -> tap Version 7x -> Research diagnostics -> "Load demo
  history". Hidden in non-debuggable builds. It writes fake study data into the DB.

### Data

- **Room v3.** `MIGRATION_2_3` adds `units.isPersonal INTEGER NOT NULL DEFAULT 0` (additive).
  Verified by installing over a v2 database on both phone and tablet; data survived.
- Personal goals are **excluded from the research log and export** (`researchExcludedTopicIds` in
  `HomeScreen`) but still count in Progress.
- `allowBackup` stays **true** (user's decision). After uninstall + reinstall the app restores its
  data, so a clean state needs clearing storage by hand.

### Tests

45 unit tests, all passing at `f90965d` (`.\scripts\build.ps1 assembleDebug testDebugUnitTest`).
New: `OutlineParserTest`, `ReviewLogExporterTest`, `completedToday` in `WorkspaceLogicTest`.
EN/SW string parity: 569/569 keys. Always add every string to both locales.

### Verified on devices

Phone and tablet: onboarding, Today, Why this?, full session to Done, Units/Assessments/Progress,
Settings and sheets, join/withdraw dialogs, CSV save dialog opens (cancelled, no file written),
Add unit, Create unit, Paste outline, Import screen, light/dark, Kiswahili, portrait + landscape,
the v2->v3 migration.

### Not verified / known gaps

- A real CSV save (contents only covered by unit tests), Import with a real pack file, external
  file open, large-font, process recreation, TalkBack.
- Mark editor was only visually checked; no save-then-edit round trip this pass.
- Progress charts are simple (week bars, confidence mix, coverage). No per-unit graphs.
- Release build is **not** done: signing is still deferred, so the shareable APK is a debug build
  (`Kumbuka-redesign-f90965d-debug.apk` in the user's Downloads; 0.1.0, minSdk 26).
- `lintDebug` still has pre-existing errors unrelated to this work.
- The tablet app language/theme were reset to English/Light after testing.

### Figma (separate from the repo)

File key `v24q8OFYX1D0navj77jrVe`. Page **"Legacy (pre-redesign)"** = the old frames, untouched.
Page **"Kumbuka v2"** = variables (Light/Dark colour, spacing, radius), text styles, components
(Button, Chip, StatusPill, TopBar, BottomNav, logo), bare screens, and the prototype.
- Prototype frames are the **"▶" wrappers** (phone bezel + status bar around a clone of each screen).
  `▶ D · ...` are the dark twins (colour mode switched). Rating states (none/Blank/Shaky/OK/Solid)
  and a share-switch on/off frame make ratings and the switch interactive. Chips toggle through
  component variants and are **not exclusive**.
- Flow starts: `▶ 00 Splash start` (light), `▶ D · 00 Splash start` (dark), two tablet frames.
- Play: `https://www.figma.com/proto/v24q8OFYX1D0navj77jrVe/Kumbuka?node-id=256-2863&starting-point-node-id=256-2863&scaling=contain&content-scaling=fixed&hide-ui=1`
  (dark start node `264-3003`).
- Audit script result: all 92 phone frames reachable, no dead ends. Present mode's own device frame
  can't be set through the API, so the phone is built into the frames; set Figma's device to None.
- Figma gotchas: load fonts before changing text sizing; toggle `layoutSizingHorizontal`
  HUG->FILL to force relayout; `createAutoLayout` frames have a default white fill; a frame cloned
  with variable bindings can render stale in screenshots (re-take).

### Tooling gotchas

- Build only via `scripts\build.ps1`. Install with `adb -s <serial> install -r app\build\outputs\apk\debug\app-debug.apk`.
- Devices: phone serial `22309369` (portrait-locked, `pm clear` and `screenrecord` blocked by
  ColorOS); tablet `R52T10DCT1H`. `cmd locale set-app-locales` conflicts with the app's own saved
  language and gives mixed-language UI; change language in Settings instead.
- Chrome extension: the Figma prototype page is heavy; loading a second prototype link in the same
  tab froze the renderer repeatedly. Use a fresh tab per prototype load.
- Write files with the Write tool; heredocs with apostrophes fail in this shell. Never use
  negative `.dp` padding (it crashed onboarding).

### Next steps (suggested)

1. Make the weight/language/unit chips single-select in Figma (state frames) if wanted.
2. Release build: signing, shrinker check, a pass over the unverified items above.
3. Richer Progress charts and per-unit graphs; real-pack Import test.
4. Decide merge: open a PR from `redesign/usiku-na-jua` into `main` (not done).

---

## Previous: Campus Workspace redesign (superseded by the section above)

The "quiet editorial" UI was replaced by a **Campus Workspace** design (see
`docs/UI-TOKENS.md`): indigo/violet/amber palette, Plus Jakarta Sans, stable per-unit colours,
and a new information architecture:

- Tabs are now **Home / Units / Assessments / Progress** (`KbNavTab`). Settings is its own
  route (`KbRoute.SETTINGS`, `ui/screens/settings/SettingsScreen.kt`) opened from the gear icon;
  hidden research controls moved to `ResearchDiagnosticsScreen.kt` (`KbRoute.RESEARCH`, still
  unlocked by tapping Version 7 times).
- `HomeScreen.kt` is the shell; tab bodies are `HomeTabContent.kt` (hero, recommended next,
  queue, coming up), `UnitsTabContent.kt` (search + your/sample units), `AssessmentsTabContent.kt`
  (Dates/Results segments, unit filter; replaces Marks), `ProgressTabContent.kt` (replaces
  Insights). Shell state (tab, segment, unit filter, pending snackbar message) lives in
  `WorkspaceShellState.kt`.
- New routes: unit detail (`unit/{unitId}`), full-screen mark editor
  (`mark-edit?markId=&unitId=`). Topic detail is read-first with a Start revision action.
- Pure date/greeting/activity helpers are in `WorkspaceLogic.kt` with `WorkspaceLogicTest`.
  Home passes **active (non-archived) topics** to the scheduler.
- Verified: unit tests (29+), `assembleDebug`, and an emulator walkthrough (onboarding → sample
  import → Home → Why this → unit → topic → full session → add mark → Assessments/Progress →
  Settings, dark theme, Kiswahili). `lintDebug` still reports pre-existing errors
  (`local.properties` escaping, manifest app-link host, `LocalContext` resource reads in
  import/session code, `MainActivity` configuration read); none come from the redesign files.
- Private semester Course Packs are generated outside the repo (session artifacts) and are
  never committed.

The sections below describe the previous pass; where they mention Today/Marks/Settings tabs,
`TodayTabContent.kt`, `MarksTabContent.kt`, `SettingsTabContent.kt` or `InsightsScreen.kt`,
read Home/Assessments/Settings route/Progress as above.

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
