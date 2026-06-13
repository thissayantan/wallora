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
