# Super Metroid Android 0.2.0 — validación y estado de entrega

Estado: código de desarrollo publicado. La APK final no se pudo empaquetar y guardar al desconectarse el entorno local de ejecución. El error fue `409 environment_offline: Environment is not connected`. No se ofrece una APK intermedia como si fuera la entrega definitiva.

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
| APK final | Empaquetado y guardado bloqueados por la desconexión del entorno. Las verificaciones de firma/alineación de builds intermedias no se presentan como una verificación de la entrega final |

La comparación usa el mismo cuadro de dos renderizadores del proyecto. No es una comparación contra una SNES física y no certifica todas las salas, márgenes o efectos.

## Fallos y cambios rechazados

El recorrido acelerado, sin salida de audio, detectó `unresolved-abandon` en `$88:D865` y límites/timeouts posteriores del puente LLE. La rutina contiene argumentos inline tras un JSL. Una prueba con `force_lle` produjo corrupción y detenciones más tempranas: se rechazó y se restauraron exactamente los bancos originales.

Este fallo no se declara corregido. Alcanzar un contador de 18.000 cuadros no se contó como estabilidad satisfactoria porque el contador seguía avanzando tras una detención. Falta reproducir con cadencia y audio normales y verificar el recorrido completo.

El emulador local sin KVM mostró ANR/reinicios de servicios y fallos FUSE. Las pruebas de View pasaron, pero ese entorno no certifica juego Android completo, rendimiento ni calidad audible.

## GitHub Actions

La ejecución de comprobación del código restaurado aprobó las dos tareas: regresiones y compilación/instrumentación Android:

https://github.com/YlPorts/SuperMetroidRecomp/actions/runs/38697000851

El artefacto `NO-GAME` de esa ejecución es una aplicación aislada `.buildcheck`, no el juego. No debe instalarse esperando ejecutar Super Metroid.

El workflow `Android ARM64 playable release` regenera desde la ROM del propietario, compila el juego y comprueba la firma original. Requiere `SM_ROM_URL`, `SM_KEYSTORE_BASE64`, `SM_ANDROID_STORE_PASSWORD` y `SM_ANDROID_KEY_PASSWORD`, como explica `android/README.md`. No se publicó la ROM ni la firma privada en el repositorio.

## Pendientes

1. Empaquetar, firmar, verificar y hacer disponible la APK definitiva desde el código restaurado.
2. Resolver las fronteras HDMA/puente detectadas y completar recorridos con audio y reloj normales.
3. Probar partida nueva, Ceres, jefes, puertas, ascensores, mapas y todas las habilidades. La lógica se conserva, pero no se certifica cada mecánica en esta entrega.
4. Guardar/cargar en distintas salas y verificar interrupciones reales del ciclo de vida.
5. Escucha, tacto, mandos, temperatura y FPS en el Galaxy A15. No se afirma 60 FPS estables.
6. Ampliar las comparaciones externas, Mode 7 real, transparencias y salas ultrawide.
7. La música y los efectos siguen compartiendo la mezcla SPC: no hay sliders separados ficticios.

Registro de cambios: [android-v0.2.0.md](../releases/android-v0.2.0.md).
