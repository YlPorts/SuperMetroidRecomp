#include "android_video.h"
#include <jni.h>
#include <stdatomic.h>

static _Atomic unsigned pending_video;

JNIEXPORT void JNICALL
Java_com_ylports_supermetroid_GameActivity_nativeVideoSettings(
    JNIEnv *env, jclass cls, jboolean enhanced, jint aspect, jboolean hud) {
  (void)env;
  (void)cls;
  if (aspect < 0 || aspect >= SM_ASPECT_COUNT) return;
  atomic_store_explicit(&pending_video, 0x100u | (unsigned)aspect |
      (enhanced ? 0x10u : 0) | (hud ? 0x20u : 0), memory_order_release);
}

void SmAndroidApplyVideo(SmVideoSettings *settings) {
  unsigned options = atomic_exchange_explicit(&pending_video, 0, memory_order_acquire);
  if (!options) return;
  settings->aspect = (SmAspect)(options & 0xf);
  settings->enhanced = (options & 0x10) != 0;
  settings->hud_anchored = (options & 0x20) != 0;
}
