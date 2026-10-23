# Mindrop 2.0.0 · memoria de desarrollo

## Base y reglas

Base: commit 79f0f2c, versión 1.1.1 (code 4), Room 5. Se conservan applicationId,
firma, repositorios, jerarquías, iconos privados y HorizontalPager.
El código nuevo usa nombres españoles; no se renombra masivamente código antiguo.

## Fases implementadas (23/10/2026)

- A: inspección realizada. UI → ViewModel → Repository → Room, migraciones 1→5,
  62 tests por variante. No hay suite androidTest ni imagen de referencia adjunta.
- B: implementados tema RPG, formas recortadas, gráficos Canvas y cuatro destinos.
- C: implementadas apertura con destellos, rebote, aparición de tarjetas, confirmación de
  edición, celebración al completar y celebración de logros. Animaciones finitas y
  ajuste para reducir movimiento; no se altera el HorizontalPager.
- D: implementados calendario mensual, eventos por fecha, CRUD, enlace opcional y
  confirmación de descarte/borrado. Tests de datos superados.
- E: implementados avisos locales de eventos/ideas, cancelación/reprogramación,
  redescubrimiento optativo, horario de silencio, permisos y receptor del sistema.
  Workers comprobados con Robolectric; funcionamiento real tras reiniciar pendiente.
- F: implementados 22 logros, contadores históricos deduplicados, rachas y conservación
  de desbloqueos. La actividad histórica no registrada no se inventa.
- G: implementadas 11 recompensas entre temas/fondos, diseños y efectos. Se valida el
  bloqueo en el repositorio y se conserva la selección en DataStore.
- H: integración automática comprobada; validación manual en Android pendiente.
- I: generada APK 2.0.0, code 5, firmada con la clave existente. Tag y Release publicados
  posteriormente por petición explícita del usuario; detalles al final de esta memoria.

## Decisiones aplicadas

Los recursos pixel art se dibujan en Canvas a partir de cuadrículas pequeñas, sin motor
gráfico ni descargas. El texto de lectura conserva una tipografía normal.

Room 5→6 añade tablas independientes para agenda, recordatorios, logros, acciones
históricas y revisiones de ideas; no reconstruirá ni borrará las tablas antiguas.
DataStore guarda preferencias cosméticas y de notificaciones.

Los avisos son aproximados mediante trabajos únicos de WorkManager (Android puede
retrasarlos por ahorro de batería). No se pide acceso a alarmas exactas. Los eventos
guardan fecha/hora local y los avisos se recalculan tras cambios de zona horaria o reloj.

Actividad válida: crear/editar contenido, validar sugerencias o completar una idea.
Cada fecha cuenta una sola vez. Aperturas y cambios cosméticos no cuentan. No se
inventan rachas anteriores. Se rechazan retrocesos y saltos grandes del reloj para
la racha; una aplicación offline no puede impedir toda manipulación local.

## Validaciones y estado

- `assembleDebug`: correcto.
- `assembleDebugAndroidTest`: correcto; se incorporan 3 tests instrumentados (pager,
  interacción sin movimiento y permisos). No se afirma que se hayan ejecutado.
- `connectedDebugAndroidTest`: intentado; no puede ejecutar la suite y termina con
  `DeviceException: No connected devices!` (limitación del entorno, no fallo de un test).
- `test`: 75/75 en debug y 75/75 en release; 0 fallos, 0 errores, 0 omitidos.
- `lint`: 0 errores, 17 avisos sobre actualizaciones disponibles de herramientas y
  dependencias. Corregidos los avisos de API 26 en Ajustes y de validación del receptor.
- `exportReleaseApk`: correcto. APK en `app/build/releases/Mindrop-v2.0.0.apk`,
  aproximadamente 8,91 MiB; firma válida, mismo certificado que v1.1.1.
- APK inspeccionada con aapt: applicationId conservado, versión 2.0.0, code 5,
  API mínima 23. Sin INTERNET, permisos de red, almacenamiento amplio ni alarmas exactas.
- Migraciones comprobadas desde esquemas 1, 2, 3, 4 y 5 hacia Room 6. El test 5→6
  conserva texto personal, carpeta, subidea, completada y actualización numerada.
- Tests de creación/edición/borrado/vinculación de eventos; deduplicación de avisos;
  cancelación al completar; destino de PendingIntent; exclusión de ideas completadas,
  revisadas o sugeridas recientemente; racha de 365 días y cambio irregular del reloj;
  bloqueo/desbloqueo de recompensas y persistencia tras reabrir DataStore y Room.

## Problemas del entorno resueltos o documentados

- ANDROID_HOME apuntaba a C:/Android/Sdk y ANDROID_SDK_ROOT a otro SDK. Las
  compilaciones se ejecutaron haciendo coincidir ambas variables en el proceso, sin
  modificar la configuración global del usuario.
- El emulador de prueba MindropV2QA no arranca por falta de aceleración de hardware.
  También se intentó el modo software; no llegó a aparecer ningún dispositivo en adb.
  No se instala ni cambia el hipervisor del equipo.
- La especificación menciona una imagen de referencia, pero el archivo recibido solo
  contiene texto. El diseño sigue la paleta y las reglas escritas, sin comparación visual
  con una imagen no disponible.
- Las bibliotecas nativas de AndroidX se empaquetan sin stripping al faltar herramientas
  NDK de depuración. No impide la compilación ni la firma.

## Continuación exacta: comprobaciones manuales pendientes

El código no contiene pantallas de relleno ni funcionalidades a medio conectar. Antes
de publicar una Release, completar en un dispositivo compatible:

1. Instalar v1.1.1, crear datos de prueba e instalar encima v2.0.0 firmada, sin desinstalar.
2. Revisar visualmente Inicio/Agenda/Logros/Ajustes, temas desbloqueados, textos largos,
   teclado, rotación y contraste. Comparar con la imagen de referencia si se proporciona.
3. Ejecutar `./gradlew connectedDebugAndroidTest` con el dispositivo conectado.
4. Probar avisos reales con permiso concedido/denegado, app cerrada, ahorro de batería,
   reinicio, cambio de hora/zona, edición y eliminación del evento, y cancelación de idea.
5. Revisar que completar/restaurar no rompe subideas, sugerencias ni el pager.

`adb install -r app/build/releases/Mindrop-v2.0.0.apk` permite probar la actualización.
No usar la APK debug sobre una instalación release: la firma debug es diferente.
Los avisos no son exactos y Android impide ejecutarlos mientras la app está forzada a
detenerse; se reconcilian cuando vuelve a abrirse. No publicar como versión final sin
estas comprobaciones. La publicación posterior se realizó por petición explícita del
usuario y comunica las comprobaciones físicas pendientes en las notas de la Release.

## Publicación autorizada de v2.0.0

- Tag anotado `v2.0.0` sobre el commit verificado `817f38e647f1474cb38cc3702d46de63785fe3e0`.
- Release: [Mindrop v2.0.0 — RPG Edition](https://github.com/manuel-tharsis/Mindrop/releases/tag/v2.0.0).
- Archivo adjunto: `Mindrop-v2.0.0.apk`, 9.346.828 bytes. GitHub confirma estado `uploaded`.
- SHA-256 local y remoto coinciden:
  `caeb86893c8aee9dd823b77742df668b6614b365b6316a186e1b9461aa38a26f`.
- Firma v1/v2/v3 válida, misma clave que v1.1.1; no se han modificado releases anteriores.
- La revisión física pendiente no se presenta como realizada.
