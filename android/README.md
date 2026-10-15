# Super Metroid Recomp · Android

Port de la base `0da320f`, con el motor y el renderizador ultrawide del proyecto.
La interfaz del teléfono y los controles se implementan en Java; la ejecución
del juego, audio y gráficos siguen en el motor C compartido. No se incluyen ROMs.

## Uso

1. Abre la app; se orienta en horizontal automáticamente.
2. **Elegir ROM** abre el selector de archivos de Android. Importa Super Metroid
   (Japan, USA), original, en `.sfc` o `.smc`. Se admite cabecera de 512 bytes;
   se elimina y se comprueba el SHA-256 antes de guardar. No elijas un ZIP.
3. **Jugar**. El modo inicial ajusta el escenario al ancho del teléfono.
4. El botón **≡** abre guardar/cargar estado rápido, pantalla, controles y salida.

La pantalla ofrece 4:3, 16:9, 21:9, 32:9 y ajuste a la pantalla. El renderizador
del repositorio dibuja los márgenes; no se estira el raster original. El HUD
puede situarse en los extremos. Algunas pantallas y salas conservan limitaciones
del renderizador experimental del proyecto original.

Cruceta de ocho direcciones con zona muerta; B saltar, Y disparar, A correr,
X seleccionar arma, L/R apuntar, Select y Start. Los nombres describen el mapa
predeterminado del juego; si lo cambias dentro del juego, sus funciones cambian.
Se puede caminar, disparar y saltar a la vez. Cada dedo se identifica por su ID;
soltar uno no libera lo que mantiene otro. Se sueltan las teclas al cancelar un
gesto, abrir el menú o salir de la app. Hay tamaño, opacidad, vibración opcional
y editor de posición. Se ocultan los controles con mando físico y reaparecen
al desconectarlo; el menú queda disponible.

Los archivos y partidas se conservan dentro de la app entre actualizaciones.
Salir desde el menú permite al motor guardar su SRAM y cerrar. Desinstalar o
borrar los datos elimina las partidas. La app no solicita acceso a toda la
memoria ni permisos de Internet.

## Compilar el juego

JDK 17, Android SDK 36, Build Tools 36.0.0, NDK 28.2.13676358, CMake y Ninja
en PATH. Para la regeneración utiliza las dependencias Python/Rust descritas
por `snesrecomp`; conserva los commits fijados de los submódulos.

```sh
export ANDROID_HOME=/ruta/al/android-sdk
export JAVA_HOME=/ruta/al/jdk-17
# Firma propia y estable: no guardes las contraseñas en el repositorio.
export SM_ANDROID_KEYSTORE=/ruta/privada/supermetroid.jks
export SM_ANDROID_STORE_PASSWORD=...
export SM_ANDROID_KEY_ALIAS=supermetroid
export SM_ANDROID_KEY_PASSWORD=...
bash android/build-port.sh /ruta/a/SuperMetroid.sfc
```

`build-port.sh` verifica la ROM, genera todos los bancos dos veces y exige
salida idéntica, prueba los controles y compila `assembleRelease`. Sin firma
configurada el resultado es `app-release-unsigned.apk`. Para desarrollo:
regenera primero y después ejecuta `cd android && ./gradlew assembleDebug`.

La arquitectura inicial es ARM64, Android 9 o posterior. Se usa SDL 2.32.8
verificado por SHA-256, con su Java correspondiente. NDK 28 y el enlace de
16 KiB preparan las bibliotecas para dispositivos de páginas grandes.
No hay promesa de 60 fps en un teléfono sin probar el juego allí.

## Verificación sin ROM

```sh
bash android/test-core.sh
cd android
./gradlew -PandroidVerifyOnly=true :app:assembleDebug
```

La segunda orden verifica Java, recursos, JNI, SDL y el motor compartido usando
el setup host oficial, que **no ejecuta el juego**. Tiene un paquete separado
`com.ylports.supermetroid.buildcheck`; el botón Jugar queda deshabilitado.
**No entregar este APK como port jugable**. La compilación normal continúa
fallando explícitamente si falta `src/gen/`. No se añaden stubs de juego ni se
sustituye la recompilación por otro emulador.

La validación de importación prueba rechazo de tamaños/hashes incorrectos,
lecturas acotadas y conservación de la ROM anterior. Para probar también la
importación correcta, con y sin cabecera, pasa tu ROM a la prueba:

```sh
bash android/test-core.sh /ruta/a/SuperMetroid.sfc
```

Para una prueba en un emulador Android x86_64, después de regenerar:

```sh
cd android
./gradlew -PsmAbis=x86_64 :app:assembleDebug
```

El APK normal usa ARM64. El caso positivo de importación, la regeneración
completa reproducible y el enlace del juego en ambas arquitecturas se han
verificado con la ROM fijada. El script prepara `recomp/funcs.h` antes de
generar, ya que el emisor también lo usa para resolver los alias del host.
En Android 15 se comprobó el selector de ROM, el título, una escena en
ultrawide, el menú táctil y la creación del estado rápido. Es una prueba de
arranque; falta una partida completa y validar estados en distintas salas.
Las pruebas de rendimiento, audio y tacto en un teléfono físico siguen pendientes.
## Alpha2: audio y controles

La actualización `0.1.0-alpha2` se compila con `assembleRelease`, versión 2 y
la misma firma que alpha1. Puede instalarse encima conservando ROM, partidas
y posiciones de los controles. La APK entregada es ARM64 y no es depurable.

Se eliminó la espera duplicada de presentación y se recuperan retrasos cortos
de planificación para alimentar el audio. Se prefiere AAudio, con fallback de
SDL, salida estéreo a 48 kHz y un bloque de dispositivo de 2.048 muestras.
El renderer ultrawide reutiliza la VRAM inmutable entre líneas hasta que la
PPU registra una escritura. No cambia el reloj ni el estado de la consola.

Los botones usan símbolos vectoriales: flecha de salto, mira para disparar,
doble flecha de carrera, proyectil para cambiar arma, miras diagonales,
pausa, selector y menú. No contienen letras ni nombres. Se conservan el
multitáctil, deslizamiento, tamaño, opacidad y posiciones guardadas.

Pruebas: captura de 1.000 cuadros de 1.257 a 22 ms (solo esa etapa); ocho
escenas y una captura anterior conservan sus píxeles. Un replay sintético
de 3.000 cuadros con el consumidor de audio real pasa de 56 vaciados a 0.
Esto no mide FPS ni reproducción audible en un teléfono físico.

## Versión 0.2.0

La continuación parte de la Alpha 2 exacta, conservada en `android-ultrawide`.
Tras importar la ROM, cada apertura entra directamente al juego. `≡` o Atrás
abre el menú. En **Editar controles**, arrastra para mover y toca un botón
para ajustar tamaño, opacidad y área táctil; la cruceta también permite
ajustar su zona muerta. El botón ✓ termina la edición.

Los ajustes tienen cuatro categorías. **Pantalla** permite 4:3, ultrawide,
HUD en extremos, escala vertical entera y modo conservador para transiciones.
**Audio** ofrece volumen general y búfer para el próximo inicio; conserva
la mezcla SPC700 original. **Rendimiento** puede reducir las presentaciones
a 30 sin cambiar la simulación SNES. El diagnóstico solo se abre a petición.
**Exportar partida SNES** guarda la SRAM como `.srm` donde elijas. No es un
estado de suspensión; conserva el progreso guardado según las reglas del juego.

### GitHub Actions

- **Android checks**: se ejecuta al publicar esta rama. Compila la aplicación
  aislada `.buildcheck`, prueba C/JVM/lint y ejecuta MotionEvents sobre la View
  real en Android 35. El artefacto `NO-GAME` es solo verificación.
- **Android ARM64 playable release**: ejecución manual, genera dos veces el C
  desde la ROM verificada y compila la APK de juego firmada. Conserva cada
  ejecución y su artefacto hasta 90 días.

Configura en GitHub → Settings → Secrets and variables → Actions:

| Secreto | Contenido |
| --- | --- |
| `SM_ROM_URL` | URL HTTPS privada que descarga la ROM original sin cabecera de 512 bytes |
| `SM_KEYSTORE_BASE64` | Contenido base64 de la firma original, nunca un archivo del repositorio |
| `SM_ANDROID_STORE_PASSWORD` | Contraseña del almacén original |
| `SM_ANDROID_KEY_PASSWORD` | Contraseña de la clave original |

El alias fijado es `supermetroid`. La verificación final exige el certificado
original; evita distribuir accidentalmente una APK con firma distinta.
Los archivos privados se descargan al directorio temporal del runner y no
se adjuntan como artefactos ni se incluyen en Git.

Validación local adicional:

```sh
bash android/fetch_sdl.sh
bash android/test-native.sh
bash android/test-core.sh /ruta/SuperMetroid.sfc
python3 android/verify-apk.py android/app/build/outputs/apk/release/app-release.apk --sdk "$ANDROID_HOME"
```

El registro de cambios y las limitaciones están en
[`docs/releases/android-v0.2.0.md`](../docs/releases/android-v0.2.0.md).
