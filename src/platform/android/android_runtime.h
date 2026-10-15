#pragma once
#include <stdbool.h>

enum { SM_ANDROID_FLUSH = 1, SM_ANDROID_SAVE = 2, SM_ANDROID_LOAD = 4 };
bool SmAndroidPaused(void);
unsigned SmAndroidCommands(void);
void SmAndroidCommandDone(unsigned commands, bool ok);
bool SmAndroidTakeVolume(int *volume);
bool SmAndroidIntegerScale(void);
bool SmAndroidSafeWide(void);
unsigned SmAndroidPresentationFps(void);
void SmAndroidNoteFrame(double guest_seconds, double run_seconds, int output_rate);
void SmAndroidResetMetrics(void);
bool SmAndroidFlushSram(void);
void SmAndroidSafeInsets(int width, int height, int edges[4]);
