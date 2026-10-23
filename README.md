# Positional

![banner](screenshots/banner.png)

A location, compass, clock, level, trail and measurement toolkit for Android. This is a community fork of [Hamza417/Positional](https://github.com/Hamza417/Positional), distributed only through GitHub Releases. It is not published on Google Play.

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

Android 7.0 (API 24) or newer is supported. Silent background updates through Obtainium require Android 12 or newer; older versions get an install prompt.

The release is signed with this fork's own key. It cannot be installed over the Play Store build of Positional; uninstall that first.

## Differences from upstream

- No Google Play licensing check, in-app update prompt or rating prompt.
- Release builds come from CI on every push to `master`.
- Dead code, unused resources and source comments removed.
- Unit tests for the math and utility code.

The map panels use the Google Maps SDK and Google Play services location. Without a Maps API key the map tiles stay blank and every other panel works.

## Building

Requirements: JDK 21 and the Android SDK (compile SDK 36).

```sh
./gradlew testDebugUnitTest assembleDebug
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

1. Unit tests and a debug build.
2. On pushes to `master` only: a signed release APK, published as a full GitHub release tagged `v1.0.<run number>`.

Required repository secrets: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS`. Optional: `MAPS_API_KEY`.

To create the keystore secret:

```sh
base64 -w0 release.keystore
```

Dependabot opens weekly update PRs for Gradle dependencies and GitHub Actions.

## Project layout

```
app/src/main/java/app/simple/positional/
  activities/   entry points and sub screens
  ui/           panel fragments (GPS, compass, clock, level, trail, measure, direction, settings)
  decorations/  custom views, map wrappers, fast scroller
  database/     Room entities, DAOs and databases
  services/     location and clock widget services
  widgets/      app widget providers
  math/         vectors, quaternions, angles, unit conversion
  util/         formatting and Android helpers
app/src/test/   JVM unit tests
```

`export.sh` writes a text dump of the repository to `docs/llm/dump.txt` for use as LLM context. It lists every tracked file and includes the contents of source, build and config files only.

## Credits and license

Original app by [Hamza Rizwan (Hamza417)](https://github.com/Hamza417/Positional). Translations come from the upstream Crowdin project.

Licensed under the [GNU GPL v3](LICENSE). See [PRIVACY.md](PRIVACY.md) for the privacy policy.
