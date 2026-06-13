# Wallora

**A minimal, Material You wallpaper app for Android.**

Browse, search, and auto-rotate beautiful wallpapers from Pexels, Wallhaven, Unsplash, and Reddit — with a live wallpaper engine, parallax scrolling, a home-screen widget, and a Quick Settings tile.

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](#building)
[![API](https://img.shields.io/badge/API-31%2B-blue)](https://developer.android.com/about/versions/12/android-12)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple)](https://kotlinlang.org)

---

## Features

| | Feature |
|---|---|
| 🖼️ | **Multi-source browsing** — staggered grid from Pexels, Wallhaven, Unsplash, and Reddit |
| 🔍 | **Search** — real-time fan-out search across all enabled sources simultaneously |
| 🏷️ | **Category filters** — Nature, Space, Animals, Abstract, AMOLED, Anime, and more |
| ❤️ | **Favorites & History** — persist your saves and track what you've set |
| 👁️ | **Full-res preview** — hardware-safe decode with per-image error states |
| ✏️ | **Editor** — blur, brightness, contrast, saturation, pan; apply or save as default |
| 🔄 | **Auto-rotation** — interval (WorkManager) + specific times (AlarmManager) + on-unlock |
| 🌊 | **Live wallpaper** — parallax scrolling, crossfade transitions, Material You colours |
| 🪟 | **Home-screen widget** — current wallpaper thumbnail + one-tap "Next" button |
| 🔲 | **Quick Settings tile** — change wallpaper from the notification shade |
| 🎨 | **Material You** — dynamic colour, WallpaperColors from the current bitmap |
| 🌙 | **Theme** — System / Light / Dark |
| ⚡ | **Prefetch cache** — next wallpaper downloaded in the background after each apply |

---

## Screenshots

_Coming soon — contributions welcome!_

---

## Getting Started

### Requirements

- Android **12** (API 31) or higher
- JDK 17
- Android SDK with `platforms;android-35` and `build-tools;35.0.0`

### Quick build

```bash
export JAVA_HOME=$HOME/.local/jdk17      # or wherever your JDK 17 lives
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug
```

Install on a connected device:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Running tests

```bash
./gradlew testDebugUnitTest
```

All unit tests are pure JVM / Robolectric — no device or emulator needed.

---

## API key setup

Wallora works **out of the box** with Wallhaven (SFW) and Reddit — no key required.

For higher-quality sources, copy `local.properties.example` to `local.properties` (already `.gitignore`d) and add your keys:

```properties
sdk.dir=/home/<you>/Android/Sdk

# Pexels — https://www.pexels.com/api/  (free)
PEXELS_API_KEY=your_key_here

# Unsplash — https://unsplash.com/developers  (free)
UNSPLASH_ACCESS_KEY=your_access_key_here

# Wallhaven — https://wallhaven.cc/settings/account  (optional; unlocks NSFW filter)
WALLHAVEN_API_KEY=your_key_here
```

Sources without a key are shown as **disabled** in Settings → Sources. They are still compiled in behind `isConfigured = false` and tested against committed JSON fixtures.

---

## Architecture

```
app/
  data/
    local/       Room: WallpaperEntity, FavoriteEntity, HistoryEntity, RecentSearchEntity
    remote/      Pexels · Wallhaven · Reddit · Unsplash  (Retrofit + kotlinx.serialization)
    paging/      MultiSourcePagingSource (round-robin fan-out + URL dedup)
    repository/  WallpaperRepository, SettingsRepository (DataStore Preferences)
    util/        SafeBitmapDecoder, ImageAdjustments
  domain/
    model/       Wallpaper, Category, SourceId, EditParams
    rotation/    RotationEngine (no-repeat window, playlist selection)
    usecase/     ApplyWallpaperUseCase, NextWallpaperUseCase (prefetch + candidate cache)
  ui/
    home/        HomeScreen + HomeViewModel (staggered grid, category chips)
    search/      SearchScreen + SearchViewModel (fan-out, recent searches)
    detail/      DetailScreen + DetailViewModel (full-res preview, actions)
    editor/      EditorScreen + EditorViewModel (adjustments, live preview)
    favorites/   FavoritesScreen + FavoritesViewModel
    history/     HistoryScreen + HistoryViewModel
    settings/    SettingsScreen + SettingsViewModel
    navigation/  WalloraNavGraph (typed sealed routes, bottom nav)
    theme/       WalloraTheme (dynamic colour, edge-to-edge)
  service/
    WalloraWallpaperService   Live wallpaper engine (parallax, crossfade)
    WalloraQsTileService      Quick Settings tile
    helpers/                  ParallaxMath, CrossfadeAnimator, CropCalculator
  widget/
    WalloraWidget             Glance app widget + NextWallpaperAction
  worker/
    RotationWorker            WorkManager periodic interval rotation
    AlarmScheduler            AlarmManager exact/inexact alarm scheduling
    BootReceiver              Re-registers alarms on BOOT_COMPLETED
```

**Key decisions:**
- Custom fan-out `PagingSource` (not `RemoteMediator`) for multi-source interleave
- Non-RenderScript blur: downscale → iterative box blur → upscale
- Parallax via over-wide bitmap (1.3×) + `onOffsetsChanged` translation
- `allowHardware(false)` in detail preview to sidestep GPU texture-size overflow on high-res images
- Baseline Profile deferred (requires device/managed emulator); see `DECISIONS.md`

---

## Gesture & shortcut binding

### Next wallpaper — fastest ways

| Method | How |
|---|---|
| **App shortcut** | Long-press the Wallora icon → **Next wallpaper** |
| **Nova Launcher gesture** | Nova Settings → Gestures & inputs → *your gesture* → App shortcuts → Wallora → Next wallpaper |
| **Tasker / automation** | Send intent `com.wallora.app.action.NEXT_WALLPAPER` to `com.wallora.app.ui.NextWallpaperActivity` |
| **Quick Settings tile** | Add "Next wallpaper" tile from notification shade edit mode |

### Double-tap (live wallpaper mode)

Off by default — most launchers consume double-tap for their own gestures. Enable in Settings → Live Wallpaper & Gestures only if your launcher passes it through.

### Parallax (Nova Launcher 8)

Nova Settings → **Desktop** → enable **Scroll wallpaper**.

---

## Contributing

Pull requests are welcome! Please:

1. Fork the repo and create a feature branch
2. Follow the existing Kotlin style (official Kotlin coding conventions)
3. Add or update unit tests for logic changes
4. Run `./gradlew testDebugUnitTest lintDebug` before opening a PR
5. Open a PR describing what changed and why

For bug reports, please include your device model, Android version, and steps to reproduce.

---

## License

```
MIT License

Copyright (c) 2026 Sayantan Dey

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

Photo content is served from third-party APIs per each source's license. See Settings → About for per-source attribution.
