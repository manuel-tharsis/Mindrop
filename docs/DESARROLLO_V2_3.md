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
