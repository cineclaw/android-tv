#!/usr/bin/env bash
set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DIST_DIR="$SCRIPT_DIR/dist"
mkdir -p "$DIST_DIR"

echo "=================================================="
echo " CineClaw Android TV — Standalone APK Builder     "
echo "=================================================="

# Check if local Android SDK is present
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    echo "[INFO] Local ANDROID_HOME found at $ANDROID_HOME. Building on host..."
    ./gradlew assembleDebug
    cp app/build/outputs/apk/debug/app-debug.apk "$DIST_DIR/cineclaw-tv-debug.apk"
    echo "[SUCCESS] APK created at $DIST_DIR/cineclaw-tv-debug.apk"
    exit 0
fi

# Fallback to Docker standalone build
echo "[INFO] No local Android SDK found. Building via Docker container..."
IMAGE_NAME="cineclaw-tv-builder:latest"

docker build -t "$IMAGE_NAME" -f Dockerfile .
docker run --rm -v "$DIST_DIR":/dist "$IMAGE_NAME"

echo "=================================================="
echo "[SUCCESS] APK built: $DIST_DIR/cineclaw-tv-debug.apk"
echo "=================================================="
