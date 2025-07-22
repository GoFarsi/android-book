FROM openjdk:17-jdk-slim

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/build-tools/35.0.0

# Install dependencies
RUN apt-get update && apt-get install -y \
    wget unzip git curl ca-certificates && \
    mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    cd /opt && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip && \
    unzip cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm cmdline-tools.zip && \
    yes | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --sdk_root=${ANDROID_HOME} --licenses && \
    ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --sdk_root=${ANDROID_HOME} \
        "platform-tools" \
        "platforms;android-35" \
        "build-tools;35.0.0" \
        "build-tools;34.0.0" \
        "ndk;27.2.12479018"

# Set Gradle properties for better performance and Java 17+ compatibility
ENV GRADLE_OPTS="-Xmx4096m -Dfile.encoding=UTF-8 --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED"

WORKDIR /workspace

# Copy project in (optional if using bind mount)
# COPY . .

# Default command to build the release APK
# Compatible with Android Gradle Plugin 8.7.3 and compileSdk 35
CMD ["./gradlew", "assembleRelease"]
