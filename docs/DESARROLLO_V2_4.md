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

## Revisión 2.4.1 · Barra de entrada rápida

Versión 2.4.1, código 12. La tarjeta se sustituye por una barra alargada de 56 dp, con radio de 28 dp, texto en una sola línea y el icono real de la app a la izquierda. Se propone un espacio de cuatro columnas y una fila; el lanzador decide el espacio final. La altura de la barra se mantiene al modificar el espacio del widget.

`RemoteViews` no soporta `EditText` ni subclases personalizadas de sus vistas admitidas. Por tanto, el widget no simula que ya se puede escribir en el escritorio: pulsarlo abre `EventoRapidoActivity`, una Activity normal con fondo transparente y una entrada de texto enfocada con teclado. No utiliza ventanas del sistema ni permiso de superposición.

La ventana contiene nombre, fecha, hora, recordatorio y Guardar/Cancelar. Al guardar se cierra sin abrir la agenda ni alterar la navegación previa de `MainActivity`. Tiene una tarea independiente excluida de Recientes y reutiliza la instancia si se vuelve a pulsar la barra, conservando el borrador. Los datos se siguen guardando con `AgendaViewModel` y `RepositorioAgenda`. El control de permisos se comparte con el editor completo en `PermisoAvisos`.

Se conserva el destino anterior `agenda/nuevo` para los accesos de widgets que todavía contengan un PendingIntent previo. Las pruebas anteriores se mantienen junto a nuevas pruebas de foco, entrada rápida, validación, recreación, canal bloqueado y guardado sin abrir la agenda.

El logo adaptativo de Android se muestra con `ImageView` en ambas barras: `painterResource` de Compose no admite XML de iconos adaptativos. No se redibuja el logo ni se crean nuevas imágenes aproximadas.

La base de datos sigue en versión 8 y no se añaden permisos. Las capturas reales incluyen `widget-agenda.png`, `widget-agenda-ancho.png`, `widget-agenda-estrecho.png` y `widget-entrada-rapida.png`.

Limitación de Android y lista de controles admitidos: [RemoteViews](https://developer.android.com/reference/android/widget/RemoteViews).
