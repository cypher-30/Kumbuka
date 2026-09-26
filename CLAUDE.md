# Kumbuka: project notes for Claude

Android revision scheduler. Kotlin, Jetpack Compose (Material3), Room (v3), DataStore, Navigation-Compose.
No DI framework, no ViewModels. Package `dev.kumbuka.app`. Start with `HANDOFF.md` (current state,
gaps, gotchas); this file holds the standing rules.

## Branch and git

- Everything is on `main` (PR #1 merged the redesign; history rewritten 2026-11-22, see `HANDOFF.md`).
  `redesign/usiku-na-jua` and `whynot` are kept only as old pointers.
- Commit or push only when the user asks. Never open a PR unless asked.
- Never add `Co-authored-by:` trailers for AI tools (Copilot, Claude): the user removed Copilot from the
  repo's contributors on purpose.
- Team members, for commit authorship when the user asks: Travis Njenga
  `168578245+Trav1s5@users.noreply.github.com`, Jeremy `206922062+Byterem@users.noreply.github.com`.

## Build and test

- Build only through `.\scripts\build.ps1 assembleDebug testDebugUnitTest` (PowerShell).
- Install: `adb -s <serial> install -r app\build\outputs\apk\debug\app-debug.apk`.
- `lintDebug` has pre-existing errors unrelated to recent work; do not claim it is clean.
- Unit tests run on the JVM (`testDebugUnitTest`); instrumented tests need a device or emulator.
- English and Kiswahili strings must stay in parity (564/564 `<string>` keys). Add every string to
  `values/strings.xml` and `values-sw/strings.xml`.

## Standing rules

- **System bars (every Android UI change):** `statusBarColor` and `navigationBarColor` are transparent
  in the theme; `MainActivity` calls `enableEdgeToEdge(SystemBarStyle.auto(...))` driven by the app's own
  theme in a `DisposableEffect(darkTheme)`; content is padded with window insets. Screenshot the top of the
  screen in light AND dark (`adb exec-out screencap -p`) and confirm no grey band before calling it done.
- **Theme:** offer System / Light / Dark in Settings; the default is Light.
- **Auto-backup stays on.** Do not change `allowBackup`.
- Never use negative `.dp` padding (crashes Compose).
- Width caps: `Modifier.widthIn(max).fillMaxSize()` caps, `fillMaxSize().widthIn(max)` does not. Use
  `kbContentWidth()` for centred, capped content.
- Never delete and reinsert Topic, Unit or Deadline rows (use upsert or archive). No login, no accounts,
  no flashcards.
- Private semester Course Packs stay out of the repo. `docs/` is gitignored and local only.
- Personal goals (`units.isPersonal`) never count toward research data or the CSV export.
- Debug-only demo history lives in `DemoHistorySeeder` and must stay hidden in non-debuggable builds.

## Working style the user asked for

- Plan after plan, in the main session; do not delegate to many sub-agents.
- Keep UI close to the Claude Design redesign, but an exact replica is not required.
- Say plainly what is verified on a device and what is not.

## Devices

Phone serial `22309369` (OPPO, portrait-locked; `pm clear` and `screenrecord` blocked). Tablet
`R52T10DCT1H` (Galaxy Tab S7+). Do not use `cmd locale set-app-locales` for testing: it conflicts with the
app's saved language. Change language in Settings.

## Figma

File `v24q8OFYX1D0navj77jrVe`, page "Kumbuka v2" holds the clickable phone prototype (light and dark).
Details and gotchas are in `HANDOFF.md`. Use a fresh browser tab for each prototype load; the page is heavy.
