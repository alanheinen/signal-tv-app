# Android build environment for local builds: JDK 17, the SDK command-line
# tools, the platform and build tools AGP needs, and platform-tools (adb) for
# installing onto a TV over the network. CI uses GitHub's runner instead.
#
#   docker build -t signal-tv-app-build -f docker/build.Dockerfile docker
FROM eclipse-temurin:17.0.13_11-jdk-noble

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools

RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/* \
 && mkdir -p /opt/android-sdk/cmdline-tools \
 && curl -fsSL -o /tmp/tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
 && unzip -q /tmp/tools.zip -d /opt/android-sdk/cmdline-tools \
 && mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest \
 && rm /tmp/tools.zip \
 && yes | sdkmanager --licenses >/dev/null \
 && sdkmanager --install "platforms;android-35" "build-tools;35.0.0" "build-tools;34.0.0" "platform-tools" >/dev/null

WORKDIR /src
