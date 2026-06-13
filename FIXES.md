# FIXES.md — Wallora round 2 (post-install feedback)

Same rules as CLAUDE.md: work top to bottom, check off each item the moment it's done,
log root causes and decisions in DECISIONS.md, keep the build green after every fix,
run unit tests after each group, do not stop until every item is checked.

Test device context (target your manual reasoning at this): Google Pixel 6 Pro,
Android 17 QPR Beta 3. **compileSdk/targetSdk kept at 35** — black-image bugs are
decode-flag issues, not SDK-gated; bumping forces an AGP upgrade with no device to
validate, so staying at 35 (noted in DECISIONS.md).

---

## Root-cause analysis (completed in plan phase)

Two distinct root causes were identified — NOT one as originally hypothesised:

**Root cause #1 — Coil full-res HARDWARE bitmap / GPU texture-size overflow.**
`WalloraApp.kt` global `ImageLoader` never sets `allowHardware(false)`; full-res originals
(Wallhaven 5-6K+, Pexels `src.original`, Unsplash `urls.full`) exceed the Pixel 6 Pro GPU
max texture size → HARDWARE bitmap can't be uploaded by Compose's `RecordingCanvas` →
renders **black**. Thumbs (server-resized to ≤1280px) are under the cap → render fine.
`DetailScreen.kt` also has no `error`/`placeholder`, so any failure shows a near-black
`surfaceVariant` box. "Black behind apply-progress" = this same preview showing through.

**Root cause #2 — Live engine never receives a bitmap (A2-live + E1).**
`WalloraEngine.loadBitmap()` has **zero callers**. `currentBitmap` starts null;
`renderOnCanvas()` paints `Color.BLACK` whenever null. `NextWallpaperUseCase` always routes
through `WallpaperManager.setBitmap` (static), never signalling the engine.
`setOffsetNotificationsEnabled(true)` is never called; bitmap is never made over-wide.
Result: live wallpaper is permanently black; rotations set an invisible static wallpaper
under the live service; parallax has no bitmap to translate.

---

## Group A — image pipeline (HIGH PRIORITY)

- [x] A1. **Detail preview black (Root cause #1).** In `DetailScreen.kt` build the
      full-res `ImageRequest` with `.allowHardware(false)` (software bitmap → always
      drawable; sidesteps GPU texture overflow and HDR/gainmap quirks on Android 17 QPR),
      and `.size(displayWidth, displayHeight)` to downsample below the texture cap before
      decode (Coil passes this as `BitmapFactory.Options.inSampleSize`). Add `.error(...)`
      and `.placeholder(...)` painters so a failed load never shows pure black. Add a
      `Listener` that logs `onError` to `android.util.Log` tagged "WalloraDetail". Keep
      the grid on hardware bitmaps (small thumbs are fine).

- [x] A2-static. **"Black behind apply progress" (Root cause #1 — resolved by A1).**
      `ApplyWallpaperUseCase` already fully decodes via `SafeBitmapDecoder.decodeRegion`
      before `WallpaperManager.setBitmap`, so static apply is NOT half-decoded. The
      black-behind-progress is the A1 preview showing through. No code change beyond A1.
      Check off once A1 is done.

- [x] A2-live. **Live engine bitmap wiring (Root cause #2).** Add a `current_wallpaper`
      key to `SettingsRepository` (JSON-serialised `Wallpaper` or just `fullUrl+thumbUrl`
      strings). `NextWallpaperUseCase.invoke()` writes the picked wallpaper there after
      every rotation. `WalloraEngine` observes the new Flow (like it already observes
      `defaultEditParams`/`parallaxEnabled`); on each emission, download + decode on IO to
      an over-wide bitmap (screen width × 1.3, bounded by source width, ARGB_8888), then
      call the existing `loadBitmap(bitmap)`. On `onSurfaceCreated` with no persisted
      wallpaper, kick `nextWallpaperUseCase(HOME)` once. Re-verify that `notifyColorsChanged`
      fires after load. (Shared with E1 — do together.)

- [x] A3. After fixing A1/A2: add DECISIONS.md entry documenting both root causes and the
      chosen decode flags. Add a unit test for `SafeBitmapDecoder.calculateInSampleSize`
      on an oversized image (pure JVM). Add a manual check list entry for HDR/large-image
      testing on-device.

## Group B — gestures & quick controls

- [x] B1. **Bindable "next wallpaper" trigger.** Add exported no-UI trampoline
      `NextWallpaperActivity` (Theme.Wallora.Trampoline — translucent/NoDisplay,
      `excludeFromRecents=true`, `taskAffinity=""`). It injects `NextWallpaperUseCase` via
      Hilt and launches it on an `@ApplicationScope` CoroutineScope so the work survives
      `finish()`. Register intent-filter action `com.wallora.app.action.NEXT_WALLPAPER`
      (for Tasker / Shortcuts). Add `res/xml/shortcuts.xml` with one static shortcut
      "Next wallpaper" pointing to `NextWallpaperActivity`; wire `meta-data` on
      `MainActivity` in the manifest. Change the live-wallpaper double-tap default from
      **ON** to **OFF** in `SettingsRepository` (`?: true` → `?: false`) and matching
      `SettingsViewModel` `stateIn` seed; add a subtitle in Settings explaining "Most
      launchers consume the double-tap; use a launcher gesture instead." Update README with
      "How to bind a Nova gesture to Next Wallpaper."

- [x] B2. **QS tile monochrome icon.** Create `res/drawable/ic_qs_next.xml` — a 24dp
      monochrome vector combining a wallpaper/image frame and a next/refresh arrow, single
      color (`#FFFFFFFF` so QS colours it). In `WalloraQsTileService` replace
      `Icon.createWithResource(this, R.mipmap.ic_launcher)` with
      `Icon.createWithResource(this, R.drawable.ic_qs_next)`. Update `<service>` manifest
      `android:icon` likewise.

## Group C — widget

- [x] C1. **Widget applies next wallpaper in place.** Create
      `widget/NextWallpaperAction.kt` implementing `ActionCallback`; obtain
      `NextWallpaperUseCase` via `EntryPointAccessors.fromApplication(context,
      WalloraEntryPoint::class.java)` (Glance callbacks aren't Hilt-injected). Call
      `nextWallpaperUseCase(WallpaperTarget.BOTH)`. In `WalloraWidget.kt` change the
      primary `clickable(...)` from `actionStartActivity<MainActivity>()` to
      `actionRunCallback<NextWallpaperAction>()`. Keep a secondary "Open app" text/button
      area if it fits cleanly; otherwise widget = shuffle-only (documented).

## Group D — state persistence

- [x] D1. **Category chip selection survives navigation.** `HomeViewModel.selectedCategories`
      is a transient `MutableStateFlow(emptySet())` — not persisted. Wire it to the EXISTING
      `SettingsRepository.selectedCategories` Flow and `setSelectedCategories()` (already
      used by `SettingsViewModel`). `toggleCategory` writes through the repository; the
      ViewModel reads from it via `stateIn`. Single source of truth; survives process death.

## Group E — parallax (Nova Launcher 8)

- [x] E1. **Parallax actually moves (Root cause #2, shared with A2-live).** After the
      engine bitmap wiring (A2-live), ensure: (a) `setOffsetNotificationsEnabled(true)` in
      `Engine.onCreate`; (b) bitmap decoded at `overwideBitmapWidth = (surfaceWidth * 1.3f)
      .roundToInt()` (or source width, whichever smaller); (c) `desiredMinimumWidth` returns
      the over-wide width so launchers send meaningful offsets; (d) `onOffsetsChanged` wired
      to use `ParallaxMath.translateX` with smoothing; (e) fixed-offset / no-offset
      launchers fall back to centred. Add `if (BuildConfig.DEBUG) Log.d(TAG, "offset=$xOffset")`
      guarded debug logging. README: "Nova Launcher — enable Settings → Scroll wallpaper."

## Group F — performance (LOW priority)

- [x] F1. **Performance pass.** Coil memory (20%) + disk (250 MB) already tuned — leave.
      Confirm `WallpaperGrid` thumb requests are size-constrained (no oversized thumbs).
      Baseline Profile generation requires a device/managed emulator (unavailable per
      CLAUDE.md) — defer and document in DECISIONS.md. Optionally hand-author a minimal
      `baseline-prof.txt` for cold-start critical paths if it doesn't break the build.
      `@Immutable` annotation on `Wallpaper` if the class is structurally stable — check
      first.

## Final verification

- [x] `./gradlew assembleDebug` green, `./gradlew testDebugUnitTest` green,
      `./gradlew lintDebug` reasonably clean (no new errors). Update README and
      DECISIONS.md. Final commit.
