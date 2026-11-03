# Mindrop 2.6.0

Tres mejoras sobre la arquitectura existente, sin cambios en Room (versión 8) ni en los datos, permisos o iconos originales. Android mínimo: 6.0 / API 23. versionCode: 14.

- Transición compartida del icono y título entre tarjetas y detalle mediante SharedTransitionLayout de la versión estable de Compose ya utilizada. Fuera de navegación, los componentes y previews se dibujan normalmente. Se respeta Reducir movimiento y la desactivación de animaciones del sistema. Cuando no existe tarjeta de origen o destino, el detalle continúa funcionando sin transición compartida.
- Confirmación táctil después de validar una sugerencia (en detalle y editor) o completar una idea correctamente. El ViewModel emite un evento tras guardar; la vista solo responde si su ventana está activa. No se repite por observar Room, restaurar una idea o recrear la Activity. View.performHapticFeedback respeta los ajustes del sistema sin VIBRATE; utiliza CONFIRM desde API 30 y VIRTUAL_KEY en versiones anteriores.
- Dos accesos dinámicos desde el icono: Nueva idea y Nuevo evento. Se registran fuera del hilo principal desde API 25. Los intent explícitos conservan la tarea y el borrador; repetir el acceso no duplica el formulario actual. No se utilizan accesos estáticos porque Android fuerza CLEAR_TASK para su primer intent. El lanzador debe admitir accesos; aparecen después de abrir la app una vez. En API 23 no se cargan las clases del servicio de accesos.

No se añaden bibliotecas beta ni servicios online. La dependencia explícita de Compose Animation utiliza el BOM existente.

## Verificación

Pruebas automatizadas de registro de accesos, navegación y conservación de borradores, guardado en Room, confirmaciones táctiles, transiciones reales entre componentes y reducción de movimiento. Recreación de MainActivity en API 23 y API 36. Las pruebas de interfaz se ejecutan con Robolectric y renderizado nativo; no sustituyen una comprobación en un teléfono físico.

Validación final: compilación debug, APK de instrumentación y APK release correctas. 195 tests por variante (debug y release), sin fallos, errores ni omitidos. Lint: 0 errores, 17 avisos previos de actualización de dependencias y 1 sugerencia opcional sobre informar a Android del uso de accesos para mejorar su clasificación. No se incorpora seguimiento de uso.

Comando: `assembleDebug assembleDebugAndroidTest test lint exportReleaseApk --max-workers=1`. Las capturas de lista, transición, detalle e icono personalizado se generan en `app/build/reports/visual-qa/` y no se incluyen en Git.

## Límites

La sensación háptica y la presentación de accesos dependen del teléfono, sus ajustes y el lanzador. No se ha medido fluidez ni respuesta háptica en un móvil conectado. Se intentó iniciar el emulador de QA en modo temporal, pero no arrancó por falta del controlador de aceleración; los tests instrumentados se han compilado y no se han ejecutado en dispositivo. La transición compartida depende de que ambos elementos estén disponibles durante la navegación; si Room aún está cargando o la tarjeta desaparece, la navegación conserva su fundido sencillo.

No se crea tag ni GitHub Release en esta fase.
