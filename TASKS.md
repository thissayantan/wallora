# TASKS.md — Wallora build plan (refined)

Rules: execute top to bottom. Check off each task immediately when done. Build after
every task; run unit tests after every phase. See CLAUDE.md for autonomy rules.

## Phase 0 — Project bootstrap

- [x] P0-a: JDK 17 — download Eclipse Temurin 17 Linux x64 tarball → ~/.local/jdk17;
      export JAVA_HOME+PATH for session; persist to ~/.bashrc, ~/.zshrc, and
      gradle.properties (org.gradle.java.home). Record version in DECISIONS.md.
- [x] P0-b: Android SDK — download commandlinetools-linux zip → ~/Android/Sdk/cmdline-tools/latest;
      `yes | sdkmanager --licenses`; install platform-tools, platforms;android-35,
      build-tools;35.0.0. Set ANDROID_HOME, write local.properties sdk.dir.
      Record versions in DECISIONS.md.
- [x] P0-c: Gradle wrapper — download Gradle 8.9 binary zip → ~/.local/gradle8;
      generate gradlew + wrapper jar in the project root. Verify:
      `sdkmanager --list_installed`, `./gradlew --version`. Commit DECISIONS.md.
- [x] P0-d: Create Android project structure — Kotlin DSL, Compose, Material 3,
      minSdk 31, targetSdk/compileSdk 35, version catalog (libs.versions.toml),
      Hilt, package layout data/domain/ui/service/worker/widget. `git init` + first commit.
- [x] P0-e: local.properties.example + BuildConfig wiring for PEXELS_API_KEY,
      UNSPLASH_ACCESS_KEY, WALLHAVEN_API_KEY — empty-safe defaults (""). Commit.
- [x] P0-f: App theme — dynamic color (dynamicLightColorScheme / dynamicDarkColorScheme
      on SDK 31+), dark/light support, edge-to-edge WindowInsets. Commit.
- [x] P0-g: Quality tooling — ktlint via gradle-lint-plugin or detekt wired into
      build; also add SafeBitmapDecoder utility stub (region + sampled decode,
      OOM-safe) that will be used by apply pipeline and editor. Commit.

## Phase 1 — Data layer & sources

- [x] P1-a: Core domain models: Wallpaper, Category (enum + subreddit/query mapping),
      SourceId, Page<T>, EditParams. Commit.
- [x] P1-b: Room database: WallpaperEntity (cache TTL), FavoriteEntity, HistoryEntity,
      RecentSearchEntity + DAOs + Database class. DataStore SettingsRepository
      (sources enabled/disabled, category defaults, rotation prefs). Commit.
- [x] P1-c: Retrofit / OkHttp stack: logging interceptor, per-source throttling
      interceptor (token-bucket or simple delay), kotlinx.serialization converter,
      Result<T> error wrapper. Commit.
- [x] P1-d: WallpaperSource interface. Pexels implementation: curated + search
      endpoints, category mapping, API-key header. JSON-fixture unit tests for
      Pexels mapper (pexels_curated.json fixture committed). Commit.
- [x] P1-e: Wallhaven implementation: categories, atleast-resolution filter, SFW-only,
      optional API key. JSON-fixture unit tests (wallhaven_search.json fixture). Commit.
- [x] P1-f: Reddit implementation: subreddit mapping per SPEC §3, listing .json
      endpoint, custom User-Agent, direct-image predicate (i.redd.it / imgur direct),
      NSFW filter, gallery/video drop, pagination cursor. JSON-fixture unit tests
      (reddit_hot.json hand-authored fixture). Fail-soft at runtime. Commit.
- [x] P1-g: Unsplash implementation: optional source, attribution fields, download-ping
      hook on apply, hidden when key missing. JSON-fixture unit tests. Commit.
- [x] P1-h: WallpaperRepository (a): Room TTL page cache — insert/expire logic,
      cache-hit path. Commit.
- [x] P1-i: WallpaperRepository (b): multi-source fan-out PagingSource —
      round-robin interleave across enabled sources, URL dedup (bloom/set),
      per-source fail-soft. Unit tests for merge/dedup. Commit.
- [x] P1-phase-test: `./gradlew testDebugUnitTest` — all Phase 1 unit tests pass.

## Phase 2 — Browsing UI

- [x] P2-a: Home screen: staggered/masonry LazyVerticalStaggeredGrid with Paging 3,
      horizontally scrollable multi-select category chip row (OR-combine), source
      filter bottom sheet, pull-to-refresh, empty/error/offline states, Coil thumb
      loading with placeholder/error. Commit.
- [x] P2-b: Search screen: Material 3 SearchBar, concurrent fan-out search across
      enabled sources, merged paged results, recent searches (DataStore) with clear,
      category-name search triggers category selection. Commit.
- [x] P2-c: Detail screen: full-bleed preview (Coil, full-res), author + source
      attribution (tappable links), resolution display, Set Wallpaper (Home/Lock/Both)
      action, Favorite toggle, Share intent, Download via MediaStore, "More like this"
      (search by category/tags). Commit.
- [x] P2-d: Favorites screen + History screen — simple LazyVerticalGrid grids,
      reuse thumb loading. Minimal Settings screen scaffold (empty placeholder nav
      destination). Commit.
- [x] P2-e: Adaptive layouts: WindowSizeClass-based column scaling (compact 2 / medium 3
      / expanded 4–5). Expanded: NavigableListDetailPaneScaffold two-pane. Landscape
      support. Compose UI tests for Home grid + chip multi-select (Robolectric). Commit.
- [x] P2-phase-test: `./gradlew testDebugUnitTest` — Compose UI tests pass.

## Phase 3 — Setting wallpapers & editor

- [x] P3-a: Apply pipeline — full-res download → SafeBitmapDecoder (region/sampled
      OOM-safe decode to device screen size, center-crop) → WallpaperManager set
      Home/Lock/Both; Unsplash download-ping on apply; success/failure snackbars;
      progress indicator. Commit.
- [x] P3-b: Adjustment pipeline — pure EditParams (blur, brightness, contrast,
      saturation, crop offset); ColorMatrix math for b/c/s; non-RenderScript blur
      (downscale→stack-blur→upscale or renderscript-toolkit); unit tests for
      ColorMatrix math and blur radius bounds. Commit.
- [x] P3-c: Editor UI — edit screen from detail, live downscaled preview + sliders,
      Reset per slider, crop/pan gesture, full-res apply on confirm; live-mode:
      "apply to this wallpaper only" vs "set as default look" toggle, persisted via
      DataStore EditParams; Material You: WallpaperColors from final edited bitmap. Commit.
- [x] P3-d: RotationEngine + NextWallpaperUseCase — playlist (Favorites or current
      category+sources), history no-repeat table, pre-fetch next on Wi-Fi (toggle).
      Unit tests: no-repeat exhaustion loop, playlist selection. Commit.
- [x] P3-phase-test: `./gradlew testDebugUnitTest`.

## Phase 4 — Auto-rotation triggers

- [ ] P4-a: Interval rotation — WorkManager PeriodicWorkRequest (15m minimum;
      offer 15m/30m/1h/3h/6h/12h/24h), Wi-Fi-only and charging-only constraints,
      Settings UI rows for interval + constraints. Commit.
- [ ] P4-b: AlarmScheduleCalculator — pure helper: given a list of time-of-day pairs
      and current instant, return next trigger instant. Unit tests. Commit.
- [ ] P4-c: Exact alarm trigger — AlarmManager setExactAndAllowWhileIdle behind
      canScheduleExactAlarms() check; inexact fallback; BOOT_COMPLETED BroadcastReceiver
      re-registers alarms + periodic work; SCHEDULE_EXACT_ALARM permission declared in
      manifest with graceful flow. Commit.
- [ ] P4-d: Rotation settings wiring — Settings rows for mode/playlist/times/constraints;
      "next change" status display; "on unlock" option hidden in static mode with
      inline explanation. Commit.
- [ ] P4-phase-test: `./gradlew testDebugUnitTest`.

## Phase 5 — Live wallpaper mode & skip controls

- [ ] P5-a: WallpaperService Engine shell — barebones WallpaperService + Engine,
      Android manifest declaration with preview metadata, battery-safe (draw only on
      change, not continuous loop). Commit.
- [ ] P5-b: Pure helpers + tests: ParallaxMath (xOffset→pixel translation, over-wide
      bounds, fixed-offset fallback), CrossfadeAnimator (alpha interpolation over
      time), CropCalculator (center-crop / desiredMinimumWidth). Unit tests. Commit.
- [ ] P5-c: Double-tap gesture → next wallpaper inside Engine (GestureDetector,
      debounced, configurable off/double-tap via Settings). Commit.
- [ ] P5-d: Parallax render + EditParams at render time — over-wide bitmap render
      (1.3–1.5x), onOffsetsChanged horizontal translation with smoothing, fixed-offset
      launcher fallback, persisted EditParams (blur/brightness/etc.) applied at render
      time; Settings toggle DEFAULT ON. Commit.
- [ ] P5-e: On-unlock rotation — runtime ACTION_USER_PRESENT receiver registered while
      Engine is alive (visible), calls NextWallpaperUseCase; Settings option shown only
      when live wallpaper mode. Commit.
- [ ] P5-f: onComputeColors + notifyColorsChanged for Material You — derive
      WallpaperColors from current rendered bitmap (EditParams applied). Commit.
- [ ] P5-g: Glance widget (thumbnail + Next button) calling NextWallpaperUseCase;
      Quick Settings tile ("Next wallpaper") calling same use case; "Set as live
      wallpaper" entry point in Settings. Commit.
- [ ] P5-phase-test: `./gradlew testDebugUnitTest`.

## Phase 6 — Settings, polish, finish

- [ ] P6-a: Full Settings screen per SPEC §10: Sources (toggles + key status),
      Categories default, Rotation (mode/playlist/interval/times/on-unlock/Wi-Fi/
      charging), Gesture toggle, Parallax toggle (DEFAULT ON, subtitle), Default look
      EditParams (live mode), Theme (system/light/dark), Cache size + clear,
      About/Licenses (per-source attribution). Commit.
- [ ] P6-b: Adaptive/themed app icon (adaptive icon with foreground + background,
      monochrome variant), splash screen API (SplashScreen compat). Commit.
- [ ] P6-c: Performance pass — Coil memory/disk cache tuning, confirm thumbs-only in
      grids / full-res only in detail+apply, fix any empty/error states missed earlier,
      no jank audit. Commit.
- [ ] P6-d: Final verification — `./gradlew assembleDebug` + `./gradlew testDebugUnitTest`
      both green; lint reasonably clean; README.md (API key setup, how to build,
      screenshots TODO); review DECISIONS.md for completeness. Final commit.
