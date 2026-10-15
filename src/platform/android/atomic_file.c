#include "atomic_file.h"
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>

/* Retain the old current file until the final atomic replacement. Rotating
 * current -> backup first leaves a crash window with no current save. */
bool SmAtomicReplace(const char *path, const void *data, size_t size) {
  char temp[1024], backup[1024], backup_temp[1024];
  if (!path || !data || !size ||
      snprintf(temp, sizeof(temp), "%s.tmp", path) >= (int)sizeof(temp) ||
      snprintf(backup, sizeof(backup), "%s.bak", path) >= (int)sizeof(backup) ||
      snprintf(backup_temp, sizeof(backup_temp), "%s.bak.tmp", path) >= (int)sizeof(backup_temp)) return false;
  FILE *f = fopen(temp, "wb");
  if (!f) return false;
  bool ok = fwrite(data, 1, size, f) == size;
  ok = fflush(f) == 0 && ok;
  ok = fsync(fileno(f)) == 0 && ok;
  ok = fclose(f) == 0 && ok;
  if (!ok) { remove(temp); return false; }
  FILE *old = fopen(path, "rb");
  if (old) {
    FILE *bak = fopen(backup_temp, "wb");
    ok = bak != NULL;
    if (bak) {
      char bytes[8192]; size_t count;
      while ((count = fread(bytes, 1, sizeof(bytes), old)))
        if (fwrite(bytes, 1, count, bak) != count) { ok = false; break; }
      ok = !ferror(old) && fflush(bak) == 0 && ok;
      ok = fsync(fileno(bak)) == 0 && ok;
      ok = fclose(bak) == 0 && ok;
    }
    fclose(old);
    if (!ok || rename(backup_temp, backup)) {
      remove(temp); remove(backup_temp); return false;
    }
  } else if (errno != ENOENT) { remove(temp); return false; }
  if (rename(temp, path)) { remove(temp); return false; }
  return true;
}
