# Mindrop

Mindrop es una aplicación Android personal y completamente offline para guardar ideas, organizarlas y llevar una agenda, con estética RPG de 16 bits, logros y recompensas cosméticas. La versión 2.1.0 añade ideas importantes y cuadernos de proyectos.

## Funciones

- Crear, editar y eliminar ideas.
- Guardar un nombre, una descripción corta y una descripción completa.
- Elegir iconos predeterminados o una imagen propia como icono.
- Crear, editar y eliminar carpetas vacías.
- Organizar carpetas en varios niveles.
- Mover ideas y carpetas a otra carpeta o a la raíz.
- Navegar mediante el botón Atrás y una ruta de carpetas.
- Buscar ideas de cualquier carpeta, incluidas las completadas, y páginas de proyectos por su título o texto.
- Conservar toda la información en el almacenamiento local del dispositivo.
- Añadir sugerencias a una idea y convertirlas en actualizaciones numeradas al validarlas.
- Relacionar ideas y subideas en varios niveles sin sustituir su organización por carpetas.
- Marcar ideas como completadas y restaurarlas después en su carpeta original.
- Marcar ideas como importantes mediante una bandera y un borde amarillo, sin cambiar su orden.
- Crear, renombrar y eliminar proyectos con cuadernos propios.
- Escribir páginas de título opcional y texto libre, con guardado automático.
- Deslizar entre páginas, acceder al índice, cambiar su orden y recuperar la última página visitada.
- Eliminar páginas con confirmación; un cuaderno siempre conserva una hoja vacía si se borra la última.
- Consultar un calendario mensual y crear, editar o eliminar eventos, vinculados opcionalmente a ideas.
- Programar recordatorios de eventos o ideas mediante notificaciones locales.
- Activar el redescubrimiento periódico de ideas antiguas, con horario de silencio.
- Conseguir logros persistentes y desbloquear temas, diseños de carpetas y efectos.
- Personalizar el aspecto y reducir las animaciones desde Ajustes.
- Deshacer durante el aviso el borrado de una idea o página.
- Ajustar el tamaño del texto del cuaderno y elegir papel claro u oscuro.
- Exportar y restaurar copias locales del contenido, el progreso y los iconos desde Ajustes.

Los avisos son aproximados: Android puede retrasarlos por ahorro de batería. Para recibirlos hay que permitir las notificaciones. El redescubrimiento viene desactivado. La racha cuenta días con cambios reales de contenido; abrir la aplicación no cuenta.

## Copias locales

En **Ajustes → Copias de seguridad**, exporta un archivo ZIP mediante el selector de archivos de Android. La copia incluye ideas, carpetas, sugerencias, actualizaciones, proyectos, páginas, agenda, recordatorios, progreso e iconos personalizados. Los ajustes de lectura y notificaciones del dispositivo se conservan al restaurar.

Restaurar una copia **sustituye el contenido actual** y requiere confirmación. El archivo se valida antes de aplicar el reemplazo en una transacción de Room. Guarda primero otra copia si quieres conservar el contenido actual. No se fusionan datos.

El archivo contiene datos privados y no está cifrado: guárdalo en un lugar de confianza. La app no lo sube a ningún servicio. El formato actual admite copias de esta versión de Room (7), hasta 32 MiB de datos de texto y 128 MiB descomprimidos, con iconos de hasta 256 × 256 píxeles.

Tras borrar una idea o página aparece **Deshacer**. El borrado definitivo se aplaza hasta que termina el aviso; si el proceso se cierra antes, el contenido se conserva. No es una papelera permanente.

## Descargar APK

Mindrop no se publica en Google Play Store. La APK instalable puede descargarse desde la sección **Releases** del repositorio de GitHub.

En el móvil, descarga el archivo de la versión deseada, ábrelo y permite la instalación desde esa fuente si Android lo solicita.

## Tecnologías

- Kotlin.
- Jetpack Compose.
- Material 3.
- Room.
- Preferences DataStore y WorkManager.
- Navigation Compose.
- Coroutines y Flow.
- Gradle Kotlin DSL.

## Ejecutar el proyecto

1. Abre la carpeta del proyecto con una versión reciente de Android Studio.
2. Instala el Android SDK 36 si Android Studio lo solicita.
3. Sincroniza el proyecto con Gradle.
4. Selecciona un dispositivo físico o un emulador y ejecuta la configuración `app`.

El proyecto requiere JDK 17 o posterior. Puede utilizarse el JDK integrado en Android Studio.

## Generar APK

La firma release se configura localmente mediante `keystore.properties`. Usa `keystore.properties.example` como plantilla y guarda el keystore fuera del control de versiones.

Conserva una copia privada y segura del keystore y de sus credenciales: serán necesarios para firmar futuras actualizaciones de la misma aplicación.

Desde la raíz del proyecto, ejecuta:

En Windows:

```powershell
.\gradlew.bat exportReleaseApk
```

En macOS o Linux:

```bash
./gradlew exportReleaseApk
```

La APK instalable se genera en `app/build/releases/Mindrop-v2.1.0.apk`.

Mindrop no solicita el permiso `INTERNET` y no utiliza cuentas, servidores, analíticas ni servicios externos.

## Desarrollo

La arquitectura y las reglas de persistencia se describen en [docs/ARQUITECTURA_V2.md](docs/ARQUITECTURA_V2.md). La memoria de fases, validaciones y limitaciones está en [docs/DESARROLLO_V2.md](docs/DESARROLLO_V2.md).

Los cambios de prioridad y cuadernos están documentados en [docs/DESARROLLO_V2_1.md](docs/DESARROLLO_V2_1.md).

Las mejoras de búsqueda, copias, deshacer, lectura y acabado visual están en [docs/MEJORAS_USABILIDAD.md](docs/MEJORAS_USABILIDAD.md).

Comprobaciones locales: `./gradlew assembleDebug test lint`. Los tests instrumentados se ejecutan con `./gradlew connectedDebugAndroidTest` en un dispositivo/emulador conectado.
