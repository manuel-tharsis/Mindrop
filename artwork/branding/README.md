# Icono de Mindrop

Diseño definitivo elegido: **Diseño 2, trazo fluido con unión de color ondulada**.

`mindrop-md-fluido.png` es una copia exacta del PNG aprobado. No se redibuja ni se cambia su color. Las letras forman una gota continua sobre fondo transparente.

`tools/prepare_launcher_icon.py` genera las densidades de Android a partir de ese original, con Pillow como dependencia de desarrollo. Únicamente recorta márgenes por alfa, redimensiona proporcionalmente y añade margen transparente. El original aprobado permanece intacto. `launcher-source.json` registra la huella del original y los parámetros.

Hay recursos PNG para Android 6/7 y primer plano adaptativo desde Android 8, con el mismo alfa para iconos temáticos. El widget y la entrada rápida de eventos comparten los recursos del lanzador y reciben el nuevo icono automáticamente.

Los iconos de carpetas, ideas, proyectos y packs no cambian.
