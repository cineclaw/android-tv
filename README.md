# CineClaw Android TV (`android-tv`)

Native Android TV and Google TV cinema client (`com.cineclaw.tv`) built for a premium 10-foot living room experience.

## Tech Stack
- **Kotlin 2.1**
- **Jetpack Compose for TV**: `androidx.tv:tv-material`, `androidx.tv:tv-foundation`
- **AndroidX Media3 / ExoPlayer**: `media3-exoplayer`, `media3-ui`, `media3-datasource-okhttp`
- **Architecture**: MVI / Clean Architecture with unidirectional state flow

## Features
- **10-Foot Living Room UI**: Obsidian cinema theme (`#07090E`), emerald focus borders, smooth focus animations (1.06x scale).
- **Zero-Transcode Direct Streaming**: Native MKV streaming from TorrServer MatriX with embedded audio and subtitle track switching.
- **Resilient Multichannel Audio**: `ChannelMixingAudioProcessor` downmixes 5.1/7.1 to stereo TV speakers using ITU-R BS.775 coefficients, eliminating clipping and mute bugs.
- **TV Quick-Pair & Remote Navigation**: D-Pad optimized, timeline scrubber with timecode deltas, grouped quality picker (4K, 1080p, 720p, SD).
- **Living Room Search**: Leanback/Gboard IME integration, dedicated voice search, suggestion chips.

## Build & Deploy
```bash
# Debug build and install via ADB
./gradlew installDebug

# Production release APK
./gradlew assembleRelease
```
