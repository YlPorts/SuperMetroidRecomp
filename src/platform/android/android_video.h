#pragma once
#include "sm_video.h"

/* Called only by the SDL presentation thread. JNI publishes a packed value. */
void SmAndroidApplyVideo(SmVideoSettings *settings);
