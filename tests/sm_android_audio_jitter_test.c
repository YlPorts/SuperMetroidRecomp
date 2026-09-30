/* Deterministic scheduling replay through the real DSP FIFO/resampler. No
 * device, ROM, synthesis substitution or wall-time performance assumption. */
#include <assert.h>
#include <math.h>
#include <stdio.h>
#include "../snesrecomp/runner/src/common_rtl.c"
#include "platform/android/android_pacing.h"
#include "desktop/host_clock.h"
void RtlApuLock(void) {}
void RtlApuUnlock(void) {}
Snes *g_snes;
static Dsp queue;
static int16_t output[4096];
static void produce(void) {
  assert(dsp_available(&queue) + 534 < DSP_SAMPLE_RING);
  for (unsigned i = 0; i < 534; ++i) {
    unsigned at = queue.sampleWrite++ & (DSP_SAMPLE_RING - 1);
    queue.sampleBuffer[at * 2] = 10000;
    queue.sampleBuffer[at * 2 + 1] = -10000;
  }
}
static uint64_t run(bool android_policy) {
  memset(&queue, 0, sizeof(queue));
  rtl_reset_audio_delivery(); RtlSetAudioOutputRate(48000);
  AudioTraceStats before, after;
  audio_trace_get_stats(&before);
  SnesHostClock clock; snes_host_clock_reset(&clock, 0, SNES_HOST_NTSC_HZ, SNES_HOST_NTSC_HZ);
  SmAndroidPacing policy = {0};
  double finished = 0, callback = 0;
  for (unsigned frame = 1; frame <= 3000; ++frame) {
    double began = fmax(finished, clock.next_simulation);
    /* 2 ms of work, with a periodic 18 ms scheduling interruption. */
    finished = began + .002 + (frame % 25 == 0 ? .018 : 0);
    while (callback < finished) {
      uint32_t write = queue.sampleWrite;
      rtl_render_native(&queue, output, 2048);
      assert(queue.sampleWrite == write); /* consumer cannot invent guest time */
      callback += 2048.0 / 48000;
    }
    produce();
    bool keep = android_policy && SmAndroidKeepShortDebt(&policy, finished, 1, SNES_HOST_NTSC_HZ, false);
    snes_host_clock_simulation_done(&clock, finished, keep, 1);
  }
  audio_trace_get_stats(&after);
  return after.output_underflows - before.output_underflows;
}
int main(void) {
  uint64_t old = run(false), improved = run(true);
  assert(old > 0 && improved == 0);
  printf("Android audio jitter replay: %llu old underflows -> %llu with short-debt recovery (3000 frames)\n",
         (unsigned long long)old, (unsigned long long)improved);
  return 0;
}
