#!/usr/bin/env bash
# Generate from the owner's ROM, then build the actual Android game.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ $# -ne 1 ]]; then
    echo "Uso: bash android/build-port.sh /ruta/SuperMetroid.sfc" >&2
    exit 2
fi
rom_path="$(realpath "$1")"
git submodule update --init --recursive
bash android/fetch_sdl.sh
bash tools/regen.sh --rom "$rom_path" --strict-idempotent
bash android/test-core.sh
cd android
./gradlew :app:assembleRelease --no-daemon --max-workers=4
echo "APK: android/app/build/outputs/apk/release/"
echo "Para una firma estable, configura SM_ANDROID_KEYSTORE y sus contraseñas."
