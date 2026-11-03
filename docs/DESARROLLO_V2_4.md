# Mindrop 2.4.0 · Widget de agenda

Versión Android 2.4.0, código 11. El cambio añade un acceso desde el escritorio para crear un evento en la agenda local de Mindrop.

## Funcionamiento

- `AgendaWidget` usa `AppWidgetProvider` y `RemoteViews`, sin dependencias nuevas en la aplicación ni actualización periódica.
- La tarjeta usa los archivos aprobados del calendario y el botón de añadir del pack Básico. Puede ensancharse horizontalmente.
- Un `PendingIntent` explícito e inmutable abre `MainActivity` con el destino `agenda/nuevo`. Su URI es diferente de las notificaciones para que no se sustituyan entre sí.
- La navegación coloca la agenda debajo del formulario. Guardar vuelve a ella y selecciona la fecha guardada, aunque sea de otro mes. Cancelar conserva la navegación anterior.
- Si el formulario del widget ya está abierto, volver a pulsar el widget conserva el mismo borrador.
- Se reutilizan `EditorEvento`, `AgendaViewModel`, `RepositorioAgenda`, Room y el programador de notificaciones existente.
- El formulario propone la siguiente hora completa, incluso al cambiar de día o año, y un aviso a la hora del evento. Todos los valores pueden modificarse.
- El borrador y la fecha inicial usan estado guardable de Compose. Salir con cambios requiere confirmar su descarte.

## Notificaciones

Guardar con un aviso comprueba el permiso y si el canal está bloqueado. Cuando corresponde solicita el permiso de Android; al concederlo continúa el guardado. Si está bloqueado, se muestran instrucciones y acceso a los ajustes. También puede elegirse Sin recordatorio.

Se ha corregido la detección del canal desactivado para no marcar un aviso como enviado sin poder mostrarlo. Android anterior a 8 utiliza el sonido predeterminado; las versiones posteriores conservan el canal existente y sus preferencias del usuario.

Los avisos siguen siendo aproximados mediante WorkManager, sujetos a ahorro de batería y ajustes del teléfono. No se añade un despertador, pantalla completa ni permiso de alarma exacta. No se escribe en calendarios externos.

## Datos y permisos

La base de datos conserva la versión 8. No hay cambios de esquema ni migraciones. Los eventos y los recordatorios usan las tablas existentes. No se añaden permisos, acceso a Internet o servicios externos.

## Verificación

Las pruebas añadidas cubren:

- Pulsación de la tarjeta y destino de su PendingIntent.
- Declaración del widget y ausencia de permisos nuevos.
- Hora futura por defecto, medianoche y cambio de año.
- Navegación y restauración de la pila de pantallas.
- Creación desde la app cerrada y con la app ya abierta.
- Conservación de un borrador tras recrear la Activity y volver a pulsar el widget.
- Confirmación al descartar y ausencia de guardado accidental.
- Canal bloqueado y guardado explícito sin recordatorio.
- Persistencia de evento y aviso tras cerrar y abrir Room.
- Envío de una única notificación y acceso posterior a la agenda.
- Representación real del widget en dos anchos y del formulario de Compose.

Comprobación completa: `assembleDebug assembleDebugAndroidTest test lint exportReleaseApk`. Las capturas reales se generan en `app/build/reports/visual-qa/widget-*.png`.

No se ha probado en un lanzador o dispositivo físico porque no hay uno conectado. El tamaño final del widget, la interfaz de permisos y el sonido dependen de Android y del lanzador del teléfono.

Referencia de la API de widgets: [Android Developers](https://developer.android.com/develop/ui/views/appwidgets).
