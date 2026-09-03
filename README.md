# Mindrop

Mindrop es una aplicación Android personal y completamente offline para guardar ideas y organizarlas en carpetas y subcarpetas.

## Funciones

- Crear, editar y eliminar ideas.
- Guardar un nombre, una descripción corta y una descripción completa.
- Elegir iconos predeterminados o una imagen propia como icono.
- Crear, editar y eliminar carpetas vacías.
- Organizar carpetas en varios niveles.
- Mover ideas y carpetas a otra carpeta o a la raíz.
- Navegar mediante el botón Atrás y una ruta de carpetas.
- Buscar ideas dentro de la ubicación actual.
- Conservar toda la información en el almacenamiento local del dispositivo.

## Tecnologías

- Kotlin.
- Jetpack Compose.
- Material 3.
- Room.
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

Desde la raíz del proyecto, ejecuta:

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

En macOS o Linux:

```bash
./gradlew assembleDebug
```

La APK instalable se genera en `app/build/outputs/apk/debug/app-debug.apk`.

Mindrop no solicita el permiso `INTERNET` y no utiliza cuentas, servidores, analíticas ni servicios externos.
