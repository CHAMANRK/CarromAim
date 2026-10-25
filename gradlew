#!/usr/bin/env sh
# Bootstrap Gradle 8.13 without a checked-in wrapper JAR. CI requires network access.
set -eu
VERSION=8.13
# Prefer a preinstalled Gradle (CI can provision it) before attempting a network bootstrap.
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/bootstrap"
DIST="$BASE/gradle-$VERSION"
ZIP="$BASE/gradle-$VERSION-bin.zip"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$BASE"
  if [ ! -f "$ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ZIP"
    elif command -v wget >/dev/null 2>&1; then wget "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -O "$ZIP"
    else echo 'curl or wget is required to bootstrap Gradle' >&2; exit 1; fi
  fi
  unzip -q -o "$ZIP" -d "$BASE"
fi
exec "$DIST/bin/gradle" "$@"
