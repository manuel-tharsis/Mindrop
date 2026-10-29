# Mindrop 2.3.0 - packs de iconos 2D

Versión Android 2.3.0, código 8. Se conserva Room 8: no cambian tablas, relaciones ni datos y no es necesaria una migración.

## Diseño

- Cinco packs para categorías, acciones y navegación: Básico, Aventurero, Bosque, Arcano y Cyberpunk.
- Dibujo vectorial local con formas sencillas, colores planos y pocos detalles. No se incluyen los catálogos de imágenes generados como recursos de producción.
- Un catálogo y un pintor comunes evitan que una pantalla olvide el estilo elegido. Las imágenes personalizadas continúan usando el cargador privado existente, con su alternativa predeterminada si el archivo falta.
- Arcano obtiene su acento del tema actual. La prioridad conserva la bandera derecha y el amarillo; las ideas completadas siguen siendo una entrada virtual verde.
- Básico sustituye al antiguo diseño pixelado. Las claves persistidas `clasico`, `cofre` y `runa` siguen siendo válidas; se muestran como Básico, Aventurero y Arcano. Los requisitos de los logros se conservan. Bosque y Cyberpunk usan los logros existentes de sus temas.
- Se añaden cinco categorías al selector: cuaderno, calendario, cámara, imagen y pincel. Son iconos decorativos; no añaden funciones de cámara ni nuevas clases de contenido.
- Las muestras de los packs incluyen varios iconos. Las de efectos muestran un fotograma visible y permiten una reproducción lenta y finita; respetan la reducción de movimiento.
- Tarjetas menos recargadas, esquinas suaves, márgenes ajustados e iconos proporcionados al texto. La hoja del cuaderno conserva su tamaño y sus controles.

## Compatibilidad

No se añaden dependencias, servicios, permisos ni conexión a Internet. Los desbloqueos y selecciones utilizan las preferencias existentes. Las fotos y los bocetos no se modifican.

## Comprobaciones

Los tests del catálogo y los packs comprueban las claves antiguas, el color de Arcano y los requisitos de desbloqueo. Las pruebas gráficas de Compose generan capturas nativas en `app/build/reports/visual-qa/`, incluidos los cinco packs y el inicio en pantalla estrecha con texto grande. Se conservan las pruebas de Room, copias, iconos personalizados, navegación y cuadernos.

La compilación, la batería completa de tests y lint se ejecutan antes de entregar la actualización. Las capturas de Robolectric verifican el dibujo real de Compose; no sustituyen una prueba táctil en un teléfono físico. La disponibilidad de pruebas instrumentadas depende de que haya un dispositivo conectado.

No se publica una GitHub Release como parte de esta implementación.

Resultados locales: 143 tests debug y 143 release aprobados, sin fallos ni errores. Las pruebas instrumentadas se compilan; no se ejecutan porque no hay un dispositivo conectado. Lint conserva únicamente los 17 avisos anteriores de versiones de herramientas o dependencias. Se corrigieron los atributos de recursos incompatibles con API 23 y se comprobó el contraste de las acciones sobre papel claro mediante una prueba gráfica. La APK utiliza la misma firma que la versión anterior y no contiene el permiso INTERNET.

## Refinamiento 2.3.1

Versión 2.3.1, código 9. La interfaz adopta la referencia visual aportada por el usuario: negro puro, paneles gris oscuro sin sombras, esquinas de 24 dp, encabezados amplios de peso normal y selección con contorno de acento. Se retiran las decoraciones pixeladas del fondo. Los temas conservan sus colores de acento y los packs se mantienen independientes; Arcano sigue el tema.

Los ajustes agrupan lectura, avisos y copias en paneles. Las opciones de aspecto conservan los mismos requisitos y pasan a tarjetas seleccionables accesibles con previsualización, indicador de estado y requisito de desbloqueo. No se incorporan funciones de sonido ni otras funciones de la aplicación de referencia.

Room, archivos privados y lógica de contenido no cambian. Se mantienen la bandera amarilla, Completadas verde y la opción de papel claro u oscuro. Las capturas nativas incluyen inicio, cuaderno y personalización en anchuras de 390 y 320 dp con texto grande.

Validación de 2.3.1: 145 tests por variante aprobados y compilación de las pruebas instrumentadas. No hay dispositivo conectado para ejecutar estas últimas. Lint conserva los 17 avisos anteriores de versiones; no aparecen errores. Se genera una APK firmada con el código de versión incrementado para actualizar la instalación existente. La foto de referencia se mantiene fuera de Git.

## Corrección de iconos 2.3.2

Versión 2.3.2, código 10. El usuario detectó correctamente que las versiones anteriores mostraban reconstrucciones geométricas simplificadas, en lugar de los dibujos aprobados. Se elimina ese pintor y se incorporan archivos de imagen extraídos de las láminas originales.

Se conservan las tres láminas sin cambios y se extraen 116 originales a carpetas por pack. Los recortes eliminan el fondo y las etiquetas; sus píxeles de color se conservan y se añade transparencia. No se regeneran las siluetas con IA. Las copias de Android son WebP sin pérdidas y hay un manifiesto con procedencia y huellas de cada archivo. La preparación, incluida la adaptación de acciones ausentes de las láminas y los colores de Arcano, está documentada en `artwork/icon-packs/README.md`.

Las imágenes normales conservan sus colores y sombreado. Solo se aplican los estados necesarios de prioridad inactiva, error y deshabilitado. La acción sobre papel claro utiliza una base de contraste. El botón principal usa la imagen original de Añadir, a tamaño apropiado para el botón.

Las pruebas nuevas verifican que todos los recursos se decodifican con transparencia, que los píxeles opacos coinciden con el PNG original y que Compose no repinta el original con el tinte de Material. Se vuelven a generar capturas de los packs y de las pantallas reales. Las imágenes idénticas se comparten sin duplicar recursos. Los PNG intermedios de preparación se regeneran y no se guardan en Git. Room, datos y permisos no cambian; no hay nuevas dependencias de Android.

Validación de 2.3.2: compilaciones debug y release correctas; 148 tests por variante aprobados; lint sin errores ni avisos nuevos, con los 17 avisos anteriores sobre versiones. Las pruebas instrumentadas se compilan y no se ejecutan al no haber un dispositivo conectado. Las dos láminas adjuntas por el usuario coinciden píxel a píxel con las fuentes conservadas. La generación normal conserva los maestros individuales editables y prepara sus copias para Android.
