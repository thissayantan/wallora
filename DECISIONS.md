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
