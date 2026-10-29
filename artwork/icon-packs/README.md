# Iconos aprobados de Mindrop

Estos archivos sustituyen los dibujos aproximados de las versiones 2.3.0 y 2.3.1.

- `sources/`: las tres láminas originales aprobadas, conservadas sin cambios.
- `originals/<pack>/`: 116 iconos recortados uno a uno, con transparencia y sin redibujar sus formas. Se mantienen los colores RGB de los píxeles originales.
- `prepared/<pack>/`: salida regenerable de la preparación, incluidas adaptaciones y variantes de color de Arcano. No se duplica en Git.
- `manifest.json`: origen, coordenadas de cada recorte, transformaciones y huellas de los archivos.
- `app/src/main/res/drawable-nodpi/pack_*.webp`: copias sin pérdidas para Android. El catálogo generado enlaza cada función con su archivo.

Las láminas no contenían todas las acciones para todos los packs. Para esas acciones se adaptan los colores de los archivos del pack Básico o se combinan sus imágenes; el manifiesto lo identifica como `adapted`, `shared` o `composite`. No se presentan como originales dibujados en esas láminas. La bandera y Completadas conservan sus colores de estado. El menú estándar de tres puntos permanece como icono del sistema.

Arcano mantiene los archivos de la lámina como variante predeterminada. Sus otras variantes cambian el acento de color manteniendo las formas. Los estados inactivos o de error pueden usar un tinte de estado; las imágenes normales conservan sus colores. Sobre papel claro se añade una base de contraste a la acción de eliminar.

La preparación se reproduce con `python tools/extract_icon_packs.py` desde la raíz del proyecto, con Pillow y NumPy disponibles. Son herramientas de desarrollo: la app no las necesita y no descarga imágenes. El script valida que cada WebP coincide sin pérdidas con su PNG preparado y reutiliza un solo recurso cuando varias opciones comparten exactamente la misma imagen. Las pruebas de Android comparan los originales con los recursos empaquetados; no necesitan ejecutar Python.

Los archivos de `originals/` son los maestros editables. La preparación normal los conserva y lleva sus cambios a los recursos de Android. Solo `--extract-originals` vuelve a recortarlos desde las láminas y sustituye esas ediciones.

Las imágenes se extraen a la resolución disponible de las láminas. No se aumenta artificialmente su detalle ni se afirma que existan originales vectoriales.
