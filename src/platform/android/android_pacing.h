#pragma once
#include <stdbool.h>

/* Policy for the framework clock, not a second emulation clock. Let a short
 * scheduling interruption repay at most three fields. Long interruptions
 * re-anchor on the current time rather than accumulating stale input work. */
typedef struct SmAndroidPacing {
  double next;
  bool started;
} SmAndroidPacing;

static inline bool SmAndroidKeepShortDebt(SmAndroidPacing *p, double seconds,
                                        double periods, double hz, bool loading) {
  double period = 1.0 / hz;
  if (!p->started) { p->next = 0; p->started = true; }
  p->next += periods * period;
  bool keep = loading || seconds <= p->next + 3 * period;
  if (!keep) p->next = seconds + period;
  return keep;
}
