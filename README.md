# Positional

![banner](screenshots/banner.png)

A location, compass, clock, level, trail and measurement toolkit for Android. Community fork distributed only through GitHub Releases; not published on Google Play.

[![Build](https://github.com/kusl/hamza417-positional/actions/workflows/build.yml/badge.svg)](https://github.com/kusl/hamza417-positional/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/kusl/hamza417-positional)](https://github.com/kusl/hamza417-positional/releases/latest)
![API](https://img.shields.io/badge/API-24%2B-138D75?logo=android)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

## Features

- **GPS**: coordinates (DD, DM, DMS, UTM), altitude, speed, accuracy, address, map with custom pins.
- **Compass**: magnetic azimuth, skins, calibration help.
- **Clock**: local and time zone time, sun and moon position, rise, set and twilight times.
- **Level**: bubble level from the accelerometer.
- **Trail**: record and review paths with markers.
- **Measure** and **Direction**: distances between points and bearings to targets.
- Home screen widgets for clock, sun time, moon phase and twilight.

## Install

1. Download the latest APK from [Releases](https://github.com/kusl/hamza417-positional/releases/latest), or
2. Add `https://github.com/kusl/hamza417-positional` to [Obtainium](https://github.com/ImranR98/Obtainium) for automatic updates.

Android 7.0 (API 24) or newer. Silent background updates through Obtainium need Android 12 or newer.

Releases are signed with this fork's own key and cannot be installed over the Play Store build; uninstall that first.

## Support

Report bugs and requests in this repository's [issues](https://github.com/kusl/hamza417-positional/issues). Do not contact the original author about this fork.

## Differences from upstream

- No Google Play licensing, update or rating prompts.
- No links to upstream chat groups, developer profiles or other apps.
- UI is being migrated to Jetpack Compose (see below).
- Release builds come from CI on every push to `master`.
- Legal pages rewritten for this fork; no license checks, analytics or crash reporting.
- Dead code, unused resources and source comments removed; unit tests for math, utilities, launcher icons, settings labels, accent palette, coordinate input, help pages, arts catalog, address search, time zone catalog, trail name rules, needle physics, tilt, clock motion, sensor accuracy, geodesic distance and path length, distance units, the speedometer dial and a check that every drawable drawn by Compose is a vector or bitmap.

The map panels use the Google Maps SDK and Google Play services location. Without a Maps API key the map tiles stay blank and every other panel works.

## Compose migration

New UI is written in Jetpack Compose (Material 3, Compose BOM) and hosted in the existing fragments and activities through `ComposeView` (`setPositionalContent` for full-screen activities). Shared theme, components, dialogs and screens live in `ui/compose/`. Saved lists observe Room through `Flow` and write with `NonCancellable` so edits survive closing a screen. Sensor panels register sensors only while resumed (at game rate, not fastest), animate needles only until they settle, and pause the clock with the frame clock when the app is in the background. Map panels share `MapPanelScaffold` (toolbar, tools, bottom sheet handle, landscape side column) and follow the view lifecycle through `MapLifecycle`; the GPS panel reverse geocodes with the non-blocking `Geocoder` API on Android 13+ and only when the position moved at least 25 m.

| Phase | Scope | Status |
|---|---|---|
| 1 | Compose toolchain, theme, preference components, Settings screen | Done |
| 2 | Fork-owned legal pages, upstream contacts and unused resources removed | Done |
| 3 | Settings dialogs (theme, icon, corners, units, coordinates, provider, language) | Done |
| 4 | Accent colors and custom locations screens, Room `Flow` for saved places | Done |
| 5 | Arts pager, help and legal page viewer, map search with debounced geocoding; unused target address screen removed | Done |
| 6 | Trails, measures, directions and time zone screens; Room `Flow` and singleton databases; search view and list adapters removed | Done |
| 7 | Level, compass, clock and direction panels in Compose; settle-aware needle physics, lifecycle-bound sensors, sun diagram on `Canvas` | Done |
| 8a | GPS map panel in Compose: toolbar, map tools, info sheet, speedometer; shared map lifecycle observer; address lookups only after moving 25 m | Done |
| 8b | Trail and measure map panels in Compose on a shared map scaffold; trail points observed through Room `Flow` with one open database per trail; popup windows, tool views and the timeline library removed | Done |
| 8c | Remaining dialogs, single activity with Navigation Compose, drop AppCompat themes | Next |
| 9 | Widgets on Glance | Planned |
| 10 | OpenStreetMap fallback with tile caching when the Maps key is missing or over quota | Planned |

## Building

Requirements: JDK 21, Android SDK (compile and target SDK 37), Android Gradle Plugin 9.2+, Kotlin 2.4 with the Compose compiler plugin.

```sh
./gradlew testDebugUnitTest lintVitalRelease assembleDebug
```

Optional `local.properties` keys (environment variables of the same name take precedence):

| Key | Purpose |
|---|---|
| `MAPS_API_KEY` | Google Maps API key |
| `KEYSTORE_FILE` | Path to the release keystore |
| `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | Release signing |
| `VERSION_CODE`, `VERSION_NAME` | Override the app version |

`./gradlew assembleRelease` produces an unsigned APK when no keystore is configured.

## CI and releases

`.github/workflows/build.yml` runs on every push and pull request:

1. Unit tests, release lint checks (the same fatal checks that gate `assembleRelease`) and a debug build.
2. On pushes to `master` only: a signed release APK, published as a full GitHub release tagged `v1.0.<run number>`.

Required secrets: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS`. Optional: `MAPS_API_KEY`.

```sh
base64 -w0 release.keystore
```

Dependabot opens weekly update PRs for Gradle dependencies, the Gradle wrapper and GitHub Actions. If a PR fails at `checkDebugAarMetadata`, raise `compileSdk` first.

## Project layout

```
app/src/main/java/app/simple/positional/
  activities/   entry points and sub screens
  constants/    static catalogs (help pages, pins, launcher art)
  ui/compose/   Compose theme, shared components, dialogs and screens
  sensors/      lifecycle-aware gravity and heading sensors
  ui/           panel fragments
  decorations/  legacy custom views, map wrappers, fast scroller
  database/     Room entities, DAOs and databases
  services/     location and clock widget services
  widgets/      app widget providers
  math/         vectors, quaternions, angles, unit conversion
  util/         formatting and Android helpers
app/src/test/   JVM unit tests
docs/llm/       LLM context files (tracked, excluded from the dump)
```

`export.sh` writes `docs/llm/dump.txt` for LLM context: every tracked file except `docs/llm/`, with contents of source, build and config files only.

## Credits and license

Based on Positional by Hamza Rizwan. Licensed under the [GNU GPL v3](LICENSE). See [PRIVACY.md](PRIVACY.md).
