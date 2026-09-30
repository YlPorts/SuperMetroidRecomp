#include "sm_renderer.h"
#include <assert.h>
#include <stdio.h>
#include <string.h>
static Ppu p, other;
static uint8_t ram[0x20000];
static uint32_t stock[256 * 224], output[SM_MAX_WIDTH * 224];

int main(int argc, char **argv) {
  SmRendererReset();
  SmRendererBeginFrame(ram, 1);
  for (unsigned y = 1; y <= 224; ++y) SmRendererCaptureLine(&p, y);
  assert(SmRendererEndFrame(stock) && SmRendererVramCopies() == 1);
  SmRendererBeginFrame(ram, 2);
  for (unsigned y = 1; y <= 224; ++y) {
    p.vram[0] = (uint16_t)y; ++p.vramWriteCount;
    SmRendererCaptureLine(&p, y);
  }
  assert(SmRendererEndFrame(stock) && SmRendererVramCopies() == 224);
  /* Different owners must not alias even with equal host revisions. */
  other.vramWriteCount = p.vramWriteCount; other.vram[0] = 0x1234;
  SmRendererBeginFrame(ram, 3);
  for (unsigned y = 1; y <= 224; ++y) SmRendererCaptureLine(y <= 100 ? &p : &other, y);
  assert(SmRendererEndFrame(stock) && SmRendererVramCopies() == 2);
  SmRendererReset();
  SmRendererBeginFrame(ram, 4);
  for (unsigned y = 1; y <= 224; ++y) SmRendererCaptureLine(&p, y);
  assert(SmRendererEndFrame(stock) && SmRendererVramCopies() == 1);
  if (argc > 1) {
    assert(SmRendererLoadCapture(argv[1]));
    assert(SmRendererDraw(output, (SmViewport){448,96,21.0/9,true}, true, 1));
    if (argc > 2) {
      FILE *f = fopen(argv[2], "wb"); assert(f);
      assert(fwrite(output, 448 * 224 * sizeof(*output), 1, f) == 1);
      assert(fclose(f) == 0);
    }
  }
  puts("VRAM snapshots: one stable copy, 224 raster uploads, owner change, reset and legacy capture passed");
  return 0;
}
