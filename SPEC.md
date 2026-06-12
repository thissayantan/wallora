# Wallora — Android Wallpaper App: Product & Technical Specification

Rename the app freely; "Wallora" is a placeholder. This document is the single source
of truth for requirements. Where it is silent, make a sensible decision and record it
in DECISIONS.md.

## 1. Product summary

A Material You wallpaper app for Android phones and tablets that aggregates free
wallpapers from multiple sources, lets users browse by (multi-select) categories,
search by keyword, set wallpapers directly, auto-rotate wallpapers on a schedule or
on unlock, and skip to the next wallpaper via a home-screen double-tap (live
wallpaper mode), a widget, and a Quick Settings tile.

## 2. Platform & stack (fixed decisions — do not change)

- Language: Kotlin (latest stable). UI: Jetpack Compose + Material 3 with dynamic color.
- minSdk = 31 (Android 12), targetSdk = latest stable, single APK, phone + tablet.
- Architecture: MVVM + Repository, unidirectional data flow, Kotlin Coroutines/Flow.
- DI: Hilt. Network: Retrofit + OkHttp + kotlinx.serialization. Images: Coil.
- Persistence: Room (cache, favorites, history, category selections) + DataStore (settings).
- Lists: Paging 3. Background: WorkManager + AlarmManager. Widget: Glance.
- Build: Gradle Kotlin DSL, version catalog (libs.versions.toml).
- Module layout: single :app module is acceptable; use clear packages
  (data/, domain/, ui/, service/, worker/, widget/). Do not over-modularize.

## 3. Wallpaper sources

Implement a common interface so sources are pluggable:

```kotlin
interface WallpaperSource {
    val id: SourceId                       // PEXELS, WALLHAVEN, REDDIT, UNSPLASH
    val isConfigured: Boolean              // false if API key missing
    suspend fun browse(categories: List<Category>, page: Int): Page<Wallpaper>
    suspend fun search(query: String, page: Int): Page<Wallpaper>
}
```

Normalize everything into one `Wallpaper` model: id, sourceId, thumbUrl, fullUrl,
width, height, author, authorUrl, sourcePageUrl, colorHint?.

Per-source notes:
- Pexels: REST API, key via header. Free tier ~200 req/hr. Use curated + search endpoints.
- Wallhaven: REST API, works without key for SFW content; key optional via local.properties.
  Has native categories and resolution filters — prefer `atleast` resolution matching the
  device screen.
- Reddit: use public listing endpoints (https://www.reddit.com/r/<sub>/hot.json) with a
  proper custom User-Agent. Map categories to subreddits (e.g. nature → r/EarthPorn,
  space → r/spaceporn, minimal → r/MinimalWallpaper, general → r/wallpaper, r/wallpapers,
  amoled → r/Amoledbackgrounds, city → r/CityPorn, animals → r/animalporn alternatives —
  choose SFW wallpaper-suitable subs). Filter to direct image posts (i.redd.it, imgur direct),
  drop galleries/videos/NSFW. Treat Reddit as best-effort: rate-limit politely, fail soft.
- Unsplash: optional source, demo tier 50 req/hr. Must follow attribution rules: show
  photographer name + link, and ping the download endpoint when a wallpaper is applied.
  If no key configured, hide the source with an explanatory row in Settings.

API keys: read from local.properties → BuildConfig (PEXELS_API_KEY, UNSPLASH_ACCESS_KEY,
WALLHAVEN_API_KEY optional). Never commit keys. If a key is missing the source is
disabled gracefully (visible but toggled off with "key missing" hint in Settings).
Create local.properties.example documenting the keys.

Rate limiting & caching: per-source OkHttp interceptors with conservative throttles;
cache pages in Room with a TTL (e.g. 1h) so re-browsing doesn't burn quota; always show
attribution (author + source) on the detail screen.

## 4. Categories & browsing

- Curated category list (enum + per-source query/subreddit mapping): Nature, Landscapes,
  Space, Animals, Technology, Architecture, City, Minimal, Abstract, Dark/AMOLED, Art,
  Cars, Anime (Wallhaven only). Extendable.
- Home screen: staggered/masonry grid of wallpapers, infinite scroll (Paging 3),
  pull-to-refresh. A horizontally scrollable category chip row supports MULTI-SELECT:
  selected categories are OR-combined; results from all enabled sources are interleaved
  (round-robin by source) and de-duplicated by URL.
- Source filter: user can enable/disable sources globally in Settings and quickly via a
  filter sheet on Home.
- Wallpaper detail screen: full-bleed preview, author/source attribution (tappable),
  resolution, actions: Set wallpaper (Home / Lock / Both), Favorite, Download to
  Pictures/Wallora via MediaStore, Share, "More like this" (search by category/tags
  when available).

## 5. Search

- Search bar on Home (Material 3 SearchBar). Keyword search fans out to all enabled
  sources concurrently, merged + paged. Recent searches stored locally (DataStore),
  clearable. Searching a category name behaves like selecting that category.

## 6. Setting wallpapers & Material You

- Use WallpaperManager to set Home, Lock, or Both. Center-crop intelligently to the
  device screen (use the full-resolution image, scale/crop via bitmap region decoding to
  avoid OOM on huge images; respect tablet resolutions).
- Material You: on Android 12+ the system re-derives the dynamic color palette from the
  wallpaper automatically once it is set — no extra work for static mode. The app's own
  UI must use dynamic color (dynamicLightColorScheme/dynamicDarkColorScheme) so it
  re-themes too. In live wallpaper mode, implement onComputeColors() returning
  WallpaperColors.fromBitmap(currentBitmap) and call notifyColorsChanged() on every
  image change so system Material You updates there as well.

## 6.5 Pre-apply wallpaper editor

- From the detail screen, "Edit & set" opens an editor with a live preview and
  sliders: Blur (0–25), Brightness, Contrast, Saturation (ColorMatrix for the last
  three), plus crop/pan repositioning of the visible area. Reset button per slider.
- Blur implementation: do NOT use deprecated RenderScript. Use the RenderScript
  Intrinsics Replacement Toolkit (com.google.android.renderscript / renderscript-
  toolkit) or a downscale → stack blur → upscale pipeline. Preview adjustments on a
  downscaled bitmap for responsiveness; apply to full resolution only on confirm,
  OOM-safely (region decoding / sampled decode as in §6).
- Static mode: edits are baked into the bitmap before WallpaperManager.set.
- Live mode: edit parameters (EditParams: blur, brightness, contrast, saturation,
  pan) are persisted in DataStore and applied at render time, so a user-chosen look
  (e.g. "always slightly blurred") persists across auto-rotations. The editor offers
  "apply to this wallpaper only" vs "set as default look" in live mode.
- Material You note: compute WallpaperColors from the FINAL edited bitmap so the
  system accent matches what is actually on screen.

## 7. Auto-rotation (the rotation engine)

A single `RotationEngine` (domain layer) decides the "next wallpaper" from a user-chosen
playlist: Favorites, or "current category selection + sources". Keep a history table to
avoid repeats until the pool is exhausted. Pre-download the next image on Wi-Fi
(user-toggleable "only on Wi-Fi / only while charging" constraints).

Triggers (all optional, configured in Settings):
1. Interval: WorkManager PeriodicWorkRequest (minimum 15 min; offer 15m/30m/1h/3h/6h/12h/24h).
2. Specific times of day: user picks one or more times; AlarmManager
   setExactAndAllowWhileIdle with SCHEDULE_EXACT_ALARM permission flow (fall back to
   inexact if denied). Re-register alarms on BOOT_COMPLETED.
3. On unlock: supported in LIVE WALLPAPER MODE ONLY (see §8) — the engine swaps the
   image when the device unlocks. In static mode, hide this option with an inline
   explanation ("Switch to live wallpaper mode to enable on-unlock rotation"). Do NOT
   implement a persistent foreground service for unlock detection.

## 8. Live wallpaper mode + skip gestures (Muzei-style)

- Implement a WallpaperService ("Wallora Live") that renders the current wallpaper
  bitmap (simple crossfade animation between images; respect insets and multiple
  displays; handle onSurfaceChanged for rotation/foldables).
- Double-tap on the home screen = next wallpaper (GestureDetector inside the engine).
  Debounce; optional haptic tick. Make the gesture configurable (off / double-tap).
- PARALLAX (toggle in Settings, DEFAULT ON): render the bitmap wider than the screen
  (~1.3–1.5x width, bounded by source image width) and translate horizontally using
  onOffsetsChanged(xOffset, xOffsetStep) as the user swipes between home pages, with
  light smoothing/interpolation so motion isn't jittery. If the launcher never sends
  varying offsets (some report a fixed 0.5 or none), detect this and fall back to a
  centered render without breaking. When parallax is OFF, render center-cropped to
  screen. Static mode: parallax is launcher-controlled and unsupported by modern
  launchers — as best effort, honor desiredMinimumWidth when setting the bitmap, and
  the Settings toggle subtitle must state "Parallax requires live wallpaper mode".
- On-unlock rotation hooks in here (engine visibility + ACTION_USER_PRESENT registered
  at runtime while the engine is alive — allowed because the service is running).
- Implement onComputeColors for Material You (see §6).
- "Skip" must also work WITHOUT live wallpaper: provide
  (a) a Glance home-screen widget with a Next button (+ current thumbnail), and
  (b) a Quick Settings tile "Next wallpaper". Both call the same NextWallpaperUseCase,
  which in static mode sets a new static wallpaper, and in live mode signals the service.

## 9. Tablet / adaptive UI

- Use WindowSizeClass: grid columns scale (compact 2, medium 3, expanded 4–5).
- Expanded width: list-detail two-pane (grid left, selected wallpaper preview right).
- Support landscape properly; wallpaper cropping must account for tablet aspect ratios.

## 10. Settings screen

Sources (toggles + key status), Categories default selection, Rotation (mode, playlist,
interval/times/on-unlock, Wi-Fi-only, charging-only), Gesture toggle, Parallax toggle
(DEFAULT ON; subtitle notes it requires live wallpaper mode), Default look (live-mode
EditParams with reset), Theme (system /
light / dark, dynamic color always on), Cache size + clear cache, About/licenses
(include per-source attribution requirements).

## 11. Permissions & manifest

- INTERNET, SET_WALLPAPER, RECEIVE_BOOT_COMPLETED, SCHEDULE_EXACT_ALARM (with graceful
  fallback), POST_NOTIFICATIONS only if rotation progress notifications are added
  (keep optional). No storage permission needed (MediaStore for downloads, scoped).
- Live wallpaper service declared with android.service.wallpaper metadata + preview.

## 12. Quality bar / definition of done

- `./gradlew assembleDebug` succeeds and `./gradlew testDebugUnitTest` passes — run
  both after EVERY phase; never leave the tree broken.
- Unit tests: source mappers (JSON fixtures per API), RotationEngine (no-repeat logic,
  playlist selection), category→query mapping, repository paging/dedup merge.
- A small number of Compose UI tests for Home grid + chip multi-select.
- ktlint or detekt formatting applied; no compiler warnings introduced knowingly.
- Empty/error/offline states for every screen; Coil placeholders; no jank from
  full-size bitmaps in grids (load thumbs in grid, full only in detail/apply).
- README.md with setup instructions (where to put API keys, how to build).

## 13. Explicit non-goals (v1)

No user accounts, no uploads, no NSFW content (filter it out at every source), no
iOS/desktop, no Play Store packaging work (signing configs etc.), no ads/analytics.
