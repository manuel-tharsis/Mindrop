# Mindrop 2.6.1 · Refinamiento visual

- Detalle con Atrás y Editar visibles; Recordatorio y Eliminar pasan al menú. Se conserva la confirmación antes del borrado.
- Bandera de prioridad junto al título del detalle. Las listas muestran únicamente las banderas amarillas; el menú de cada tarjeta conserva la acción de cambiar importancia.
- Nombre de la página visible junto al indicador del detalle: Descripción, Actualización N o Sugerencias.
- Acciones de tarjetas con zonas de pulsación de 48 dp, márgenes uniformes y un margen final desplazable de 112 dp para poder situar la última tarjeta por encima del botón de añadir, sin recortar la altura de la lista.
- Navegación con selección más clara y etiquetas de 12 sp. Tipografía de lectura de 16 sp e interlineado de 25 sp.
- Cuaderno más ancho, márgenes interiores de 20 dp y separación uniforme entre texto e imágenes. Las acciones utilizan el acento del tema.

Se conservan los archivos originales de los packs, las cinco secciones, el estado de los formularios, Room y Android mínimo 6. No hay cambios de datos, permisos ni dependencias. Se distribuye como versión 2.6.1 (código 15), conservando la firma de las versiones anteriores y sin reemplazar la release 2.6.0.

Verificación: assembleDebug, testDebugUnitTest y lint. Capturas nativas de inicio, detalle estrecho con texto grande y cuaderno. Pruebas de interacción de los nuevos menús y conservación de la acción de prioridad.

La suite debug superó 197 pruebas. Se recompiló la APK de tests instrumentados y se repitieron las pruebas de menús y capturas tras los ajustes finales. No se ha realizado una comprobación en teléfono físico.

Corrección del inicio: eliminado el margen fijo de 80 dp del contenedor, que causaba una franja vacía detrás del FAB. Prueba de regresión con 1 y 40 ideas: la lista ocupa la zona inferior y Completadas puede desplazarse por encima del botón.
