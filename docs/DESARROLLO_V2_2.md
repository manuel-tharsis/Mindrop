# Mindrop 2.2.0 - cuadernos con imágenes y PDF

Versión Android 2.2.0, código 7. Se conserva la arquitectura y no se añaden servicios online ni permisos. Se incorpora únicamente AndroidX ExifInterface 1.4.2 para leer la orientación de las fotos de forma compatible, sustituyendo la API antigua desaconsejada por lint. La biblioteca se ejecuta localmente: [referencia oficial](https://developer.android.com/jetpack/androidx/releases/exifinterface).

## Datos y migración

Room pasa de 7 a 8 mediante `MIGRATION_7_8`. Solo se crea `imagenes_pagina`, con id, página, ruta privada y orden. Su clave foránea elimina las filas al borrar su página. Las ideas, sugerencias, carpetas, proyectos y páginas anteriores no se recrean ni destruyen. Se mantiene la cadena de migraciones desde Room 1.

Las fotos y los bocetos se conservan en `files/imagenes_cuaderno/`. El selector moderno de Android concede acceso a la imagen elegida y se copia inmediatamente; no se depende de la URI original. Se respeta la orientación EXIF y la proporción, se reduce el lado mayor a 1600 píxeles y se genera JPEG sin metadatos. Entrada máxima: 32 MiB / 150 megapíxeles. Cada imagen privada ocupa como máximo 3 MiB. Los bocetos se guardan en PNG de 1024 × 640, con un lápiz de un color, deshacer y limpiar.

La creación del archivo y su vínculo se coordinan para evitar que una cancelación elimine un archivo ya vinculado. Si falla el alta en Room, se elimina el archivo preparado. Borrar una imagen requiere confirmación. Borrar una página o proyecto limpia sus archivos; durante «Deshacer» de la página se conservan las imágenes. Los originales seleccionados del teléfono no se eliminan.

## Interfaz y estado

Los controles de editar, nueva página, imagen y dibujo se colocan encima de la hoja, con botones de 48 dp y descripciones accesibles. Las flechas quedan debajo junto al estado de guardado. El cuaderno ocupa el espacio restante y conserva el desplazamiento vertical, el tamaño de letra y el papel oscuro.

El dibujo pendiente reside en un ViewModel, de modo que se conserva al rotar la Activity. Salir con trazos sin guardar pide confirmación. La página destinataria de una foto se recuerda mientras está abierto el selector. El texto pendiente se guarda antes de importar o añadir un boceto.

## PDF

Cada tarjeta de Proyectos incluye un botón PDF. Se toma una instantánea transaccional del cuaderno y se genera un archivo temporal privado con `PdfDocument`, `Canvas` y `StaticLayout`. Después se abre el selector de documentos de Android para elegir dónde guardarlo.

Las páginas se exportan en su orden, con título, texto y todas sus imágenes. Cada página del cuaderno comienza en una hoja A4 nueva; el texto largo continúa por líneas completas y conserva saltos de línea. Las imágenes mantienen la proporción y pasan a otra hoja si no caben. Los campos vacíos y las imágenes ausentes se manejan sin fallos; estas últimas dejan un aviso en el PDF y al terminar. La letra de lectura y el papel oscuro no alteran la salida sobre papel blanco.

El nombre se sanea y se limita a 100 caracteres, con extensión PDF. Hay un límite de 1000 hojas físicas para evitar exportaciones desmesuradas. Si falla la generación, se borra el temporal y no se entrega un archivo incompleto. Al cancelar el selector no se modifica el cuaderno. Si falla la escritura, se conserva el temporal para elegir otra ubicación. La ruta preparada se recuerda con SavedStateHandle para no abrir dos veces el selector al recrear la Activity. Los temporales antiguos se limpian al preparar otra exportación.

## Copias locales

El formato ZIP 1 ahora indica Room 8 e incluye la nueva tabla y las imágenes bajo `imagenes/`. Acepta también los ZIP de Room 7: se añade la tabla vacía a la instantánea antes de validarla, sin inventar imágenes. Se mantienen los límites de 32 MiB de JSON y 128 MiB descomprimidos.

La restauración valida rutas, dimensiones, contenido de las imágenes y claves foráneas en una base temporal. Copia los archivos con nombres nuevos y solo elimina los anteriores después del reemplazo transaccional. El tramo final no se interrumpe por una cancelación del ViewModel. Una imagen dañada o ausente en el ZIP no sustituye los datos actuales.

## Verificación

Se añaden pruebas de migración 7-8, importación, reducción, persistencia, dibujos, limpieza, deshacer con imágenes, restauración, copias anteriores, corrupción, exportación y recuperación del selector. Se actualizan todas las pruebas de migración anteriores para llegar a Room 8.

Se renderiza la pantalla real de Compose mediante Robolectric con gráficos nativos a 390 × 844 y 320 × 740 con letra ampliada. Las capturas se generan en `app/build/reports/visual-qa/`. El compositor del PDF se prueba con gráficos nativos y produce cinco hojas de muestra en `app/build/reports/pdf-qa/`, con texto largo, saltos, foto, boceto y una página vacía, para su revisión visual.

Robolectric no implementa `PdfDocument`, por lo que la apertura del archivo PDF real con `PdfRenderer` se comprueba en `ExportacionPdfTest`, un test instrumentado preparado para Android. No hay dispositivo conectado; esa prueba, el Photo Picker del sistema y las interacciones físicas de teclado/táctil siguen pendientes de ejecutarse en Android. Compilar esos tests no equivale a ejecutarlos.

No se crea un tag ni se publica una GitHub Release con este trabajo.

Resultado de la verificación local: builds debug y release correctas; 134 tests correctos por variante, con 24 pruebas nuevas; lint sin errores y con los 17 avisos anteriores de actualización de dependencias. Se compilan los ocho tests instrumentados. Además, el entorno de SDK muestra avisos de metadatos de sus herramientas y conserva dos bibliotecas nativas sin retirar símbolos; no impiden compilar y no requieren cambios de funcionalidad.
