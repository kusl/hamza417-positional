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
- Home screen widgets: analog clock (Android 12+, ticked by the launcher with no background service), sun time, moon phase and twilight (Glance, hourly refresh).

## Install

1. Download the latest APK from [Releases](https://github.com/kusl/hamza417-positional/releases/latest), or
2. Add `https://github.com/kusl/hamza417-positional` to [Obtainium](https://github.com/ImranR98/Obtainium) for automatic updates.

Android 7.0 (API 24) or newer. Clock widgets and silent background updates through Obtainium need Android 12 or newer.

Releases are signed with this fork's own key and cannot be installed over the Play Store build; uninstall that first.

## Support

Report bugs and requests in this repository's [issues](https://github.com/kusl/hamza417-positional/issues). Do not contact the original author about this fork.

## Differences from upstream

- No Google Play licensing, update or rating prompts.
- No links to upstream chat groups, developer profiles or other apps.
- UI is being migrated to Jetpack Compose (see below).
- Release builds come from CI on every push to `master`.
- Legal pages rewritten for this fork; no license checks, analytics or crash reporting.
- Dead code, unused resources and source comments removed; unit tests for menu and dialog state, panel catalog, panel settings (map rotation, bloom, needle and pin fallbacks), location pin colours, map preference bindings, series statistics and sample windows, fix latency, location sharing, math, utilities, launcher icons, settings labels, accent palette, coordinate input, help pages, arts catalog, launcher art path data, address search, time zone catalog, trail name rules, search result encoding, sun and moon refresh distance, settings theme and corner values, location provider choice, map source choice, Google map type and style, OpenStreetMap style and geometry, tracking pins, map animation interpolation, delayed map tasks, measure storage, widget day and night changes, needle physics, tilt, clock motion, sensor accuracy, twilight times, geodesic distance and path length, distance units, the speedometer dial, launcher art frame sizing and a check that every drawable drawn by Compose is a vector or bitmap.

Every map (GPS, trail and measure panels and address search) can show either Google Maps or OpenStreetMap (Settings > Map source). Automatic uses OpenStreetMap when the build has no Maps API key and Google Maps otherwise. The SDK gives no signal when a key is rejected or over quota, so if the Google map stays blank pick OpenStreetMap. OpenStreetMap is drawn with MapLibre Native from `tile.openstreetmap.org`, keeps up to 128 MB of tiles in MapLibre's cache, and darkens tiles in night mode. Google-only options (satellite, labels, traffic, buildings, high contrast, geodesic trails) have no effect on it; traffic is drawn only on the GPS map. With compass rotation the map follows your position only when the pin leaves the screen, so the heading keeps turning the map. With OpenStreetMap the GPS panel only asks for Google Play services when the fused location provider is selected. Location uses Google Play services or the Android provider as set in Settings. Both are cold flows (`LocationSource`) collected by `LocationViewModel` only while a panel that shows location is started (they stop 5 s after the last one stops, so the level and settings panels and the background use no location), request one fix per second, and fall back from GPS to network to passive as providers are switched; with approximate location only, the Android provider uses the network. The last position, used by widgets and as a map starting point, is saved only after moving 50 m.

## Compose migration

New UI is written in Jetpack Compose (Material 3, Compose BOM) and hosted in `BaseActivity` subclasses through `setPositionalContent`, which takes the top inset from the window insets; no AppCompat, Material Components or fragments are used. Panels are `PanelController` classes (no fragments): each owns a child lifecycle that follows the activity while the panel is shown and is destroyed when another panel is picked, so sensors, maps, location observers and coroutines stop with it. Shared theme, components, dialogs and screens live in `ui/compose/`. A panel shows a dialog by putting a `PanelDialog` value in its `DialogHost` and draws it in `DialogContent`. View models expose `StateFlow` (no `LiveData`); preferences reach panels as view model `StateFlow`s built with `preference`, which panels apply with `bind` while started; map views follow their own settings through `MapPreferences` flows collected while resumed (in a child scope cancelled on pause that also runs their delayed tasks), applying only values that changed (including changes made while paused), so no panel, view model or map view registers a preference listener. Text a panel shows (rows, summaries, addresses) is built by its view model, created by `PanelController.panelModel` with the shared `LocationViewModel`, and collected by the screen with `collectAsStateWithLifecycle`; panel controllers keep only sensors, maps, needle animation, preference toggles and actions, and use `PanelController.observe` for map updates. View model flows stop 5 s after the last screen stops collecting, so location, the GPS target summary and address lookups run only while a panel that shows them is visible. Saved lists and the selected measure observe Room through `Flow` and write with `NonCancellable` so edits survive closing a screen. Sensor panels register sensors only while resumed (at game rate, not fastest), animate needles only until they settle, and pause the clock with the frame clock when the app is in the background. Map panels share `MapPanelScaffold` (toolbar, tools, bottom sheet handle, landscape side column) and the map views (`GoogleMapView` and `OsmMapView` bases) follow the panel lifecycle and share pin, accuracy circle, line, heading sensor and camera code; each trail or measure is one line updated in place; address lookups and map search share `Geocoding` (the non-blocking `Geocoder` API on Android 13+), and the GPS panel reverse geocodes only when the position moved at least 25 m. Heading and level sensors read the display rotation from `ContextCompat.getDisplayOrDefault`. A custom location or direction target is stored in one preference edit, so flows never see a half-written location.

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
| 8c | Panel menus (GPS, trail, measure, compass, clock, direction) as one Compose bottom sheet built from `PanelMenus`; sub dialogs no longer reopen menus while being destroyed | Done |
| 8d | Error, clock motion, new measure, new trail and new marker dialogs in Compose through a shared `DialogHost`; marker icons picked inline; popup marker grid removed | Done |
| 8e | Clock needle, bloom skin, pin, physical properties, calibration and target coordinates dialogs in Compose; calibration reads accuracy from the panel sensors instead of registering its own; unused permission dialog, switch view and pager adapters removed | Done |
| 8f | Location summary, location details (Compose `Canvas` sparklines) and coordinate formats dialogs in Compose; coordinate formats computed only while the dialog is open instead of on every fix; sparkline, fast scroller and recycler view decorations removed | Done |
| 8g | `MainActivity` in Compose: panels hosted with `AndroidFragment`, floating panel switcher (tap for a grid, swipe to step) replaces the view pager bottom bar and panel dialog; permission request through the Activity Result API; all dialog fragments and bottom sheet styles removed | Done |
| 8h | Panels as plain `PanelController` classes with their own child lifecycle; panel fragments, `ScopedFragment` and the fragment Compose host removed | Done |
| 8i | AppCompat, Material Components, fragments, ConstraintLayout and legacy support libraries removed: activities extend `ComponentActivity` with `enableEdgeToEdge`, day/night is applied through a configuration override (`NightMode`), splash and clock widget setup are Compose screens | Done |
| 8j | Sub screens (accent colours, arts, time zones, saved measures, trails and directions, custom locations, map search, help pages) are Navigation Compose destinations inside `MainActivity`; the panel lifecycle follows its destination so sensors and maps pause while a sub screen is open; nine activities removed | Done |
| 9a | Moon phase and sun time widgets on Glance (same receivers, so placed widgets keep working); location updates delivered through an in-process `LocationBus` instead of the deprecated `LocalBroadcastManager` | Done |
| 9b | Twilight and sun time art widgets on Glance with shared widget parts; twilight times computed once in `Twilight` for the clock panel and widget; twilight and sun art widgets refresh hourly; info section graphics no longer overlap text | Done |
| 9c | Clock widgets on the platform `AnalogClock` (Android 12+): hands rendered once with the chosen skin and accent on the 12-hour face, ticked by the launcher; the widgets refresh when the app is left so skin and accent changes apply; date from `TextClock`; day and night icon refreshed by a non-waking alarm at 6:00 and 19:00. Foreground service, notification and battery optimization prompts removed. Text without an explicit style no longer inherits a fixed line height, so large wrapped headings do not overlap | Done |
| 10a | Map source setting (automatic, Google Maps, OpenStreetMap); GPS panel on MapLibre with OpenStreetMap tiles, tile cache, pin, accuracy circle, target line, follow, compass and bearing rotation behind a `GpsMap` interface; pin bitmaps shared by both maps; APK limited to arm64-v8a, armeabi-v7a and x86_64 | Done |
| 10b | Trail and measure panels on OpenStreetMap through `TrailMap` and `MeasureMap` interfaces on a shared `OsmMapView` base (camera, heading sensor, pin, accuracy circle, lines, trail markers, measure guide line, wrap to fit); trail auto center stops while paused instead of re-posting forever; tracking pins shared with the Google maps; unused measure label overlay removed | Done |
| 10c | Address search map on OpenStreetMap through a `PickerMap` interface; map views share an `OsmMapView` base and a `MapHost` lifecycle contract; Google Play services prompt only when Google Maps or the fused provider is in use | Done |
| 11 | All code in Kotlin: Room entities keep their column names and nullability so existing databases open unchanged (CI fails if an exported schema hash changes); map callbacks trimmed to the events panels use; Google marker, circle and line animations as extensions on shared interpolation (accuracy circles no longer wrap radii above 360 m like angles); stale schema exports removed | Done |
| 12 | View models on `StateFlow`; `lifecycle-livedata` removed; Google GPS, trail, measure and picker maps on a shared `GoogleMapView` base mirroring OpenStreetMap; target start point stored when a target is set instead of on every fix; map bearing not saved on every compass frame; Google trail auto center stops while paused | Done |
| 13 | Location providers as lifecycle-bound flows (`AndroidLocationSource`, `FusedLocationSource`) replacing the started services and `LocationBus`; fused updates every second instead of every 100 ms; the Android provider registers once instead of re-registering every second and switches provider on `PROVIDERS_CHANGED`; approximate location permission is honoured; last position saved in one write after moving 50 m instead of three writes per fix | Done |
| 14a | Panel dialogs as state (`PanelDialog` values rendered by each panel) instead of stored composables; map search result returned through the previous back stack entry's `SavedStateHandle`, so it survives configuration changes; sub screens collect with `collectAsStateWithLifecycle`; clock second hand drawn at full size again; empty trail art scaled to width | Done |
| 14b | Panel info in view models (`ClockViewModel`, `CompassViewModel`, `DirectionViewModel`, `GpsViewModel`, trail and measure summaries) built on `Dispatchers.Default` and collected with `collectAsStateWithLifecycle`, so it survives rotation and panel switches; GPS address lookup moved into the view model; sun and moon report recomputed once a minute or after moving 1 km instead of on every fix; the clock no longer turns on location with a custom location; view models react to unit, coordinate format and clock format changes through `SharedPreferences.changes`; localized context cached per language | Done |
| 15 | Settings and level panel state in view models (`SettingsViewModel`, `LevelViewModel`) built from `SharedPreferences.changes` and collected with `collectAsStateWithLifecycle`; preference writes moved out of the panel controllers, which keep only permission requests, activity recreation, window flags and navigation; the settings screen no longer rebuilds its state (and queries every launcher alias) on unrelated preference changes | Done |
| 16 | Compass, clock, direction, GPS, trail and measure panel settings as view model `StateFlow`s built with `preference` (`SharedPreferences.watch`, only distinct values); panels apply them with `PanelController.bind` while started; panels and view models no longer register preference listeners; clock time zone, custom location, location provider, GPS target, selected trail and measure follow preferences through flows; trail map clear removed (switching trails redraws from the points flow); duplicate accent recreate in the settings panel removed | Done |
| 17 | Map view preference listeners (Google and OpenStreetMap bases) replaced by `MapPreferences` flows bound to the map lifecycle: look, compass rotation, pin, bearing, target, auto center, geodesic and wrap settings react only to their own keys (the OpenStreetMap GPS map no longer redraws the target line on every preference write); unused north only preference removed; location pin colours as constants so panel settings load in JVM tests | Done |
| 18 | Map view timers (heading sensor delay, trail auto center) as `DelayedTask` coroutines in a scope that lives only while the map is resumed, so pausing cancels them together with the preference flows; `Handler` runnables and the unused pause hook removed | Done |
| 19 | Sensor display rotation through `ContextCompat.getDisplayOrDefault` instead of the deprecated `WindowManager.defaultDisplay`; status bar height from window insets instead of the internal `status_bar_height` resource; reverse geocoding and map search share `Geocoding`; network check without the deprecated `NetworkInfo`; web view force dark fallback (a no-op on target SDK 33+) removed; custom location and direction target written in one edit with `apply` instead of several `commit`s; unused math helpers removed | Done |
| 20 | Unused resources removed (accent ripple attribute, `colorAccent`, animation duration, bold font, stale lint keep file, generated `versionName` string) and lint `UnusedResources` made fatal, so CI and `assembleRelease` fail on any unused resource; unused instrumentation runner and support vector flag removed | Done |
| 21 | Launcher art vectors rewritten: coordinates rounded to 0.1 viewport units on absolute positions (no drift along relative paths), default attributes, transparent strokes, viewport-sized and redundant rectangle clips and empty groups dropped; path data about 36% smaller (16.2 MB to 10.4 MB of XML) and about 2,300 fewer clip paths to apply per draw, with renders matching the originals within anti-aliasing; `LauncherArtTest` checks path grammar, precision and that every art has colours | Done |
| 22 | Launcher art (splash, arts pager, sun time art widget) rasterised once at display size on `Dispatchers.Default` by `ArtRaster` and faded in, instead of parsed and drawn as a vector during composition or sent to the launcher as a vector; the splash renders only the visible crop and the widget renders at its own width | Done |

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

1. Unit tests, release lint checks (the same fatal checks that gate `assembleRelease`, including `UnusedResources`) and a debug build; the build fails if an exported Room schema identity hash changed.
2. On pushes to `master` only: a signed release APK, published as a full GitHub release tagged `v1.0.<run number>`.

Required secrets: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS`. Optional: `MAPS_API_KEY`.

```sh
base64 -w0 release.keystore
```

Dependabot opens weekly update PRs for Gradle dependencies, the Gradle wrapper and GitHub Actions. If a PR fails at `checkDebugAarMetadata`, raise `compileSdk` first.

## Project layout

```
app/src/main/java/app/simple/positional/
  activities/   launcher and main activity
  ui/navigation/ routes and sub screen destinations
  constants/    static catalogs (help pages, pins, launcher art)
  ui/compose/   Compose theme, shared components, dialogs and screens
  sensors/      lifecycle-aware gravity and heading sensors
  ui/panels/    panel controllers
  extensions/   panel controller base, sensor panel, map callbacks
  decorations/  Google Maps and OpenStreetMap views, marker helpers
  database/     Room entities, DAOs and databases
  location/     location sources as lifecycle-bound flows
  widgets/      app widget providers
  math/         vectors, angles, rotation physics, distances, unit conversion
  util/         formatting and Android helpers
app/src/test/   JVM unit tests
docs/llm/       LLM context files (tracked, excluded from the dump)
```

`export.sh` writes `docs/llm/dump.txt` for LLM context: every tracked file except `docs/llm/`, with contents of source, build and config files only.

## Credits and license

Based on Positional by Hamza Rizwan. Licensed under the [GNU GPL v3](LICENSE). See [PRIVACY.md](PRIVACY.md).
