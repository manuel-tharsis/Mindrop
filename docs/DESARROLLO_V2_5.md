# Mindrop 2.5.0 · Interfaz sobria

Versión 2.5.0, código 13. Revisión visual orientada a un aspecto más minimalista y con menos protagonismo de la presentación RPG. No se añaden funciones ni se modifica el modelo de datos.

## Cambios

- Fondo carbón y superficies oscuras con diferencias suaves, sin degradados o sombras intensas.
- Tipografía del sistema con títulos compactos, pesos moderados y mejor separación entre títulos, descripciones y datos secundarios.
- Radios y márgenes coherentes en tarjetas, campos, ajustes y diálogos.
- Inicio con encabezado más compacto y más espacio útil para las ideas. Carpetas en dos columnas desde 360 dp y una columna en pantallas más estrechas.
- Tarjetas de ideas con jerarquía discreta y menos indentación para conservar espacio de lectura. La prioridad sigue usando únicamente bandera a la derecha y amarillo.
- Navegación inferior con fondo diferenciado y selecciones contenidas. Conserva Inicio, Proyectos, Agenda, Logros y Ajustes, con las mismas rutas y restauración de estado.
- Botón circular de añadir común en Inicio, Proyectos y Agenda, con el archivo de icono correspondiente al pack elegido.
- Tarjeta de proyecto con el título separado de la acción PDF, para que los nombres largos tengan más espacio. Edición, borrado, apertura y exportación conservan sus acciones anteriores.
- Detalle de idea con encabezado más compacto y alineado a la izquierda. El recordatorio conserva su acceso mediante un icono de calendario con descripción accesible. La acción de completar utiliza un botón de contorno para no competir con la lectura.
- Ajustes y logros más discretos. Se mantienen progreso, desbloqueos, recompensas y opciones cosméticas. El término visual TRYHARD se sustituye por Reto avanzado.
- Pulsación y aparición más suaves, respetando la preferencia de reducir movimiento y los efectos elegidos.

Se conservan los archivos originales de todos los packs y los iconos personalizados. No se redibujan ni se cambian sus colores. El widget conserva su formato de barra y su funcionamiento.

## Alcance técnico

Los cambios se limitan a Compose, el tema compartido y componentes de presentación. Las acciones de navegación permanecen en el NavHost; el nuevo componente de la barra inferior solo comunica la selección. Los repositorios, ViewModels, Room, notificaciones y permisos no cambian. La base de datos sigue en versión 8, sin migración.

## Comprobaciones

- Capturas reales de Compose de inicio, vacío, personalización, cuaderno, proyectos y detalle.
- Pantallas de 320, 360 y 390 dp y texto aumentado para revisar saltos de línea, truncamiento y desplazamiento.
- Pruebas de interacción de la tarjeta de proyecto: exportar y editar no abren accidentalmente el cuaderno.
- Pruebas de las cinco entradas de navegación y su selección.
- Pruebas existentes de persistencia, jerarquías, migraciones, iconos y widgets.

Comando de comprobación: `assembleDebug assembleDebugAndroidTest test lint exportReleaseApk --max-workers=1`. Las capturas se generan en `app/build/reports/visual-qa/` y no se incluyen en Git.

Las capturas usan el pack Básico para mostrar la distribución. Los packs personalizados siguen aplicándose en el dispositivo. No hay un móvil conectado: la revisión visual se realiza con renderizado nativo de Android y Compose mediante Robolectric, y no sustituye la comprobación en un teléfono físico.
