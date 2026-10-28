# Mejoras de uso y acabado RPG

Se mantiene la arquitectura UI → ViewModel → Repository → Room y el funcionamiento sin Internet. No se añaden dependencias ni permisos. La base sigue en Room 7: estas mejoras no cambian su esquema.

## Funciones

- Búsqueda global desde Inicio: ideas activas y completadas de cualquier carpeta, y títulos/textos de páginas. Muestra la ubicación y permite abrir directamente la idea o la página encontrada. Conserva la consulta al recrear la pantalla. Hasta 100 resultados por tipo; trata `%`, `_` y `\` literalmente y admite mayúsculas castellanas.
- Copias ZIP mediante el selector de documentos de Android, desde Ajustes. Incluyen las once tablas de contenido y progreso e iconos privados. Los ajustes del dispositivo permanecen. La restauración sustituye el contenido y requiere confirmación; no fusiona datos.
- La copia usa una instantánea transaccional, sin copiar archivos SQLite abiertos. Se valida en una base temporal antes del reemplazo transaccional real. Se comprueban claves foráneas, ciclos, imágenes, versiones y rutas. Los iconos se copian con nombres nuevos antes del cambio; un error previo conserva los datos originales. Se preservan las secuencias de identificadores y se invalidan las revisiones de avisos anteriores.
- Borrado recuperable de ideas y páginas durante el aviso «Deshacer». El contenido se oculta mientras se aplaza el borrado físico. Deshacer conserva las filas originales, sugerencias, vínculos e iconos. Si muere el proceso antes del borrado definitivo, el contenido permanece. La última página deja una hoja vacía al confirmar el borrado. Eliminar proyectos completos conserva su confirmación y no se puede deshacer.
- Lectura del cuaderno: tamaños 16, 18, 20, 22 y 24, y papel claro/oscuro; preferencias persistidas en DataStore.

## Interfaz

Tipografía más legible, mayor separación entre títulos y descripciones, títulos largos de dos líneas en tarjetas, bordes más discretos, iconos de navegación uniformes y estados vacíos de proyectos. El cuaderno conserva el protagonismo, con papel cálido u oscuro y mayor interlineado. Al editar se ocultan los controles de pasar página para dejar espacio al teclado. Se conserva la bandera amarilla a la derecha sin etiqueta visible.

## Verificación y límites

Las pruebas nuevas cubren exportación/restauración completa, jerarquía y sugerencias, iconos sin archivo original, corrupción y rutas incorrectas, claves foráneas, persistencia al reabrir Room, avisos, deshacer y orden de páginas, búsqueda literal y castellana, selección de una página desde búsqueda y persistencia de preferencias.

El ZIP no está cifrado y es privado. Admite el formato 1 / Room 7, hasta 100.000 registros aproximadamente, 32 MiB de JSON y 128 MiB descomprimidos, con iconos de hasta 256 × 256 píxeles. Las copias de versiones futuras se rechazan explícitamente. Si un icono ya no existe al exportar, se conserva su icono predeterminado.

La comprobación visual, el teclado y la recreación completa de Activity en Android real siguen pendientes: no hay un dispositivo conectado y el emulador existente no ha arrancado, tampoco sin aceleración. Se compilan los tests instrumentados de navegación, edición, desplazamiento y lectura; no debe confundirse su compilación con su ejecución.

Comprobaciones locales: builds debug y release firmada correctas; 110 tests correctos por variante (debug y release), con 24 pruebas nuevas. Lint sin errores y con los 17 avisos existentes de versiones de dependencias. Se corrigieron el retorno intermitente a una página anterior tras añadir una hoja y la ordenación de páginas mientras existe un borrado pendiente.

No se publica una nueva GitHub Release con estos cambios.
