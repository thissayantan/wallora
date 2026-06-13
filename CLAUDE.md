# CLAUDE.md — Wallora project rules

## Project status
The app is fully built and shipped. SPEC.md, TASKS.md, and FIXES.md have been deleted
(all complete). Active reference docs: README.md (user-facing), DECISIONS.md (architecture
log). For new features or bug fixes, log decisions in DECISIONS.md and keep the build green.

## Autonomy rules
- Work continuously on the requested task without pausing for confirmation.
- If ambiguous, choose the most reasonable option and append a short entry to DECISIONS.md
  (date, decision, why).
- If a third-party API behaves differently than expected, adapt and note it in DECISIONS.md.
- If you hit the same build/test failure 3 times, simplify rather than loop; record the
  trade-off in DECISIONS.md.

## Build & verify discipline
- After every code change: `./gradlew assembleDebug` must succeed.
- After every feature/fix: `./gradlew testDebugUnitTest` must pass.
- No device/emulator CI; Compose UI tests run with Robolectric or are `@Ignore`d with TODO.
- NEVER use sudo — all tooling is installed user-locally (~/.local, ~/Android/Sdk).
- JAVA_HOME=/home/sayantan/.local/jdk17

## Code conventions
- Kotlin official style; small focused files; no god-classes.
- Compose: stateless composables + ViewModel state holders.
- All user-visible strings in strings.xml. No hardcoded API keys anywhere; keys come from
  local.properties → BuildConfig with safe empty defaults.
- Commit format (gitmoji): `<emoji> <type>(<scope>): <description>`
  Emoji map: ✨ feat · 🐛 fix · 📝 docs · ♻️ refactor · ⚡ perf · ✅ test
             🔒 security · 🔧 chore · 🗑️ remove · 🏗️ arch · 💄 ui · 🎉 init
  Scopes: ui · api · worker · db · config · infra · ci · docs · search
- Errors fail soft in the UI (per-source failures must not blank the whole grid).

## What not to do
- Do not add accounts, analytics, ads, NSFW content, or Play signing configs.
- Do not over-modularize the Gradle build or introduce unrequested architecture.
- Do not ask the user to run commands for you; run them yourself.
