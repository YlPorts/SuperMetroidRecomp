# Super Metroid Android 0.2.0

Continuación de Alpha 2, rama `android-v0.2.0`, sobre
`57b4a2fabcec973e07f62e8e9089b970abcbe6dd` (`android-ultrawide`).
La rama anterior y su APK permanecen intactas. No se sustituye el juego.

## Implementado

- Inicio directo al juego cuando ya hay una ROM importada y verificada; tras
  importar por primera vez también se abre el juego. El selector de Android
  sigue siendo necesario cuando todavía no hay ROM.
- Menú dentro del juego, sin launcher adicional en cada arranque. Categorías
  Pantalla, Controles, Audio y Rendimiento.
- Controles compactos, letras SNES opcionales e iconos alternativos. Se
  conservan las posiciones anteriores y la asignación original del juego.
- Editor: arrastrar para mover; tocar para tamaño individual, opacidad, radio
  táctil y zona muerta de cruceta. Guardado inmediato y restablecimiento por
  botón o completo. Se muestran las áreas táctiles durante la edición.
- IDs de dedos independientes, diagonales y liberación al cancelar, perder
  foco, abrir un menú o cambiar de tamaño. Ocultación con mando físico SDL.
- Filtrado nearest obligatorio, escala vertical entera opcional, ajuste al
  ancho útil del teléfono y protección frente a recortes/barras del sistema.
- Modo conservador ultrawide: las transiciones, pausa, mapas y cinemáticas
  fuera del estado de juego 8 usan el raster original 4:3. El escenario se
  amplía durante juego normal con el renderer existente. No se escribe en la
  cámara, la activación de enemigos o el mapa de colisiones.
- Presentación original o ahorro a 30; simulación con tiempos SNES intactos.
  Se desactiva la interpolación Android. El único reloj de cadencia evita una
  espera VSync adicional. Esto no certifica FPS en un teléfono físico.
- Volumen general en vivo y búfer 512/1024/2048/4096 al siguiente inicio.
  AAudio con alternativas SDL, salida 48 kHz, misma lógica SPC700.
- Pausa nativa explícita: el propietario del motor procesa los comandos,
  guarda SRAM y duerme sin ejecutar el juego mientras hay un menú o la app
  está en segundo plano. La reanudación reinicia la cadencia de presentación.
- SRAM sucia revisada cada 120 cuadros. Solo se escribe si cambió; no se
  crean estados completos periódicos. Reemplazo atómico y copia `.bak`
  conservando el archivo actual hasta publicar el nuevo. Exportación `.srm`
  mediante el selector moderno de documentos de Android.
- Guardado y carga rápida ejecutados por el motor, con resultado real de las
  funciones de snapshot y mensajes de error cuando fallan.
- Diagnóstico bajo demanda: cuadros/s del motor, CPU del juego, vaciados de
  audio, muestras perdidas, descartes y ocupación del FIFO. No hay overlay
  de depuración permanente.
- Workflows de comprobación sin ROM y compilación ARM64 jugable con entrada
  privada del propietario. Pruebas nativas, JVM, lint y MotionEvents reales
  mediante una instrumentación Android sin dependencias de terceros.

## Arquitectura conservada

`snesrecomp` y `recomp-ui` mantienen sus commits fijados en Alpha 2. Se
regeneran las funciones 65816 en C y se ejecutan las variantes restantes
mediante el puente LLE existente. PPU, SPC700/DSP, DMA, HDMA, registros y
mapeado siguen en el hardware SNES emulado del proyecto. El renderer
ultrawide lee capturas inmutables; se conserva la caché de VRAM de Alpha 2.
Las extensiones de ciclo de vida se aplican a una copia del host durante
CMake; el submódulo compartido no se modifica y el parche falla si el host
fijado ya no coincide.

Se recuperaron APK, firma original y ROM aportada por el propietario. La
identidad comprobada es `com.ylports.supermetroid`, certificado SHA-256
`3162b5fb52ddd1606e211c3d9f54010dd114d210eefab178c1536e2d7cdf25af`.
La APK anterior es versión interna 2; esta usa versión interna 20.

## Límites y pendientes

- No se certifica haber corregido todos los sprites, paletas, transparencias,
  jefes o salas. Son necesarios recorridos y comparaciones de toda la partida.
- El modo conservador limita la ampliación en transiciones. Se conserva la
  cámara original: no se ha creado una cámara nueva ni un mapa panorámico.
- Música y efectos comparten la mezcla original SPC; no se ofrecen controles
  separados sin una clasificación fiable de voces y eco.
- No se inventan resolución interna arbitraria, backend Vulkan ni VSync
  adicional. El raster se mantiene a 224 líneas y la textura se presenta con
  nearest; hay opciones reales de aspecto y escala entera.
- SRAM compatible y exportable. Los snapshots mantienen el formato existente,
  pero su compatibilidad en todas las posiciones necesita pruebas extensas.
- Pendientes de hardware A15: audio audible, Bluetooth/háptica, latencia al
  tacto, temperatura/batería, sesiones largas y FPS por zona. No se afirma
  60 FPS estables ni se usa un dispositivo virtual para atribuirlos al A15.
- El workflow jugable necesita los secretos privados indicados en
  `android/README.md`. El workflow sin ROM genera una aplicación identificada
  `.buildcheck` que no ejecuta el juego y no se entrega como APK jugable.
