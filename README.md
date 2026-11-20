# SYNESTHESIA — Native Android

A native Kotlin + Jetpack Compose local music player, using the SYNESTHESIA website source as its visual reference. The app is not a WebView or browser wrapper.

## Website-to-Android visual port in this update

- Bundles the website's original `synesthesia-background.jpg` as the persistent app backdrop.
- Uses the website's actual ambient palette defaults: `#ff3d8b`, `#ff9f40`, `#a855f7`.
- Extracts a three-colour palette from the current album artwork and animates the ambient colours when the track changes.
- Ports the website's translucent liquid-fill gradient, reflective rim, and shadow treatment into the shared Compose glass modifier.
- Repositions the mini-player and bottom navigation as independent floating overlays above the scrolling content. The mini-player is deliberately narrower and uses a richer layered liquid-glass material; navigation is wider and uses lighter frosted glass. List padding allows the final items to scroll fully above them.
- Gives the mini-player an adaptive two-line title layout and animated size changes, with a dedicated Liquid Queue shortcut and long-press gesture.
- Adds a native Liquid Queue surface inspired by the website queue: organic rounded glass panel, reflective layers, connecting spring entrance, current-track emphasis, queue count, track selection, upcoming-track reordering and clear-upcoming action.
- Reworks library track rows into compact rounded artwork tiles with restrained glass layering. Navigation selection glides between destinations with spring motion; screen transitions use softer spring movement. The fixed wallpaper receives only a very slight softening pass.
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

The current Android playback engine is still Activity-owned. Persistent background playback through a `MediaSessionService`, persistent playlists/favourites, touch drag-reordering (the current queue offers explicit move-up/move-down controls), queue saving as a playlist, and complete website menu/artist interactions are not yet implemented in this project snapshot. They must be added before describing the Android app as a complete functional parity port.

## Build

The GitHub Actions workflow builds `app-debug.apk` with Gradle 8.7, Android Gradle Plugin 8.6.1, JDK 17 and Android SDK 35. ZIP integrity and Kotlin parser checks are run before delivery. A full Gradle/Android build must still be confirmed by GitHub Actions; do not treat source parsing as a successful APK build.

## v13 interaction and player polish
- Settings now consumes touches as a full-screen modal layer and handles Back to close, preventing controls beneath Settings from receiving taps.
- Expanded-player Queue now toggles an in-artwork smart queue overlay; it no longer opens the separate floating Liquid Queue. The floating queue remains available from the mini-player.
- Settings includes a persisted Dark/Light appearance choice (Dark is the default) and the Settings header has more top spacing.
- Expanded-player accents use the current album/atmosphere palette for Now Playing, album metadata, seek bar, queue indicator and playback controls.
- Play/pause now uses a spring rotation and scale transition with an animated icon swap.


## v15 playback and visual refinements

- The bottom navigation remains visible and fixed while the mini-player's floating Liquid Queue is open.
- Corrected the expanded-player play glyph so the idle play arrow is not rotated.
- Album-derived accents preserve their hue but are lifted when too dark; near-black/low-saturation art maps to a readable soft grey, and dark chromatic hues (such as green) are brightened.
- Added a Media3 `MediaSessionService` around the shared ExoPlayer instance. Android can now publish the standard media notification and expose play/pause/previous/next controls to the lock screen, Bluetooth devices and headset media buttons while the Activity is backgrounded.
- Track title, artist, album and artwork URI are attached to each media item for system media metadata.

The notification and lock-screen presentation are rendered by Android from the app's MediaSession metadata; their exact appearance varies by Android version and device manufacturer.
