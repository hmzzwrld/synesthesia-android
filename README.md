# SYNESTHESIA Mobile — Native Android

This build is a first functional local-library/player milestone based on the supplied SYNESTHESIA website and Android starter project.

## Included in this milestone
- Native Kotlin + Jetpack Compose interface; no WebView.
- Midnight Aurora palette and shared translucent glass surfaces.
- Home dashboard: recently played, most played and current-month top 10 (counts persist locally).
- MediaStore audio scan for device-stored music; permission is requested at runtime.
- Library groupings: songs, albums, artists, genres placeholder (Android MediaStore does not reliably expose genre for every file), and folders.
- Search across local song, artist and album names.
- ExoPlayer playback, playback queue in library order, mini-player and expanded player with seeking and previous/next.
- Animated screen changes and content transitions.

## Important limitations / next milestones
- This milestone does not yet include a MediaSessionService, so background playback, lock-screen/media notification controls and headset/Bluetooth controls are not complete.
- Genre grouping currently uses a safe `Unknown genre` fallback; embedded genre extraction and proper genre browsing are next.
- Album artwork uses MediaStore album-art URIs with a fallback tile; not every device exposes artwork.
- No cloud sync or streaming catalogue. Music remains local and is not copied by the app.
- Monthly top 10 counts begin when this version is installed; earlier browser history is not imported.
- Android Gradle build must be verified by GitHub Actions. A green workflow is required before treating the APK as build-verified.

## Build
GitHub Actions runs `assembleDebug` and uploads `app-debug.apk` as an artifact.
