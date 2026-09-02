# Mindrop

Aplicación Android personal y completamente offline para guardar y organizar ideas en carpetas y subcarpetas.

## Tecnologías

- Kotlin
- Jetpack Compose y Material 3
- Room
- Navigation Compose
- Gradle Kotlin DSL

## Arquitectura

El proyecto mantiene un flujo sencillo: `UI -> ViewModel -> Repository -> Room`.

La base inicial contiene el modelo local, los DAO, el repositorio, un ViewModel y un destino de navegación mínimo. Las pantallas de gestión y edición se implementarán en fases posteriores.

## Compilación

Se necesita Android SDK 36 y un JDK compatible con Gradle/AGP; se recomienda JDK 21. En Android Studio, selecciónalo como Gradle JDK. Desde PowerShell también puede indicarse antes de compilar:

```powershell
$env:JAVA_HOME = "C:\ruta\al\jdk-21"
```

Desde la raíz del proyecto:

```powershell
.\gradlew.bat assembleDebug
```

La aplicación no declara el permiso `android.permission.INTERNET` ni integra servicios remotos.
