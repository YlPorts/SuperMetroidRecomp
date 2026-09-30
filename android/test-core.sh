#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
output="$(mktemp -d)"
trap 'rm -rf "$output"' EXIT
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
javac_bin="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
"$javac_bin" -d "$output" \
    android/app/src/main/java/com/ylports/supermetroid/TouchInput.java \
    android/app/src/main/java/com/ylports/supermetroid/RomImporter.java \
    android/tests/TouchInputTest.java android/tests/RomImporterTest.java
"$java_bin" -cp "$output" com.ylports.supermetroid.TouchInputTest
"$java_bin" -cp "$output" com.ylports.supermetroid.RomImporterTest "$@"
