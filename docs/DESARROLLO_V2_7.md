# Mindrop 2.7.0

Versión 2.7.0, código 16. Android mínimo 6.0. Room permanece en versión 8, sin migraciones ni cambios de datos.

- Nuevo icono oficial: MD con trazo fluido y unión ondulada de dos tonos azules. Se conserva el PNG aprobado como original y se preparan sus densidades sin redibujarlo. Lo comparten el lanzador y el widget.
- Proyectos con portadas de cuaderno, curvas azules, nombre y número real de páginas. PDF y menú conservan sus acciones separadas de la apertura del cuaderno.
- Hoja con bordes suaves, contorno de papel y un acento discreto. Respeta papel oscuro y tamaño de lectura.
- Controles superiores agrupados y contador entre las flechas inferiores. El estado de guardado permanece visible.
- Índice desplegable con la página actual resaltada, selección directa y menús Subir, Bajar y Eliminar. El borrado mantiene su confirmación y Deshacer. Añadir página conserva la operación existente.
- Controles y navegación con vectores Android monocromos. Los PNG originales de los packs de ideas y carpetas permanecen intactos, incluidos los iconos personalizados.
- Corregido el inicio: el espacio para el FAB pertenece al final desplazable del contenido y no recorta el contenedor con una franja fija.

No se añaden campos, servicios, permisos, dependencias ni funciones de datos. Las páginas de los prototipos contienen texto y dibujos de muestra; la app muestra únicamente el contenido del usuario.

## Verificación

Pruebas de apertura y exportación de proyectos, selección de páginas, flechas, menús de movimiento, confirmación de borrado, creación desde el índice y regreso al cuaderno. Prueba del inicio con 1 y 40 ideas. Capturas nativas a 320 y 390 dp, con texto aumentado. Pruebas existentes de persistencia, migraciones, PDFs, iconos, widgets y recreación de Activity.

Comando final: `assembleDebug assembleDebugAndroidTest test lint exportReleaseApk --max-workers=1`.

Las capturas se generan en `app/build/reports/visual-qa/`. No hay un teléfono físico conectado; la revisión de interacción se realiza con Robolectric y renderizado nativo. Los tests instrumentados se compilan, pero no se han ejecutado en un dispositivo.

Resultados: 203 pruebas por variante (debug y release), sin fallos, errores ni omitidos. Lint: 0 errores, 17 avisos previos de dependencias y 1 sugerencia opcional sobre clasificación de accesos rápidos.
