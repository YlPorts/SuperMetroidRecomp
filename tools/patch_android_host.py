#!/usr/bin/env python3
"""Build-local hooks for the pinned host. Never modify the shared submodule.

Refuse an unrecognized host instead of silently losing suspend/save support.
The native host owns SRAM, snapshots and the device mixer on its own thread.
"""
import pathlib
import sys

def patch(source):
    anchor = '    if (!running)\n      break;\n    OverlaySelftestPadMainTick(frameCtr);'
    assert source.count(anchor) == 1, 'Pinned host changed: event-loop hook missing'
    service = '''    /* Android lifecycle: SDL's blocking-on-pause is disabled so the
     * emulation owner can flush SRAM before sleeping. No guest runs while
     * a phone menu or background activity holds the pause request. */
    {
      static bool android_was_paused;
      bool phone_paused = SmAndroidPaused();
      unsigned requests = SmAndroidCommands();
      if (phone_paused != android_was_paused) {
        SetAudioPaused(true);
        ResetAudioTimeline();
        g_reset_clock = true;
        SmAndroidResetMetrics();
        if (phone_paused) {
          g_input_state = 0;
          (void)SmAndroidFlushSram();
        } else SetAudioPaused(g_paused);
        android_was_paused = phone_paused;
      }
      /* Dirty SRAM only: bounded comparison every two seconds, no periodic
       * full savestate or storage write when the guest has not changed it. */
      {
        static unsigned checked;
        static bool retained;
        static uint8_t previous[8192];
        extern uint8_t *g_sram;
        extern int g_sram_size;
        if (g_sram && g_sram_size == sizeof(previous) && frameCtr - checked >= 120) {
          checked = frameCtr;
          if (!retained || memcmp(previous, g_sram, sizeof(previous))) {
            if (SmAndroidFlushSram()) { memcpy(previous, g_sram, sizeof(previous)); retained = true; }
          }
        }
      }
      int phone_volume;
      if (SmAndroidTakeVolume(&phone_volume)) {
        RtlApuLock();
        g_config.volume = phone_volume;
        g_sdl_audio_mixer_volume = phone_volume * SNESRECOMP_SDL_MIX_MAXVOLUME / 100;
        RtlApuUnlock();
      }
      if (requests) {
        bool ok = true;
        if (requests & SM_ANDROID_FLUSH) ok = SmAndroidFlushSram() != 0;
        char slot[128]; RtlSaveSlotPath(0, slot, sizeof(slot));
        if (requests & SM_ANDROID_SAVE) ok = RtlSaveSnapshot(slot);

        if (requests & SM_ANDROID_LOAD) {
          ok = RtlLoadSnapshot(slot);
          if (ok) GameReset();
          ResetAudioTimeline();
          g_reset_clock = true;
        }
        SmAndroidCommandDone(requests, ok);
      }
      if (phone_paused && running) { SDL_Delay(8); continue; }
    }
'''
    source = source.replace(anchor, service + anchor)
    assert source.count('  RtlWriteSram();') == 1
    source = source.replace('  RtlWriteSram();', '  (void)SmAndroidFlushSram();')
    return '#include "platform/android/android_runtime.h"\n' + source

if __name__ == '__main__':
    src, dst = map(pathlib.Path, sys.argv[1:])
    dst.write_text(patch(src.read_text()), encoding='utf-8')
