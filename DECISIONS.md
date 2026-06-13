# DECISIONS.md — Wallora

Decisions made during autonomous execution (date, decision, why).

---

## 2026-06-13

**JDK**: Eclipse Temurin 17.0.13 LTS (OpenJDK 17) installed to ~/.local/jdk17.
Chose JDK 17 over 21 for broadest AGP 8.x compatibility; Gradle 8.9 supports both
but some AGP versions warn on 21 with certain flags. JDK 17 is the safe default.

**Reddit datacenter block**: Reddit returns HTTP 403 from this server's IP address.
This affects build-time network tests only — the app itself runs on-device where
Reddit is accessible. Reddit source implemented and tested against committed JSON
fixtures; runtime is fail-soft (per-source failure never blanks the grid).

**Paging architecture**: Used a custom fan-out PagingSource (not RemoteMediator+Room)
for multi-source round-robin interleave. Room is used as a side TTL page cache
(lookup by key), not as the Paging 3 backing store. This avoids the complexity of
RemoteMediator coordinating N heterogeneous sources and gives direct control over
interleaving and dedup logic.

**Rotation pre-fetch**: TASKS.md P3-d mentions pre-fetching the next wallpaper on Wi-Fi
as a background warm-up. Deferred from P3-d implementation — true pre-fetch requires an
on-device persistent image cache beyond OkHttp's in-memory cache (e.g., writing to a
temp File, then using that file on next apply). The rotation apply itself is already
fast once the full-res URL is known. Pre-fetch will be revisited in Phase 6 performance
pass if needed; the mechanism (kick off OkHttp download, store to cacheDir file) is
straightforward to add then.

**Splash screen theme parent**: `Theme.Wallora` now extends `Theme.SplashScreen` (from
`core-splashscreen 1.0.1`) and declares `postSplashScreenTheme = Theme.Wallora.Main`.
`MainActivity.installSplashScreen()` must be called before `super.onCreate()` to honour
the splash. The base `Theme.Wallora.Main` parent remains
`android:Theme.Material.Light.NoActionBar`; Compose handles all runtime theming via
`WalloraTheme` (dynamic color + dark mode).

**Coil memory cache type**: `MemoryCache.Builder.maxSizeBytes()` in Coil 2.7.0 takes
`Int`, not `Long`. Memory cache sized at 20% of maxMemory(), coerced to [32 MB, 256 MB]
then cast to Int. Disk cache uses `Long` (DiskCache.Builder.maxSizeBytes accepts Long)
set to 250 MB.

**Non-RenderScript blur**: `ImageAdjustments.applyStackBlur()` uses a three-pass
downscale → iterative box blur → upscale algorithm. No `android.renderscript` or
`androidx.renderscript` dependency — fully compatible with minSdk 31 without the
deprecated RenderScript Intrinsics Replacement Toolkit.

---

## 2026-06-13 (round 2 — post-install fixes)

**Root cause A1 — black detail preview (GPU max-texture-size overflow)**:
`WalloraApp.kt` global `ImageLoader` defaults to `allowHardware=true`. Full-res originals
from Wallhaven (5K+), Pexels (`src.original`), and Unsplash (`urls.full`) routinely exceed
the Pixel 6 Pro GPU max texture size (≈4096–8192px). A HARDWARE `Bitmap` that large cannot
be uploaded by Compose's `RecordingCanvas` → renders black. Thumbnails (server-resized to
≤1280px) stay under the limit → render fine. Fix: `DetailScreen.kt` `ImageRequest` now
uses `.allowHardware(false)` (software bitmap, always drawable regardless of size) and
`.size(displayW, displayH)` so Coil's `inSampleSize` downsamples before decode. Visible
error overlay added — load failure never shows a silent black box. Hardware bitmaps kept
for the grid (thumbs are always small).

UltraHDR/Display-P3 on Android 17 QPR Beta 3 is a contributing factor: gainmap images
decoded without sRGB pinning can also render incorrectly. `allowHardware(false)` sidesteps
both issues for the detail preview path.

**Root cause A2-live — live engine permanently black (zero callers on loadBitmap)**:
`WalloraWallpaperService.WalloraEngine.loadBitmap()` had zero callers. `currentBitmap`
started null; `renderOnCanvas` painted `Color.BLACK` whenever null. `NextWallpaperUseCase`
always routed through `WallpaperManager.setBitmap` (static apply), never signalling the
engine. Fix: added `current_wallpaper_full_url` + `_thumb_url` keys to `SettingsRepository`;
`NextWallpaperUseCase` persists the picked wallpaper on every rotation; in live-wallpaper
mode avoids `setBitmap(FLAG_SYSTEM)` (which would deactivate the live wallpaper). The engine
observes `currentWallpaperUrls` Flow and calls `loadWallpaperFromUrl()` → `loadBitmap()`.
On first surface ready with no persisted URL, kicks `nextWallpaperUseCase` once.

**Root cause E1 — parallax never moved**: `setOffsetNotificationsEnabled(true)` was never
called in `Engine.onCreate`, so the launcher never delivered xOffset values. Bitmap was
never made over-wide. Fix: added `setOffsetNotificationsEnabled(true)` in `onCreate`;
`loadWallpaperFromUrl` decodes at `ParallaxMath.PARALLAX_SCALE` (1.3×) × `surfaceWidth`.
`ParallaxMath.clampTranslateX` applied to avoid edge gaps. DEBUG-gated offset logging added.

**compileSdk/targetSdk kept at 35**: FIXES.md suggested bumping for Android 17 QPR Beta 3.
The black-image bugs are decode-flag issues, not SDK-gated. Bumping compileSdk past 35 would
require an AGP upgrade (8.5.2 → 8.6+/8.7+) with no device to validate runtime behaviour
changes. Decision: stay at 35, fix the actual root causes. Revisit if a specific API ≥36
is required.

**D1 — category persistence**: `HomeViewModel.selectedCategories` was a transient
`MutableStateFlow(emptySet())` — not persisted. Wired to the EXISTING
`SettingsRepository.selectedCategories` DataStore flow (already used by `SettingsViewModel`).
Single source of truth; survives navigation and process death.

**B1 — double-tap default changed to OFF**: Most launchers (Nova, Lawnchair, etc.) consume
double-tap on the home screen for their own gesture (e.g. lock screen, app drawer). Leaving
double-tap ON causes confusion. Default changed from `?: true` to `?: false`. Alternative
provided: `NextWallpaperActivity` trampoline (exported, `com.wallora.app.action.NEXT_WALLPAPER`)
bindable as a Nova gesture shortcut or Tasker action; static shortcut in `shortcuts.xml`.

**C1 — widget applies in-place**: Widget previously launched `MainActivity` on tap.
Changed to `actionRunCallback<NextWallpaperAction>()`. `NextWallpaperAction` is a Glance
`ActionCallback` that retrieves `NextWallpaperUseCase` via `EntryPointAccessors` (Glance
callbacks are not Hilt-injected). App-name label retained as a way to open the app.

**F1 — Baseline Profile deferred**: Generating a Baseline Profile requires running
`BaselineProfileRule` or `MacrobenchmarkRule` on a real device or managed emulator. Neither
is available in this environment (CLAUDE.md: "no device/emulator attached"). Deferred.
WallpaperGrid already uses stable keys, hardware bitmaps for thumbs, and correctly-sized
requests. `@Stable` added to `Wallpaper` data class (List<String> tags field prevents
automatic inference). Coil cache (250MB disk, 20% heap) left unchanged.

**Manual check list for HDR/large-image decode (A3)**:
On device: open detail screen for a Wallhaven full-res image (5K+). Verify image renders
(not black). Check Logcat tag `WalloraDetail` for any `onError` entries. Test with an
UltraHDR image (Pixel 6 Pro native HDR camera shot uploaded to Pexels/Unsplash). Verify
live wallpaper engine shows the image (not black) after setting as live wallpaper and
rotating once. Enable Nova "Scroll wallpaper" and verify xOffset values appear in Logcat
tag `WalloraEngine` (DEBUG build only).
