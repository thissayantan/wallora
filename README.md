# Wallora

A Material You Android wallpaper app that browses, searches, and auto-rotates
wallpapers from multiple sources — Pexels, Wallhaven, Unsplash, and Reddit.

---

## Features

| Feature | Description |
|---------|-------------|
| **Multi-source browsing** | Staggered grid from Pexels, Wallhaven, Unsplash, Reddit |
| **Category filtering** | Nature, Space, Animals, Abstract, AMOLED, Anime, and more |
| **Search** | Real-time fan-out search across all enabled sources |
| **Favorites & History** | Persistent favorites + set history tracked in Room |
| **Detail view** | Full-res preview, author attribution, resolution, Set/Share/Download |
| **Editor** | Blur, brightness, contrast, saturation, pan; live preview; apply or save as default |
| **Auto-rotation** | Interval (WorkManager) + specific times (AlarmManager) + on-unlock |
| **Live wallpaper** | Double-tap for next, parallax scrolling, crossfade transitions |
| **Glance widget** | Home screen widget showing current wallpaper + "Next" button |
| **Quick Settings tile** | One-tap "Next wallpaper" from notification shade |
| **Material You** | Dynamic colour, WallpaperColors from current bitmap |
| **Theme** | System / Light / Dark |

---

## Building

### Prerequisites

- **JDK 17** at `~/.local/jdk17` (or update `JAVA_HOME`)
- **Android SDK** with `platforms;android-35` and `build-tools;35.0.0`
- A `local.properties` file at the project root (see below)

### Quick build

```bash
export JAVA_HOME=$HOME/.local/jdk17
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

### Running tests

```bash
./gradlew testDebugUnitTest
```

All unit tests are pure JVM or Robolectric — no emulator needed.

---

## API key setup

Copy `local.properties.example` to `local.properties` (already git-ignored) and fill
in your keys:

```properties
sdk.dir=/home/<you>/Android/Sdk

# Pexels — https://www.pexels.com/api/
PEXELS_API_KEY=your_key_here

# Unsplash — https://unsplash.com/developers
UNSPLASH_ACCESS_KEY=your_access_key_here

# Wallhaven — https://wallhaven.cc/settings/account (optional, enables higher-res)
WALLHAVEN_API_KEY=your_key_here
```

**Wallhaven works without a key** (SFW only). Reddit works without any key.
Sources without a key are shown as disabled in Settings > Sources; they are still
built into the app behind `isConfigured = false` and backed by JSON-fixture unit tests.

---

## Architecture

```
app/
  data/
    local/          Room: WallpaperEntity, FavoriteEntity, HistoryEntity, RecentSearchEntity
    remote/         Pexels, Wallhaven, Reddit, Unsplash — Retrofit + kotlinx.serialization
    paging/         MultiSourcePagingSource (round-robin fan-out + URL dedup)
    repository/     WallpaperRepository, SettingsRepository (DataStore)
    util/           SafeBitmapDecoder, ImageAdjustments
  domain/
    model/          Wallpaper, Category, SourceId, EditParams
    rotation/       RotationEngine (no-repeat window, playlist selection)
    usecase/        ApplyWallpaperUseCase, NextWallpaperUseCase
  ui/
    home/           HomeScreen + HomeViewModel (staggered grid, category chips)
    search/         SearchScreen + SearchViewModel (fan-out, recent searches)
    detail/         DetailScreen + DetailViewModel (full-res preview, actions)
    editor/         EditorScreen + EditorViewModel (adjustments, live preview)
    favorites/      FavoritesScreen + FavoritesViewModel
    history/        HistoryScreen + HistoryViewModel
    settings/       SettingsScreen + SettingsViewModel (all SPEC §10 sections)
    navigation/     WalloraNavGraph (typed sealed routes, bottom nav)
    theme/          WalloraTheme (dynamic color, edge-to-edge)
  service/
    WalloraWallpaperService   Live wallpaper engine (parallax, crossfade, double-tap)
    WalloraQsTileService      Quick Settings tile
    helpers/                  ParallaxMath, CrossfadeAnimator, CropCalculator
  widget/
    WalloraWidget             Glance app widget
  worker/
    RotationWorker            WorkManager periodic interval rotation
    AlarmScheduler            AlarmManager exact/inexact alarm scheduling
    AlarmScheduleCalculator   Pure helper: next trigger from HH:mm set
    BootReceiver               Re-registers alarms + work on BOOT_COMPLETED
    RotationAlarmReceiver      Alarm fires → next wallpaper → re-chain
```

### Key decisions

See `DECISIONS.md` for full rationale. Highlights:

- **Custom PagingSource** (not RemoteMediator) for multi-source fan-out. Room is a side
  cache, not the Paging backing store.
- **Non-RenderScript blur**: downscale → iterative box blur → upscale. No RS dependency.
- **Parallax**: over-wide bitmap (1.3×), `onOffsetsChanged` → pixel translation;
  `PARALLAX_SCALE = 1.3f`.
- **Reddit**: source is fully implemented and unit-tested against a committed
  `reddit_hot.json` fixture. On-device fetch may be blocked from datacenter IPs (HTTP
  403); this is expected and fails soft without blanking the grid.
- **Exact alarms**: `setExactAndAllowWhileIdle` behind `canScheduleExactAlarms()`;
  inexact fallback via `setAndAllowWhileIdle`.

---

## Screenshots

_TODO: add screenshots once running on a device or emulator._

---

## License

Source code: MIT.  
Photo content is served from third-party APIs per each source's license.  
See Settings > About for per-source attribution.
