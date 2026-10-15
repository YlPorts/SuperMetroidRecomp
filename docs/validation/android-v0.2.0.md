# Super Metroid Android 0.2.0 — validación y estado de entrega

Estado: APK ARM64 de desarrollo empaquetada y verificada tras recuperar las herramientas. Se conserva el motor restaurado; el experimento HDMA rechazado no está incluido. Esta entrega no se declara estable ni validada en el Galaxy A15.

APK: `SuperMetroid-0.2.0-arm64.apk`, 49.911.200 bytes.
SHA-256: `9dc015b23967f6f0138658ab83171efc0524409bbf1536a35a75c1d3c6abffa0`.

## Trabajo guardado en esta rama

Base exacta: Alpha 2, commit `57b4a2fabcec973e07f62e8e9089b970abcbe6dd`. La rama `android-ultrawide` y la APK anterior se conservan.

Se implementaron arranque directo tras importar la ROM; ajustes por categorías; controles compactos con letras SNES opcionales; editor individual de posición, tamaño, opacidad, área táctil y zona muerta; manejo multitáctil por ID; liberación al cancelar y perder foco; escala nítida, anclajes y protección de recortes; modo 4:3 conservador en transiciones; presentación de 30 o cadencia original con tiempos SNES intactos; volumen general y búfer para el próximo inicio; pausa explícita del motor; SRAM sucia, reemplazo atómico y copia anterior; exportación de SRAM; directorio interno consistente si falla el volumen externo; diagnóstico bajo demanda y tema de arranque oscuro.

Se mantiene `com.ylports.supermetroid` y la configuración de firma original. La compilación Android declara versión 0.2.0, código 20 y ARM64. Los commits de las dependencias y la arquitectura original se conservan.

## Resultados comprobados

| Prueba | Resultado y límite |
| --- | --- |
| Regeneración | Dos generaciones originales idénticas. Tras rechazar un cambio experimental se restauraron 348 archivos de generación/declaraciones: todos sus SHA-256 coinciden con la salida original verificada |
| Compilación nativa | El motor completo ARM64 restaurado enlazó correctamente con NDK 28.2. También se compiló la variante x86_64 de pruebas |
| Java y recursos | Compilación correcta; lint sin errores |
| JVM | Multitáctil, ocho direcciones, teclas compartidas, migración INI e importación válida/inválida con y sin cabecera |
| Pruebas C | SRAM atómica, fallo de escritura conservando el archivo anterior, geometría, reloj de presentación a 30/60 y otras tasas, Mode 7, ventanas PPU, renderer, VRAM inmutable, overlays y gestos |
| Audio automático | Consumidor y FIFO reales: arranque, tasas, recuperación, rampas y aislamiento del productor. Cinco regresiones del analizador de audio de puertas |
| Android 15/API 35 | View de producción con MotionEvents reales: cuatro dedos, cambio de índices, botón compartido, deslizamiento, cancelación, editor persistente y cambio de tamaño. Pasó localmente y en GitHub |
| Comparación visual | Doce capturas Mode 1: cero diferencias en los 49.152 píxeles centrales bajo el HUD de cada captura frente al raster PPU del propio framework |
| Galaxy A15 | Sin acceso físico: no se midieron FPS, reproducción audible, latencia ni consumo en el SM-A155M |
| APK entregada | Gradle `assembleRelease` y `lint` correctos; paquete original, versión 0.2.0/código 20, ARM64, no depurable, certificado original, firma v2 y alineación ELF/ZIP de 16 KiB. Sin archivos ROM ni claves privadas |
| Identidad del motor | El `libmain.so` extraído de la APK coincide byte por byte con la biblioteca ARM64 restaurada y preparada para distribución: SHA-256 `cc1012e0f47f332f743f28312a234849cbf16b393c8be2dbdf1a106b1c570c5a`. `verify-apk.py --engine-library` permite repetir esa comprobación y rechazar un empaquetado obsoleto |

La comparación usa el mismo cuadro de dos renderizadores del proyecto. No es una comparación contra una SNES física y no certifica todas las salas, márgenes o efectos.

## Fallos y cambios rechazados

El recorrido acelerado, sin salida de audio, detectó `unresolved-abandon` en `$88:D865` y límites/timeouts posteriores del puente LLE. La rutina contiene argumentos inline tras un JSL. Una prueba con `force_lle` produjo corrupción y detenciones más tempranas: se rechazó y se restauraron exactamente los bancos originales.

Este fallo no se declara corregido. Alcanzar un contador de 18.000 cuadros no se contó como estabilidad satisfactoria porque el contador seguía avanzando tras una detención. Falta verificar el recorrido completo y la salida de audio en un dispositivo real.

Se repitió el recorrido con cadencia normal y el dispositivo de audio SDL dummy a 48 kHz/1024 muestras. Completó 4.000 cuadros en 69,39 segundos y salió correctamente al límite, pero reprodujo `unresolved-abandon` en `$88:D865` en el cuadro 2.659. Por ello tampoco se cuenta este recorrido como una prueba de fidelidad aprobada. La captura del cuadro 2.800 muestra la nave en el exterior.

`tools/check_engine_run.py --log recorrido.log --frames 4000 --report resultado.json` rechaza este registro aunque el proceso haya devuelto 0: comprueba el final real y los avisos conocidos de abandono, watchdog y puente. Se ejecutó contra el registro real y devolvió 1 por el abandono en el cuadro 2.659. No sustituye las pruebas visuales, de partida o de audio.

La sonda de audio registró vaciados: entre los cuadros 501–1.500 no hubo nuevos; entre 1.501–2.500 hubo 7 y entre 2.501–4.000 hubo 34. El arranque también presentó vaciados. Son mediciones de este host con audio dummy y carga de compilación concurrente; no demuestran cortes audibles en el teléfono ni que el sonido esté resuelto. No se ha cambiado la velocidad de música o simulación para ocultarlos.

La repetición con el mismo búfer y sin compilación local simultánea completó otros 4.000 cuadros en 69,43 segundos. Registró 7, 3 y 40 nuevos vaciados en esos mismos tres intervalos, respectivamente. Por tanto no se atribuye el problema únicamente a la carga de compilación. Sigue pendiente la escucha y medición en un dispositivo real.

El emulador local sin KVM mostró ANR/reinicios de servicios y fallos FUSE. Las pruebas de View pasaron, pero ese entorno no certifica juego Android completo, rendimiento ni calidad audible.

## GitHub Actions

La ejecución de comprobación del código restaurado aprobó las dos tareas: regresiones y compilación/instrumentación Android:

https://github.com/YlPorts/SuperMetroidRecomp/actions/runs/38739742770

Ambas tareas aprobaron sobre el código `8787b9c1712b1951de2b4f9dde711a219356ec0d`; la actualización posterior de este informe no cambia el motor ni la aplicación.

El artefacto `NO-GAME` de esa ejecución es una aplicación aislada `.buildcheck`, no el juego. No debe instalarse esperando ejecutar Super Metroid.

El workflow `Android ARM64 playable release` regenera desde la ROM del propietario, compila el juego y comprueba la firma original. Requiere `SM_ROM_URL`, `SM_KEYSTORE_BASE64`, `SM_ANDROID_STORE_PASSWORD` y `SM_ANDROID_KEY_PASSWORD`, como explica `android/README.md`. No se publicó la ROM ni la firma privada en el repositorio.

## Pendientes

1. Resolver las fronteras HDMA/puente detectadas y completar recorridos prolongados con audio y reloj normales.
2. Probar partida nueva, Ceres, jefes, puertas, ascensores, mapas y todas las habilidades. La lógica se conserva, pero no se certifica cada mecánica en esta entrega.
3. Guardar/cargar en distintas salas y verificar interrupciones reales del ciclo de vida.
4. Escucha, tacto, mandos, temperatura y FPS en el Galaxy A15. No se afirma 60 FPS estables.
5. Ampliar las comparaciones externas, Mode 7 real, transparencias y salas ultrawide.
6. La música y los efectos siguen compartiendo la mezcla SPC: no hay sliders separados ficticios.

Registro de cambios: [android-v0.2.0.md](../releases/android-v0.2.0.md).
