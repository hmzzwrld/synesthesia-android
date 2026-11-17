# SYNESTHESIA — Native Android

A native Kotlin + Jetpack Compose local music player, using the SYNESTHESIA website source as its visual reference. The app is not a WebView or browser wrapper.

## Website-to-Android visual port in this update

- Bundles the website's original `synesthesia-background.jpg` as the persistent app backdrop.
- Uses the website's actual ambient palette defaults: `#ff3d8b`, `#ff9f40`, `#a855f7`.
- Extracts a three-colour palette from the current album artwork and animates the ambient colours when the track changes.
- Ports the website's translucent liquid-fill gradient, reflective rim, and shadow treatment into the shared Compose glass modifier.
- Repositions the mini-player and bottom navigation as independent floating overlays above the scrolling content. List padding allows the final items to scroll fully above them.
- Makes the expanded player use blurred current-album artwork behind its glass panel.
- Adds an Up Next view backed by the current ExoPlayer media-item list, plus working shuffle, repeat, seek, previous, next and play/pause controls.
- Draws the subtle dotted mesh from the website background.
- Includes the website genre art assets in Android drawable resources for later native genre-card use.

## Native functionality retained

- MediaStore local audio scanning and runtime audio permission.
- ExoPlayer playback of local content URIs without copying audio files.
- Home listening history, most-played counts and monthly top tracks.
- Search, songs, albums, artists, genres and folders.
- Album artwork via MediaStore/Coil.

## Important implementation notes

The website CSS uses browser `backdrop-filter: blur(28px) saturate(170%)`. Standard Compose modifiers cannot sample and refract arbitrary sibling content behind a surface. This update ports the visible glass fill, highlights, edges and depth, and uses the real ambient background underneath; it does not claim pixel-identical browser backdrop refraction.

The current Android playback engine is still Activity-owned. Persistent background playback through a `MediaSessionService`, persistent playlists/favourites, queue drag-reordering, and complete website menu/artist interactions are not yet implemented in this project snapshot. They must be added before describing the Android app as a complete functional parity port.

## Build

The GitHub Actions workflow builds `app-debug.apk` with Gradle 8.7, Android Gradle Plugin 8.6.1, JDK 17 and Android SDK 35. ZIP integrity and Kotlin source syntax were checked in the preparation environment, but a full Gradle/Android build could not be run there because the Gradle distribution and Android SDK were not available. GitHub Actions is the required build verification checkpoint.
