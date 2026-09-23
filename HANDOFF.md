# Kumbuka — session handoff

## What was asked
Build the full production version of Kumbuka (offline-first Android revision scheduler)
from scratch on branch `whynot`, phase by phase: propose → implement → build → run on
emulator → click through → commit, each phase separately. Full plan approved and saved at
`C:\Users\Alvin\.claude\plans\snoopy-stargazing-starfish.md` (read it for full detail —
architecture, entity list, scheduler formula, pack format, 12-phase breakdown).

## What's done (commits on `whynot`)
- **Phase 0** (`9572e96`) — Gradle/Compose/Room scaffold, AVD `Kumbuka_Test`, `scripts/build.ps1`
  (solves the 3 UNC-path build issues — see its header comment; always build via this script).
- **Phase 1** (`639b57b`) — Room entities/DAOs/repositories in `app/src/main/kotlin/dev/kumbuka/app/data/`
  and `domain/model/`, 4 passing instrumented tests.
- **Phase 2** (`0ee8f9c`) — Design system (`ui/theme/`), Splash/Login/Consent/Onboarding screens,
  DataStore prefs, nav graph (`ui/navigation/KumbukaNavGraph.kt`), i18n scaffold. Verified on-device;
  fixed a real "returning user skips onboarding" bug found only by manual QA, not compiling.
- **Phase 5** — Session flow in `app/src/main/kotlin/dev/kumbuka/app/ui/screens/session/SessionScreens.kt`,
  active-session lookup in `data/local/dao/SessionDao.kt` + `data/repository/SessionRepository.kt`, Today card
  launch button in `ui/screens/home/TodayScreen.kt`, route wiring in `ui/navigation/KumbukaNavGraph.kt` and
  session strings in `app/src/main/res/values/strings.xml`.
- **Phase 6** — Re-import diff engine and review flow: `data/repository/PackDiff.kt`,
  `data/repository/PackDiffEngine.kt`, repository wiring in `data/repository/PackRepository.kt`,
  new UI `ui/screens/packs/PackDiffScreen.kt`, importer wiring in `ui/screens/packs/PackScreens.kt`,
  strings in `app/src/main/res/values/strings.xml`, and unit coverage in
  `app/src/test/kotlin/dev/kumbuka/app/data/repository/PackDiffEngineTest.kt`.
- **Phase 7** — Completed pack authoring flow (Add Unit 1-3 + Add Choice) in
  `ui/screens/packs/PackAuthoringScreen.kt` with topic+deadline drafting, validation (weight,
  duplicate titles, deadline date/topic links), and persistence to Unit/Topic/Deadline rows;
  route wiring in `ui/navigation/KumbukaNavGraph.kt`, Units entry action in
  `ui/screens/packs/PackScreens.kt`, strings in `app/src/main/res/values/strings.xml`, and
  export-shape tests in `app/src/test/kotlin/dev/kumbuka/app/pack/CoursePackCodecTest.kt`.
- **Font licenses** (`add76f7`) — `THIRD_PARTY_LICENSES/*-OFL.txt` (kept out of `res/font/`,
  which is type-restricted to font files only).
- **Design polish pass (Apple-inspired, Android-native)** — Introduced shared shape tokens in
  `app/src/main/kotlin/dev/kumbuka/app/ui/theme/Theme.kt`, switched primary/secondary buttons
  to subtle press-scale microinteractions in `app/src/main/kotlin/dev/kumbuka/app/ui/components/Buttons.kt`,
  replaced Today "Why this?" dialog with bottom sheet in
  `app/src/main/kotlin/dev/kumbuka/app/ui/screens/home/TodayScreen.kt`, added
  `app/src/main/kotlin/dev/kumbuka/app/ui/theme/Spacing.kt` (8pt spacing scale), refined
  typography hierarchy in `app/src/main/kotlin/dev/kumbuka/app/ui/theme/Type.kt`, and applied
  interaction consistency updates in onboarding/session screens.
- **Adaptive UI follow-through** — Added breakpoint-aware layout behavior for compact vs larger
  devices in `app/src/main/kotlin/dev/kumbuka/app/ui/screens/home/TodayScreen.kt`,
  `app/src/main/kotlin/dev/kumbuka/app/ui/screens/onboarding/OnboardingScreen.kt`, and
  `app/src/main/kotlin/dev/kumbuka/app/ui/screens/session/SessionScreens.kt` (max-content-width
  centering, row-to-column fallback on narrow screens, chip wrapping, and list animations).
- **Token guide** — Added `docs/UI-TOKENS.md` with spacing/typography/shapes/adaptive/motion
  usage conventions to keep future screens consistent.
- **Phase 8 "Exams & marks"** — Built ahead of this handoff's prior record: `ExamsTabContent`,
  `ExamResultEntryCard`, `ExamDeadlineCard`, `ExamResultCard` in
  `ui/screens/home/TodayScreen.kt`, wired to `DeadlineRepository`/`AssessmentMarkRepository`.

## Tested — phases 1-7 audit pass (2026-09-08/09)
User manually tested and confirmed Phases 1, 2, 3 working. Phases 4-7 (plus the in-progress
Phase 8 Exams work) were then code-reviewed, built, unit-tested, and partially click-tested on
the `Kumbuka_Test` emulator. Findings and fixes from that pass:
- **Fixed** — Reimport diff engine (Phase 6) could not actually delete a topic/deadline that was
  removed from the incoming pack; it only showed an informational note, contradicting DESIGN.md
  §7's "always ask" rule. Added `RemovalChoice` (KEEP/DELETE) to `PackDiff.kt`, wired
  `PackRepository.importPack` to call the existing `TopicRepository.delete()` /
  `DeadlineRepository.delete()` for rows resolved to DELETE (default stays KEEP), and added
  Keep/Delete `FilterChip`s to `PackDiffScreen.kt` / `PackScreens.kt`. Verified by code review +
  build + unit tests; not yet click-tested end-to-end on-device (needs a real reimport-with-a-
  removed-topic pass).
- **Fixed** — `TodayScreen.kt`'s `TonightTabContent` rendered both `SummaryCard` and
  `NoUnitsState` for the no-units state, showing two near-identical cards with duplicate
  Import/Browse buttons stacked on top of each other. `SummaryCard` is now skipped when
  `plan.state == TodayState.NoUnits`. Verified on-device via screenshot.
- **Fixed** — `PackAuthoringScreen.kt` had no `BackHandler`, so the system back button bypassed
  the wizard's step-back logic and silently discarded the entire draft (unit info + all topics +
  all deadlines) with no warning — reproduced twice on-device. Added a `BackHandler` routing
  through the same step-back logic as the toolbar arrow, plus a "Discard this draft?"
  confirmation dialog when there's unsaved content. Verified live on-device.
- **Fixed (unrelated, found blocking the build)** — `OnboardingScreen.kt` had a stray duplicate
  closing brace breaking the "Continue/Get started" button's `onClick` lambda, a pure syntax
  error blocking `compileDebugKotlin` entirely. Removed the stray brace.
- **Fixed by user** — `DeadlineDao.upsert` switched from `@Insert(onConflict = REPLACE)` to
  `@Upsert`, matching the fix already applied to `TopicDao`/`UnitDao` in `f637832` (REPLACE does
  delete-then-insert, which would cascade-wipe anything FK'd to a Deadline on re-import).
- **Fixed by user** — `TopicDetailScreen`'s `saveMessage` state is now actually populated after a
  successful save (was declared but never set, so no "Saved" confirmation ever showed).
- Confirmed via full read-through: Phase 4 scheduler math (`TodayScheduler.kt`), Phase 5 session
  flow (`SessionScreens.kt`), and the bulk of Phase 6's diff/merge logic were already correct —
  no other bugs found there.
- Full `compileDebugKotlin` + `testDebugUnitTest` + `installDebug` all pass on current `whynot`
  working tree as of this pass.

## Not done — next step: Phase 9/10 "Progress, Settings, Reminders, Search"
Progress and Settings tabs in `TodayScreen.kt` are still honest `PlaceholderTab` stubs. Build
Progress/Comparison analytics (Phase 9) and Settings/Reminders/Search (Phase 10) per the plan.

## UI follow-ups after polish pass
- Run full Android build/tests once local SDK build-tools mismatch is fixed (current blocker:
  Build Tools 34.0.0/aapt corruption on local machine).
- Do a visual QA sweep for typography scale increases (especially tighter screens and small
  devices) and tune any overflow hotspots.
- If desired, extend list enter/reorder motion to Exams/Progress lists for parity with Tonight.

## Decisions locked in (don't re-litigate)
- Full product build, not academic demo — pack-authoring UI (Phase 7) stays in scope.
- Stack fixed: Kotlin/Compose/Room/kotlinx.serialization/Navigation/Repository, no DI framework.
- Git: always `git -c safe.directory='*' ...`, never touch global git config.
- Release signing deferred — ship debug-signed only, for now.
- Branding assets (Phase 11) blocked on user running `/design-login` for Claude Design canvas.
- No flashcards, ever (absolute rule, §9).

## Known landmines
- Upgraded AGP 8.6.1→9.4.0, Gradle 8.9→9.7.1, Kotlin 2.0.21→2.4.20, KSP→2.3.11, Room 2.6.1→2.8.4,
  Compose BOM→2026.08.00, compileSdk/targetSdk 36→37 (2026-09-09):
  - `gradle.properties` sets `android.builtInKotlin=false` + `android.newDsl=false` — required
    because we keep the explicit `org.jetbrains.kotlin.android` plugin (alongside
    `kotlin.plugin.compose`/`kotlin.plugin.serialization`) instead of migrating to AGP 9's
    built-in Kotlin support. This opt-out is **removed in AGP 10.0** — full built-in-Kotlin
    migration is deferred, not done.
  - Room had to jump all the way to 2.8.4: 2.6.1's KSP integration crashes
    (`IllegalStateException: unexpected jvm signature V`) processing `Unit`-returning DAO
    methods under Kotlin 2.4.x.
  - compileSdk 37 required because Compose BOM 2026.08.00's libraries (1.12.0) declare a
    `minCompileSdk` of 37; android-37.0 platform was already installed locally.
  - `settings.gradle.kts` now applies the `org.gradle.toolchains.foojay-resolver-convention`
    plugin — required for `updateDaemonJvm` (and any JVM toolchain auto-provisioning) to know
    where to download a JDK from if one isn't found locally.
  - `gradle/gradle-daemon-jvm.properties` (generated via `gradlew updateDaemonJvm --jvm-version=17`)
    now pins the daemon JVM to 17, matching CLI/IDE.
- Project now lives natively at `C:\Users\Alvin\AndroidStudioProjects\Kumbuka` (moved off the
  `\\wsl.localhost\Ubuntu\var\www\html\Kumbuka` UNC mount). `scripts\build.ps1 <task>` is now a
  thin `gradlew.bat` wrapper (no more UNC-cwd/mapped-drive or Gradle cache-locking workarounds
  needed) — calling `gradlew.bat` directly from PowerShell works fine too now.
- adb screenshot coords are ×1.2 scaled down from real device resolution — multiply before tapping.
- **Reimport (Phase 3/6) must UPDATE Topic rows in place by UUID, never delete+reinsert** —
  `TopicEntity`→`SessionEntity` FK is `CASCADE`, so a delete+reinsert would silently wipe a
  student's session history, violating §7's "never touches history" rule.
- Font OFL license text lives in `THIRD_PARTY_LICENSES/`, not `res/font/` (aapt2 would likely reject it there).
- Multi-step Compose wizards (e.g. `PackAuthoringScreen.kt`) need an explicit `BackHandler` —
  system back does not automatically respect in-screen step state, and without one it pops the
  whole screen instead of stepping back, discarding any draft silently.
- adb isn't necessarily on PATH in a fresh shell — locate it at
  `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` (or `$ANDROID_HOME`/`$ANDROID_SDK_ROOT`) if
  bare `adb` isn't found.
- To install/run on a physical device instead of the emulator when both are connected, target it
  explicitly: `adb devices -l` to get its serial, then `adb -s <serial> install -r <path-to-apk>`
  and `adb -s <serial> shell monkey -p dev.kumbuka.app -c android.intent.category.LAUNCHER 1`.
  Built APKs land at `app/build/outputs/apk/debug/app-debug.apk`.
- If Android Studio's Run button / device dropdown doesn't show a physical device that `adb
  devices` sees fine from a Windows shell: check whether Studio itself is running from inside
  WSL (its own adb server won't see a USB device without a `usbipd` passthrough bridge), confirm
  the "Allow USB debugging" RSA prompt was accepted on the phone, and try restarting the adb
  server Studio manages (`adb kill-server` then reopen Studio's Device Manager) if there are two
  adb installations fighting over the same USB device.

## Resume prompt
"Continue Kumbuka from HANDOFF.md — start Phase 9/10 (Progress, Settings, Reminders, Search):
replace the PlaceholderTab stubs in TodayScreen.kt with real Progress/Comparison analytics and
Settings/Reminders/Search screens."
