#include "platform/android/atomic_file.h"
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>

static void check_file(const char *path, unsigned char value) {
  FILE *f = fopen(path, "rb"); assert(f);
  for (unsigned i = 0; i < 8192; ++i) assert(fgetc(f) == value);
  assert(fgetc(f) == EOF); fclose(f);
}
int main(void) {
  char directory[] = "/tmp/sm-sram-test-XXXXXX"; assert(mkdtemp(directory));
  char path[256], bak[280], blocked[280];
  snprintf(path, sizeof(path), "%s/save.srm", directory);
  snprintf(bak, sizeof(bak), "%s.bak", path);
  unsigned char data[8192]; memset(data, 17, sizeof(data));
  assert(SmAtomicReplace(path, data, sizeof(data))); check_file(path, 17);
  memset(data, 91, sizeof(data));
  assert(SmAtomicReplace(path, data, sizeof(data))); check_file(path, 91); check_file(bak, 17);
  /* A failed backup write must leave the current save untouched. */
  snprintf(blocked, sizeof(blocked), "%s.bak.tmp", path);
  assert(mkdir(blocked, 0700) == 0);
  memset(data, 62, sizeof(data));
  assert(!SmAtomicReplace(path, data, sizeof(data))); check_file(path, 91); check_file(bak, 17);
  rmdir(blocked); unlink(path); unlink(bak); rmdir(directory);
  puts("Atomic SRAM: exact bytes, previous revision, failed-backup preservation passed");
}
