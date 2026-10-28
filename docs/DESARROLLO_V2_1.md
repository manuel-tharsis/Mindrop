# Mindrop 2.1.0 · prioridad y cuadernos

## Alcance aprobado

Se implementa el diseño «Cuaderno enfocado». Cada proyecto tiene un cuaderno propio
con páginas de texto libre para separar ideas o partes de su desarrollo. Las páginas
no necesitan corresponder a entidades Idea. Los proyectos no sustituyen a las carpetas.

La prioridad de Idea es binaria: marcada o no marcada. En las tarjetas solo aparece la
bandera derecha y el borde amarillo cuando está activa, sin etiqueta «Importante».
Las descripciones de accesibilidad sí indican la acción de marcar/desmarcar.
No se reordena ninguna lista por prioridad y no se rompe la jerarquía de subideas.

## Datos y migración

Room pasa de 6 a 7 con `MIGRATION_6_7`:

- `ideas.es_importante INTEGER NOT NULL DEFAULT 0`: todas las ideas anteriores empiezan
  sin marcar. No se modifica el estado completada, carpeta, padre ni contenido.
- Tabla `proyectos`: id, nombre, última página visitada.
- Tabla `paginas_proyecto`: id, proyectoId, título, texto, orden. FK a proyectos con
  borrado en cascada e índice por proyectoId/orden.

La cadena 1→2→3→4→5→6→7 sigue registrada. No se utilizan migraciones destructivas,
no se reconstruye la tabla de ideas y no se cambian las preferencias de DataStore.
Los cambios de prioridad usan una actualización limitada a su columna: no alteran
fechas, orden, recordatorios ni métricas de logros.

## Cuaderno y guardado

- Crear proyecto crea también su primera página vacía en una sola transacción.
- El título de página es opcional; se muestra «Página N» si está vacío.
- HorizontalPager muestra una hoja cada vez; el texto se desplaza verticalmente.
- Durante la edición el swipe se desactiva para poder escribir/seleccionar texto.
  «Terminar edición» vuelve a la lectura. Las flechas son una alternativa accesible.
- El índice se abre a petición y permite seleccionar, subir, bajar o eliminar hojas.
- Las escrituras se agrupan tras 400 ms de inactividad y se serializan con Mutex.
  La interfaz muestra «Guardando…» hasta confirmar la escritura.
- Cambiar de hoja, añadir/reordenar/borrar o salir fuerza el guardado pendiente antes
  de completar la operación. Si falla, el borrador permanece en pantalla y se puede
  reintentar; la navegación de salida no se confirma hasta guardar.
- ON_STOP solicita guardar; la rotación conserva el ViewModel. SavedStateHandle
  guarda página seleccionada y modo de edición, sin introducir textos largos en Bundle.
- Se persiste la última página por proyecto en Room. Al borrar esa página se recuerda
  otra válida. El repositorio valida que una página pertenezca al proyecto.
- Borrar la última página conserva una nueva hoja vacía. Se confirma antes de borrar
  y el texto eliminado no se recupera de forma implícita.
- Borrar un proyecto confirma claramente que elimina todas sus páginas; las ideas,
  sus sugerencias, carpetas y eventos no se borran.

La barra principal incorpora Proyectos entre Inicio y Agenda. El cuaderno, como el
detalle de una idea, utiliza toda la pantalla con Atrás e índice en la barra superior;
salir siempre pasa por la confirmación de guardado del ViewModel.

## Archivos y dependencias

- `data/local/entity/EntidadesProyecto.kt`, `data/local/dao/ProyectoDao.kt` y
  `data/repository/RepositorioProyectos.kt`: persistencia y transacciones de páginas.
- `ui/proyectos/`: lista, formulario, cuaderno, índice y ViewModels.
- `ui/idea/BanderaPrioridad.kt`, `ic_flag.xml`: control reutilizable para listas,
  formulario y detalle. Completadas también conserva la marca.
- IdeaEntity/IdeaDao/IdeaRepository, formularios y listas se amplían con la prioridad.
- `ui/navigation/MindropNavHost.kt`: destinos nuevos y navegación principal.
- Esquema Room 7 exportado junto con los esquemas históricos.

No se incorporan dependencias, permisos ni servicios nuevos. La aplicación sigue sin
INTERNET, usuarios ni sincronización. Se conservan applicationId y la clave de firma.
Versión 2.1.0, versionCode 6.

## Validación

La batería añade pruebas de CRUD, orden, hojas vacías, textos largos con saltos de
línea, aislamiento entre proyectos, borrado, persistencia tras reabrir, prioridad y
autoguardado/guardado al salir/recuperación de la última página en ViewModel.

El test de migración 6→7 crea una base real con el esquema exportado de v2.0.0 y
comprueba conservación de ideas, subideas, icono privado, completadas, sugerencias,
actualizaciones, agenda, recordatorios y logros antes de crear un cuaderno nuevo.
Los tests históricos se actualizan para comprobar toda la cadena hacia Room 7.

Se añaden dos tests instrumentados para swipe/índice/confirmación de borrado y bandera
sin etiqueta. La revisión física sigue pendiente: adb no detecta dispositivos y el
hipervisor del emulador no está instalado. No se presenta esa comprobación como realizada.

No se publica una GitHub Release nueva automáticamente.

### Resultados ejecutados · 28/10/2026

- `assembleDebug`: correcto.
- `assembleDebugAndroidTest`: correcto; suite de 5 tests instrumentados compilada.
- `test`: 86/86 debug y 86/86 release; 0 fallos, errores u omitidos.
- `lint`: 0 errores, 17 avisos de actualizaciones disponibles de herramientas y dependencias.
- `exportReleaseApk`: correcto. `app/build/releases/Mindrop-v2.1.0.apk`, 9.413.768 bytes
  (8,98 MiB), versionName 2.1.0, versionCode 6, API mínima 23.
- Firma v1/v2/v3 verificada; certificado idéntico al de v2.0.0. Aapt confirma el
  mismo applicationId y la ausencia de INTERNET y de permisos adicionales.
- SHA-256: `46b872e42bd151270b18494804ce6f94ee5bfd18ad4e3f2d63d55113361ad706`.
- `connectedDebugAndroidTest`: intentado, termina con `No connected devices!`;
  no se ha ejecutado ningún test físico ni se afirma validación visual en Android.
- Se corrigió el lector del esquema usado en el test 6→7: los esquemas Room omiten
  `indices` en tablas sin índices. El fallo estaba en el preparador de la prueba,
  no en una migración ejecutada sobre datos del usuario.

Para probar la actualización sin desinstalar la versión anterior:
`adb install -r app/build/releases/Mindrop-v2.1.0.apk`.
Revisar teclado, swipe, edición al rotar y textos largos en un dispositivo; después
ejecutar `./gradlew connectedDebugAndroidTest`. No instalar la APK debug sobre la
release, porque la clave debug es distinta. Los cambios todavía no se han publicado
como nueva Release en GitHub.
