# NasMusic

Rev. 0 | Created: 2026-09-15 | Updated: 2026-09-15 05:47 UTC

## 1. Purpose

NasMusic is an Android client that plays a music library kept on a Synology NAS and served over the
Subsonic REST API. It covers the path from a server login to audio coming out of the phone: browse
artist, album and song, then hand the album to a background player that keeps running when the app
is closed.

This repository holds the project skeleton. The screens, the API client and the playback service are
in place; the features listed in section 7 are not.

## 2. Requirements

Table 1. Toolchain and runtime targets

| Item                    | Version                   |
| :---------------------: | :-----------------------: |
| Android Studio          | Ladybug (2024.2) or later |
| JDK                     | 17                        |
| Android Gradle Plugin   | 8.7.3                     |
| Kotlin                  | 2.0.21                    |
| Gradle wrapper          | 8.11.1                    |
| compileSdk / targetSdk  | 35                        |
| minSdk                  | 26 (Android 8.0)          |

A Subsonic-compatible server is required. Navidrome is the one this project is written against; see
section 5.

## 3. Project Layout

Table 2. Source files under `app/src/main/java/com/yrocket/nasmusic/` and what each one owns

| Path                             | Role                                                          |
| :------------------------------: | :-----------------------------------------------------------: |
| MainActivity.kt                  | Hosts the UI, binds the MediaController to the service        |
| data/ServerConfig.kt             | Server URL, username and password, validated on construction  |
| data/SettingsStore.kt            | Persists the configuration in DataStore                       |
| data/subsonic/SubsonicApi.kt     | Retrofit endpoints: ping, getArtists, getArtist, getAlbum     |
| data/subsonic/SubsonicAuth.kt    | Salted-token parameters, as interceptor and as URL builder    |
| data/subsonic/SubsonicModels.kt  | Reply payloads                                                |
| data/subsonic/SubsonicRepository.kt | Unwraps the envelope, builds stream and cover-art URLs     |
| playback/PlaybackService.kt      | MediaSessionService holding ExoPlayer                         |
| ui/LibraryViewModel.kt           | Screen state and the artist to album to song drill-down       |
| ui/NasMusicApp.kt                | Compose screens: setup, artists, albums, songs                |

ExoPlayer and Coil fetch their URLs directly, without passing through Retrofit, so the
authentication parameters live in `SubsonicAuth.appendTo`, which serves both those URLs and the
Retrofit interceptor.

## 4. Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Android Studio's Run button builds and installs the same debug APK, and is the shorter path for the
first build because it installs the missing SDK packages on its own.

## 5. Server Setup

Navidrome exposes the Subsonic API and runs as one container on DSM.

```yaml
# docker-compose.yml, deployed through DSM Container Manager
services:
  navidrome:
    image: deluan/navidrome:latest
    ports:
      - "4533:4533"
    volumes:
      - /volume1/music:/music:ro
      - /volume1/docker/navidrome:/data
    restart: unless-stopped
```

Reach it at `http://<NAS_IP>:4533` on the local network and create the first admin account. For
access from outside, put the DSM reverse proxy in front of it with a Let's Encrypt certificate and
enter that HTTPS address in the app. Port 4533 itself stays closed on the router.

The app requires HTTPS: an `http://` address fails on a release build unless
`usesCleartextTraffic` is enabled in the manifest.

## 6. Status

This skeleton has not been compiled. The environment it was written in carries no Android SDK and
cannot reach `dl.google.com`, so neither Gradle sync nor `assembleDebug` has been run. Treat the
first build in Android Studio as part of the work, and expect the versions in
`gradle/libs.versions.toml` to need a bump if Studio reports newer ones.

## 7. Further Work

- Offline download, through `SimpleCache` on the ExoPlayer data source
- Now-playing screen with seek bar, shuffle and repeat
- Search across artists, albums and songs, through `search3.view`
- Playlist read and write, through `getPlaylists.view` and `updatePlaylist.view`
- Cover art in the lists, through Coil and `SubsonicRepository.coverArtUrl`
- Password storage in an encrypted store rather than clear-text DataStore
- Multiple servers, replacing the single stored configuration

---

## Appendix A. Terminology

- **DataStore**: Jetpack library that persists key-value settings, replacing SharedPreferences.
- **ExoPlayer**: Media3 player implementation that handles decoding, buffering and streaming.
- **MediaController**: Client handle an activity uses to drive a player living in another process.
- **MediaSessionService**: Android service type that owns a player and publishes it to the system,
  giving the notification, Bluetooth and Android Auto controls.
- **Navidrome**: Server that indexes a music folder and serves it over the Subsonic API.
- **Salted token**: Subsonic authentication where each request carries a random salt and the MD5 of
  the password concatenated with that salt, so the password itself is never sent.
- **Subsonic API**: REST protocol for music servers, implemented by Navidrome and read by many
  Android clients.
