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

---

## 2026-06-14

**Crash fix — CancellationException swallowed in MultiSourcePagingSource**: Toggling a
category while wallpapers are loading makes `HomeViewModel.flatMapLatest` cancel the
in-flight paging coroutine. The `CancellationException` propagated into
`MultiSourcePagingSource.load()` where a blanket `catch (e: Exception)` silently swallowed
it, corrupting coroutine state and crashing the app. Fixed by adding `catch (e:
CancellationException) { throw e }` before the generic catch — the canonical structured-
concurrency rule: never swallow CancellationException.

**DTO Int→Long widening**: Several API fields were `Int` but real values exceed
`Int.MAX` (2,147,483,647). Confirmed: Pexels `photographer_id` = 2,151,143,420. Widened
`id`, `photographerId`, `totalResults` in `PexelsDtos.kt`; `views`, `favorites`, `total`
in `WallhavenDtos.kt`; `total` in `UnsplashDtos.kt`; `score`, `ups` in `RedditDtos.kt`.
Width/height/page fields stay `Int` (never overflow).

**Default categories = MINIMAL, AMOLED, NATURE**: User asked for "minimal, oled, amoled,
nature" as defaults. AMOLED (displayName "Dark/AMOLED") covers oled/amoled/dark — there is
no separate OLED entry. Changed `SettingsRepository.selectedCategories` fallback from
`emptySet()` to `DEFAULT_CATEGORIES`. Fallback only fires on fresh install (key absent);
subsequent toggles persist the explicit set.

**Splash screen — dark background + animated icon, no delay**: Changed
`windowSplashScreenBackground` from `#1565C0` to `#0E0E11` (near-black). Added
`splash_icon_anim.xml` (AnimatedVectorDrawable) targeting a new `icon_root` wrapper group
in `ic_launcher_foreground.xml`; plays a 400ms scale 0.88→1.0 + fade-in on entrance.
`windowSplashScreenAnimationDuration` = 400ms. Deliberately no
`setKeepOnScreenCondition` in `MainActivity` — the splash dismisses the instant the first
Compose frame is ready, so launch time is unchanged.

**IzzyOnDroid: dedicated release keystore via GitHub Secrets**: CI previously debug-signed
with an ephemeral keystore (regenerated per run) — IzzyOnDroid would reject updates as
"signature mismatch". Generated a 4096-bit RSA keystore at `~/.android/wallora-release.keystore`
(not committed). Added `build.gradle.kts` env-var fallback so CI can sign via
`SIGNING_KEYSTORE_B64` / `SIGNING_STORE_PASSWORD` / `SIGNING_KEY_ALIAS` secrets.
Workflow decodes keystore to `$RUNNER_TEMP` before build; graceful debug fallback if
secrets absent (forks). Fastlane metadata (`changelogs/1.txt`, screenshots, icon.png)
added under `fastlane/metadata/android/en-US/`. README IzzyOnDroid badge + install section
added; badge links go live after RFP accepted. RFP to be filed by the developer at
https://gitlab.com/IzzyOnDroid/repo/-/issues.

---

## 2026-06-14 (round 2 — vibrant feed + Pixabay source)

**Fast-scroll duplicate-key crash**: Device crash log showed
`IllegalArgumentException: Key "UNSPLASH:IbpngRvnDzo" was already used` in
`LazyVerticalStaggeredGrid`. Root cause: `MultiSourcePagingSource.load()` used
`distinctBy { it.globalKey }` which dedups within a single page load only. A wallpaper
re-appearing on a later page (or from a Room cache hit) produced two grid items sharing
the same key, crashing Compose's staggered-grid measure pass. Fixed by adding a
per-instance `seenKeys: MutableSet<String>` (synchronized) and replacing `distinctBy`
with `filter { seenKeys.add(it.globalKey) }`. The set persists across all `load()` calls
on the same PagingSource instance (which covers one full scroll session) and resets on
refresh when a new instance is created. Regression test added to `DedupTest.kt`.

**Vibrancy overhaul — Pixabay + category redesign**: User flagged the feed as "pale" after
comparing against Pinterest/Pixabay/Unsplash "mobile wallpaper" reference galleries.
Root cause: DEFAULT_CATEGORIES = {MINIMAL, AMOLED, NATURE} are the three muted/dark
categories; the empty-category fallbacks (Pexels getCurated, Unsplash order_by=popular)
pull editorial/desaturated content. Decisions:

1. **Added Pixabay as a 5th source** (`SourceId.PIXABAY`). Pixabay returns inherently
   saturated, wallpaper-friendly photos. API key as query param (no auth interceptor needed).
   Full Hilt multibinding + UserKeyCache + SettingsRepository key flow + SettingsViewModel
   sourceConfiguredMap wiring. isConfigured=false if key absent → silently excluded.

2. **Added three vibrant categories**: VIBRANT (colorful vibrant wallpaper), NEON (neon
   lights glow), GRADIENT (gradient colorful background). Each has queries for all five
   sources including Pixabay.

3. **Reordered Category.entries**: Vibrant categories (VIBRANT, ABSTRACT, NEON, GRADIENT,
   SPACE, AI_ART) lead the chip row. Topical categories (NATURE, LANDSCAPES, CITY, …)
   follow. MINIMAL and AMOLED moved to end — still selectable, just not dominant.

4. **Changed DEFAULT_CATEGORIES** from {MINIMAL, AMOLED, NATURE} to {VIBRANT, ABSTRACT,
   SPACE} — bold/colorful first impression on fresh install.

5. **Widened Wallhaven topRange** from 6M to 1y — more variety in top-rated content while
   still filtering for highest-quality wallpapers.

6. Deliberately NOT wiring `color=`/`colors=` single-hue filters: locking to one color
   kills the variety the user wants (they want "different types", not a single hue wall).

## 2026-06-14

**Per-source category distribution (variety fix)**: User reported that even with vibrant
categories, the feed looked monotonous — all images shared the same dark-background +
colorful-swirl aesthetic. Root cause: all selected categories were passed to EVERY source,
so all 5 sources converged on the highest-scoring "abstract fluid art" look. Fix: in
MultiSourcePagingSource, each source receives `categories[index % categories.size]` — one
category determined by its position. With NATURE/SPACE/CITY/ABSTRACT/ANIMALS as defaults,
round-robin interleave yields nature → space → city → abstract → animal images in sequence.
`buildCacheKey` updated to use per-source `effectiveCategories` so cache keys are
per-source-per-category (not shared). When categories is empty or a search query is active,
all sources fall through to the global categories (unchanged behavior).

**DEFAULT_CATEGORIES changed** from {VIBRANT, ABSTRACT, SPACE} (three similar-aesthetic
vibrant/abstract categories) to {NATURE, SPACE, CITY, ABSTRACT, ANIMALS} — five subjects
that are visually distinct from each other so each grid column shows a genuinely different
subject category, producing the Pinterest-style variety the user wanted.

**PIXABAY_API_KEY added to GitHub Actions secrets** (not committed to source). The release
workflow now writes all four API keys from secrets into local.properties before
assembleRelease, so the GitHub-released APK has all sources working out of the box. New
installs without any keys still work — sources are fail-soft when unconfigured.
