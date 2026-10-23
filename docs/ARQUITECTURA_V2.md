# Arquitectura y reglas de Mindrop RPG

## Estructura

Se conserva UI → ViewModel → Repository → Room. Los módulos nuevos no sustituyen los
repositorios de ideas. Sus acciones llaman a `ProgresoRpg` dentro de la misma transacción:
si una operación falla, también se deshace su progreso. Los componentes visuales no
asignan logros ni modifican directamente contadores.

- `ui/rpg`: Canvas, tema, pantallas de logros/ajustes y ViewModel de preferencias.
- `ui/agenda`: calendario, formulario y ViewModel del CRUD de eventos.
- `notifications`: programación, Workers y receptor de cambios del sistema.
- `data/rpg`: catálogo finito y evaluación de métricas.
- `data/preferences`: DataStore, preferencias cosméticas y frecuencia/silencio.
- `data/local`: migración 5→6, entidades y DAO adicionales.

## Migraciones

La cadena completa es 1→2→3→4→5→6. La nueva migración solo crea tablas e índices:

| Tabla | Uso |
| --- | --- |
| eventos | Título, descripción, fecha ISO local, minuto del día opcional, idea y aviso opcionales |
| recordatorios | Aviso de evento o idea, fecha/hora local, revisión y envío reclamado |
| logros | Clave, instante de desbloqueo y celebración ya presentada |
| acciones_rpg | Claves únicas para deduplicar primeras creaciones, validaciones y completados |
| metricas_rpg | Contadores históricos, racha y anclajes de reloj |
| revisiones_ideas | Última visita y último redescubrimiento de cada idea |

Borrar una idea desvincula sus eventos, conserva las subideas y elimina sus recordatorios
directos. Borrar un evento elimina su recordatorio. Las carpetas solo pueden borrarse
vacías. Completar una idea conserva folderId y parentIdeaId, y cancela su aviso directo.
La entrada verde Completadas continúa siendo virtual.

## Logros y actividad

Catálogo fijo de 22 logros y 11 recompensas, sin monedas ni servicios remotos.
Al inicializar v2 se calculan los datos históricos disponibles (ideas, carpetas, subideas,
completadas y sugerencias validadas). No se inventan eventos ni actividad de días pasados.
Los logros históricos se marcan como ya celebrados para evitar una secuencia de ventanas
al actualizar. Los nuevos logros muestran una celebración cerrable una sola vez.

Crear ideas/carpetas/eventos suma una vez por identificador. Completar, restaurar y volver
a completar la misma idea no multiplica el contador. Validar varias veces una sugerencia
no multiplica el progreso. Las métricas y logros ya obtenidos no disminuyen al borrar
contenido. Profundidad incluye la idea raíz como nivel 1 y conserva el máximo histórico.

Actividad válida: crear o editar una idea/evento/carpeta, añadir o validar una sugerencia,
completar/restaurar una idea. Una fecha local cuenta una sola vez. Lecturas, aperturas,
preferencias cosméticas y notificaciones no cuentan. La racha se reinicia después de
un día sin actividad; se conserva la racha máxima. El reloj de pared se contrasta con
el monotónico para rechazar saltos mientras permanece el mismo arranque; los retrocesos
no suman. Offline no se puede impedir toda manipulación, especialmente entre reinicios.

## Notificaciones

WorkManager utiliza trabajo único por id de recordatorio y una revisión de fila. El
Worker vuelve a comprobar la existencia, revisión y estado antes del envío. Reclama
transaccionalmente el envío para evitar duplicados; si Android termina el proceso
justo después de reclamarlo, ese aviso puede perderse (se prioriza no duplicarlo).

Los avisos atrasados más de 24 horas se descartan al reconciliar para no inundar al
usuario al volver a encender el teléfono. Los avisos recientes se reprograman al abrir
la aplicación. WorkManager conserva sus trabajos tras reiniciar; el receptor también
reconcilia tras BOOT_COMPLETED, MY_PACKAGE_REPLACED, TIME_SET y TIMEZONE_CHANGED.
Se guardan horas locales; si cambia la zona horaria se conserva la hora local elegida.
En cambios de horario de verano, java.time aplica la resolución de zona estándar.

Redescubrimiento: desactivado inicialmente; sondeo único diario, frecuencia 1/3/7/14
días, una notificación como máximo por intervalo, sin ideas completadas, al menos
7 días sin revisión y 14 días antes de repetir una idea. La ventana de silencio es
configurable (22:00–09:00 por defecto). No usa red ni alarmas exactas.

Permisos: POST_NOTIFICATIONS (Android 13+), RECEIVE_BOOT_COMPLETED y WAKE_LOCK
(WorkManager). No se solicita INTERNET, almacenamiento amplio, ubicación, alarmas
exactas ni acceso a cuentas. La aplicación no usa workers en primer plano.

## Visuales y rendimiento

Canvas dibuja cuadrículas pequeñas con recursos locales: carpetas, cofres, runas,
trofeo/estrella, fondo y partículas. No hay motor ni recursos descargados. Las
animaciones son finitas (90–650 ms), solo están activas cuando se utilizan y respetan
la reducción de movimiento. Los temas mantienen texto normal y alto contraste.
Las listas utilizan componentes lazy y claves estables. El cálculo de profundidad
memoriza ancestros; el redescubrimiento consulta con un JOIN local una vez al día.

DataStore guarda la selección; RepositorioLogros comprueba el desbloqueo antes de
aplicarla. Temas: Clásico, Bosque, Nocturno, Cyberpunk y Dorado. Diseños: carpeta,
cofre y runa. Efectos: chispa, magia y partículas doradas. Todo es cosmético.

## Dependencias y firma

Se añaden Preferences DataStore 1.1.7, WorkManager 2.10.0 y desugaring java.time
para API 23. Los tests usan work-testing y Compose UI Test. No se actualiza toda la
cadena de herramientas. La firma sigue en keystore.properties local y la clave
existente; ambos permanecen excluidos de Git. Versión 2.0.0, code 5.
