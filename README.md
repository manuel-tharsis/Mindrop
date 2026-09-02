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

La base local contiene entidades y repositorios separados para carpetas e ideas, DAO reactivos con `Flow`, una migración de esquema v1 a v2, un ViewModel y un destino de navegación mínimo. Las pantallas de gestión y edición se implementarán en fases posteriores.

Las carpetas admiten jerarquía recursiva sin ciclos. Solo se pueden borrar carpetas sin subcarpetas; sus ideas se conservan y pasan a la raíz.

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
