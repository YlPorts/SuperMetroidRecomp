#pragma once
#include <stdbool.h>
#include <stddef.h>
bool SmAtomicReplace(const char *path, const void *data, size_t size);
