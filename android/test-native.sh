#!/usr/bin/env bash
# Device-free regressions against the pinned production renderer/audio consumer.
set -euo pipefail
cd "$(dirname "$0")/.."
out="${SM_TEST_OUTPUT:-build-tests}"
mkdir -p "$out"
cc_bin="${CC:-cc}"
common=(-std=c11 -D_POSIX_C_SOURCE=200809L -O2 -UNDEBUG -Isrc -Isnesrecomp/runner/src -Isnesrecomp/runner/src/desktop -Isnesrecomp/runner/src/snes -Iandroid/app/jni/SDL/include)
compile_run() {
    local name="$1"; shift
    "$cc_bin" "${common[@]}" "$@" -lm -o "$out/$name"
    "$out/$name"
}
compile_run atomic_file tests/sm_atomic_file_test.c src/platform/android/atomic_file.c
compile_run display tests/sm_display_test.c src/sm_display.c
compile_run pacing tests/sm_android_pacing_test.c
compile_run mode7 tests/sm_mode7_test.c src/sm_mode7.c
compile_run renderer tests/sm_renderer_test.c src/sm_renderer.c src/sm_mode7.c
compile_run snapshot tests/sm_snapshot_test.c src/sm_renderer.c src/sm_mode7.c
compile_run windows tests/ppu_window_test.c
compile_run overlay tests/sm_overlay_blit_test.c snesrecomp/runner/src/snes_overlay_draw.c
compile_run gesture -DSNESRECOMP_SDL3=0 tests/sm_savestate_gesture_test.c snesrecomp/runner/src/snes_savestate_menu.c snesrecomp/runner/src/snes_overlay_draw.c
compile_run audio -flto -fwhole-program -ffunction-sections -fdata-sections -DSNESRECOMP_TRACE=0 -DSNESRECOMP_SDL3=0 -DSDL_MAIN_HANDLED snesrecomp/tests/audio_delivery_test.c snesrecomp/runner/src/snes/dsp.c snesrecomp/runner/src/audio_trace.c -Wl,--gc-sections
python3 tests/door_audio_trace_test.py
python3 tools/patch_android_host.py snesrecomp/runner/src/desktop/host_main.c "$out/android_host.c"
