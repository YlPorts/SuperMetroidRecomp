#include "android_runtime.h"
#include "audio_trace.h"
#include "common_rtl.h"
#include "atomic_file.h"
#include <sys/stat.h>
#include <jni.h>
#include <stdatomic.h>
#include <stdio.h>
#include <stdint.h>

/* UI threads publish requests only. SRAM, snapshots, mixer and PPU are owned
 * by the SDL thread. No Java callback touches guest memory or SPC clocks. */
static _Atomic bool paused;
static _Atomic unsigned commands, completed, result;
static _Atomic unsigned options = 100 | (1u << 11), volume_pending;
static _Atomic uint64_t frame_count, mean_us, worst_us, underflows, missing, dropped;
static _Atomic unsigned fps_milli, occupancy, device_rate;
static double sample_start, guest_sum;
static unsigned sample_frames;
static uint64_t window_max;
static _Atomic uint64_t safe_edges;
JNIEXPORT void JNICALL Java_com_ylports_supermetroid_GameActivity_nativeInsets(
    JNIEnv *e, jclass c, jint left, jint top, jint right, jint bottom) {
  (void)e; (void)c;
  if (left < 0 || top < 0 || right < 0 || bottom < 0 ||
      left > 4096 || right > 4096 || top > 4096 || bottom > 4096) return;
  atomic_store(&safe_edges, (uint64_t)left | (uint64_t)top << 16 |
      (uint64_t)right << 32 | (uint64_t)bottom << 48);
}
void SmAndroidSafeInsets(int width, int height, int edges[4]) {
  uint64_t packed = atomic_load(&safe_edges);
  for (int i = 0; i < 4; ++i) edges[i] = (packed >> (i * 16)) & 65535;
  if (edges[0] + edges[2] >= width || edges[1] + edges[3] >= height)
    for (int i = 0; i < 4; ++i) edges[i] = 0;
}
bool SmAndroidFlushSram(void) {
  extern uint8_t *g_sram;
  extern int g_sram_size;
  if (!g_sram || g_sram_size <= 0) return false;
  mkdir("saves", 0755);
  char path[128]; RtlSramFilePath(path, sizeof(path));
  return SmAtomicReplace(path, g_sram, (size_t)g_sram_size);
}

JNIEXPORT void JNICALL Java_com_ylports_supermetroid_GameActivity_nativeSetPaused(
    JNIEnv *e, jclass c, jboolean value) {
  (void)e; (void)c; atomic_store_explicit(&paused, value, memory_order_release);
}
JNIEXPORT void JNICALL Java_com_ylports_supermetroid_GameActivity_nativeOptions(
    JNIEnv *e, jclass c, jint volume, jint fps, jboolean integer, jboolean safe) {
  (void)e; (void)c;
  if (volume < 0 || volume > 100 || (fps != 30 && fps != 60)) return;
  atomic_store(&options, (unsigned)volume | (fps == 30 ? 1u << 8 : 0) |
      (integer ? 1u << 10 : 0) | (safe ? 1u << 11 : 0));
  atomic_store_explicit(&volume_pending, (unsigned)volume + 1, memory_order_release);
}
JNIEXPORT void JNICALL Java_com_ylports_supermetroid_GameActivity_nativeCommand(
    JNIEnv *e, jclass c, jint command) {
  (void)e; (void)c;
  if (command != SM_ANDROID_FLUSH && command != SM_ANDROID_SAVE && command != SM_ANDROID_LOAD) return;
  atomic_store(&completed, 0);
  atomic_fetch_or_explicit(&commands, (unsigned)command, memory_order_release);
}
JNIEXPORT jint JNICALL Java_com_ylports_supermetroid_GameActivity_nativeCommandResult(
    JNIEnv *e, jclass c) {
  (void)e; (void)c;
  return atomic_load_explicit(&completed, memory_order_acquire) ? (atomic_load(&result) ? 1 : -1) : 0;
}
bool SmAndroidPaused(void) { return atomic_load_explicit(&paused, memory_order_acquire); }
unsigned SmAndroidCommands(void) { return atomic_exchange_explicit(&commands, 0, memory_order_acquire); }
void SmAndroidCommandDone(unsigned cmd, bool ok) {
  atomic_store(&result, ok); atomic_store_explicit(&completed, cmd, memory_order_release);
}
bool SmAndroidTakeVolume(int *volume) {
  unsigned v = atomic_exchange_explicit(&volume_pending, 0, memory_order_acquire);
  if (!v) return false;
  *volume = (int)v - 1; return true;
}
bool SmAndroidIntegerScale(void) { return (atomic_load(&options) & (1u << 10)) != 0; }
bool SmAndroidSafeWide(void) { return (atomic_load(&options) & (1u << 11)) != 0; }
unsigned SmAndroidPresentationFps(void) { return (atomic_load(&options) & (1u << 8)) ? 30 : 60; }
void SmAndroidResetMetrics(void) { sample_start = 0; sample_frames = 0; guest_sum = 0; window_max = 0; }
void SmAndroidNoteFrame(double guest, double seconds, int rate) {
  atomic_fetch_add(&frame_count, 1);
  if (!sample_start) sample_start = seconds;
  ++sample_frames; guest_sum += guest;
  uint64_t us = (uint64_t)(guest * 1000000);
  if (us > window_max) window_max = us;
  if (seconds - sample_start < 1) return;
  atomic_store(&fps_milli, (unsigned)((sample_frames - 1) * 1000 / (seconds - sample_start)));
  atomic_store(&mean_us, (uint64_t)(guest_sum * 1000000 / sample_frames));
  atomic_store(&worst_us, window_max);
  AudioTraceStats stats; audio_trace_get_stats(&stats);
  atomic_store(&underflows, stats.output_underflows);
  atomic_store(&missing, stats.output_missing_frames);
  atomic_store(&dropped, stats.dropped);
  atomic_store(&occupancy, stats.occupancy_current);
  atomic_store(&device_rate, rate);
  SmAndroidResetMetrics();
}
JNIEXPORT jstring JNICALL Java_com_ylports_supermetroid_GameActivity_nativeDiagnostics(
    JNIEnv *e, jclass c) {
  (void)c; char text[768];
  snprintf(text, sizeof(text),
      "Motor: %.2f cuadros/s\nCuadros ejecutados: %llu\nCPU del juego: %.2f ms (máx. %.2f ms)\n"
      "Audio: %u Hz\nVaciados desde el arranque: %llu\nMuestras de salida ausentes: %llu\n"
      "Muestras descartadas: %llu\nCola SPC: %u muestras\n\n"
      "La media de CPU excluye composición y GPU. Los vaciados incluyen el arranque. "
      "Este informe mide esta sesión; no certifica FPS estables en otros dispositivos.",
      atomic_load(&fps_milli) / 1000.0, (unsigned long long)atomic_load(&frame_count),
      atomic_load(&mean_us) / 1000.0, atomic_load(&worst_us) / 1000.0,
      atomic_load(&device_rate), (unsigned long long)atomic_load(&underflows),
      (unsigned long long)atomic_load(&missing), (unsigned long long)atomic_load(&dropped),
      atomic_load(&occupancy));
  return (*e)->NewStringUTF(e, text);
}
