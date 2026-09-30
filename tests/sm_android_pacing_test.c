#include "platform/android/android_pacing.h"
#include <assert.h>
#include <math.h>
#include <stdio.h>

int main(void) {
  SmAndroidPacing p = {0};
  double period = 1.0 / 60.098811862;
  for (unsigned f = 1; f <= 600; ++f)
    assert(SmAndroidKeepShortDebt(&p, f * period - .003, 1, 1 / period, false));
  /* A 25 ms interruption is recovered, rather than adding a full idle field
   * after each late frame. Work more than three fields old is discarded. */
  assert(SmAndroidKeepShortDebt(&p, 601 * period + .025, 1, 1 / period, false));
  assert(SmAndroidKeepShortDebt(&p, 602 * period + .007, 1, 1 / period, false));
  assert(!SmAndroidKeepShortDebt(&p, 15, 1, 1 / period, false));
  assert(fabs(p.next - (15 + period)) < 1e-9);
  assert(SmAndroidKeepShortDebt(&p, 15 + period, 1, 1 / period, false));
  /* Non-interactive loaders keep the pre-existing guest audio debt policy. */
  assert(SmAndroidKeepShortDebt(&p, 20, 18, 1 / period, true));
  assert(!SmAndroidKeepShortDebt(&p, 20.01, 1, 1 / period, false));
  assert(SmAndroidKeepShortDebt(&p, 20.01 + period, 1, 1 / period, false));
  puts("Android cadence: short jitter, long stalls, resume and multi-field loaders passed");
  return 0;
}
