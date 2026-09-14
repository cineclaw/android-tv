# CineClaw Android TV Builder
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

RUN dpkg --add-architecture amd64 && \
    apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk-headless \
    curl \
    unzip \
    git \
    libc6:amd64 \
    libstdc++6:amd64 \
    zlib1g:amd64 \
    && rm -rf /var/lib/apt/lists/*

# Install Android SDK Command-line Tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    curl -sSL -o /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip -q /tmp/cmdline-tools.zip -d /tmp && \
    mv /tmp/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept Android SDK licenses and install platform 35 + build-tools
RUN yes | sdkmanager --sdk_root=${ANDROID_HOME} --licenses || true && \
    sdkmanager --sdk_root=${ANDROID_HOME} "platforms;android-35" "build-tools;35.0.0" "platform-tools"

WORKDIR /workspace

# Pre-cache gradle wrapper
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Copy source
COPY app ./app

# Build debug APK
RUN ./gradlew assembleDebug --no-daemon

VOLUME /dist
CMD cp /workspace/app/build/outputs/apk/debug/app-debug.apk /dist/cineclaw-tv-debug.apk &&     echo "APK built successfully and copied to /dist/cineclaw-tv-debug.apk"
