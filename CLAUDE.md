# CLAUDE.md — Wallora project rules

## Mission
Build the Android wallpaper app defined in SPEC.md by completing every task in
TASKS.md, in order, without stopping for confirmation.

## Autonomy rules (important)
- Work continuously until ALL tasks in TASKS.md are checked off. Do not pause to ask
  questions. If something is ambiguous, choose the most reasonable option consistent
  with SPEC.md and append a short entry to DECISIONS.md (date, decision, why).
- Never wait for user approval between tasks or phases. The only acceptable stopping
  point is "all TASKS.md items complete and the final verification passes".
- If a third-party API behaves differently than expected, adapt the integration,
  note it in DECISIONS.md, and keep going. If a source is fully blocked (e.g. no API
  key available at build time), implement it behind the isConfigured=false path with
  JSON-fixture-backed unit tests, mark the task done, and note it.
- If you hit the same build/test failure 3 times, simplify the approach rather than
  looping; record the trade-off in DECISIONS.md.

## State tracking (survives context compaction)
- TASKS.md is the single source of truth for progress. Immediately after finishing a
  task, edit TASKS.md to check it off ([ ] → [x]). Never batch checkbox updates.
- Keep DECISIONS.md updated as described above.
- If context was compacted or the session restarted: re-read CLAUDE.md, SPEC.md,
  TASKS.md, DECISIONS.md, then resume at the first unchecked task.

## Build & verify discipline
- After every task that touches code: `./gradlew assembleDebug` must succeed.
- After every phase: `./gradlew testDebugUnitTest` must pass. Fix failures before
  moving on. Never check off a task with a broken build.
- There is no device/emulator attached; do not block on instrumented tests or
  `connectedAndroidTest`. Compose UI tests should be written to run with Robolectric
  where feasible; otherwise write them and mark them @Ignore with a TODO note.
- If the Android SDK or a dependency is missing from the environment, install or
  bootstrap it yourself (sdkmanager / gradle wrapper) rather than stopping.
- NEVER use sudo or any command that can prompt for a password — it would hang the
  session. All tooling must be installed user-locally (~/.local, ~/Android/Sdk) per
  TASKS.md Phase 0. If something genuinely seems to require root, find a userspace
  alternative and note it in DECISIONS.md.

## Code conventions
- Kotlin official style; small focused files; no god-classes.
- Compose: stateless composables + ViewModel state holders; previews for key screens.
- All user-visible strings in strings.xml. No hardcoded API keys anywhere; keys come
  from local.properties → BuildConfig (see SPEC.md §3) with safe empty defaults.
- Commit with git after each task, one task ≈ one commit. Always use gitmoji format:
  `<emoji> <type>(<scope>): <description>` — e.g. `✨ feat(ui): add wallpaper grid`
  Emoji map: ✨ feat · 🐛 fix · 📝 docs · ♻️ refactor · ⚡ perf · ✅ test
             🔒 security · 🔧 chore · 🗑️ remove · 🏗️ arch · 💄 ui · 🎉 init
  Scopes: ui · api · worker · db · config · infra · ci · docs · search
- Errors fail soft in the UI (per-source failures must not blank the whole grid).

## What not to do
- Do not add accounts, analytics, ads, NSFW content, or Play signing configs.
- Do not over-modularize the Gradle build or introduce unrequested architecture.
- Do not ask the user to run commands for you; run them yourself.
