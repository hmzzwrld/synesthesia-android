# SYNESTHESIA Mobile — Native Android

This build is a first functional local-library/player milestone based on the supplied SYNESTHESIA website and Android starter project.

## Latest visual update — Midnight Aurora / Liquid Glass pass

- Reworked the shared glass material with a softer translucent body, layered lavender reflections, inner/outer rim lighting and a restrained upper sheen.
- Removed the almost-opaque bottom-navigation fill; the actual navigation used by `MainActivity` is now a floating glass surface.
- Reduced the expanded-player dim layer so the Aurora remains visible behind the player.
- Made the Aurora responsive to device dimensions instead of relying on fixed pixel coordinates.
- Added slow ambient drift and scroll-linked parallax; scrolling Home, Search and Library subtly moves the background light fields.
- Connected album-art colour extraction to the selected track so the persistent Aurora palette transitions with the music.
- Improved artwork palette sampling to choose repeated/representative colours rather than arbitrary sample pixels.
- Enabled transparent system bars so the atmosphere can extend behind the status and navigation bars.
- Updated the GitHub Actions SDK packages to match compileSdk 35 and removed duplicate Gradle declarations.

## Included in this milestone
- Native Kotlin + Jetpack Compose interface; no WebView.
- Midnight Aurora palette and shared translucent glass surfaces.
- Home dashboard: recently played, most played and current-month top 10 (counts persist locally).
- MediaStore audio scan for device-stored music; permission is requested at runtime.
- Library groupings: songs, albums, artists, genres placeholder (Android MediaStore does not reliably expose genre for every file), and folders.
- Search across local song, artist and album names.
- ExoPlayer playback, playback queue in library order, mini-player and expanded player with seeking and previous/next.
- Animated screen changes and content transitions.

## Visual implementation notes

The current glass treatment is a lightweight Compose implementation using layered gradients, shadows, rim highlights and animated ambient colours. It is **not yet true real-time backdrop refraction**: Compose does not automatically provide a captured copy of the content behind each surface. A proper backdrop capture/refraction pass would need a separate rendering approach and device testing.

## Important limitations / next milestones
- This milestone does not yet include a MediaSessionService, so background playback, lock-screen/media notification controls and headset/Bluetooth controls are not complete.
- Genre grouping currently uses a safe `Unknown genre` fallback; embedded genre extraction and proper genre browsing are next.
- Album artwork uses MediaStore album-art URIs with a fallback tile; not every device exposes artwork.
- No cloud sync or streaming catalogue. Music remains local and is not copied by the app.
- Monthly top 10 counts begin when this version is installed; earlier browser history is not imported.
- Android Gradle build must be verified by GitHub Actions. This ZIP has not been compiled in this environment; a green workflow is required before treating the APK as build-verified.

## Build
GitHub Actions runs `assembleDebug` and uploads `app-debug.apk` as an artifact.
