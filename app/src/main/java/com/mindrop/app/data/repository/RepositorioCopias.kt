package com.mindrop.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mindrop.app.data.local.MindropDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Instantánea lógica: no copia una base abierta ni depende del archivo WAL de SQLite. */
class RepositorioCopias(private val contexto: Context, private val db: MindropDatabase,
    private val borrados: BorradosRecuperables? = null) {
    private val cerrojo = Mutex()
    private val directorioIconos get() = File(contexto.filesDir, "custom_icons").apply { mkdirs() }.canonicalFile

    suspend fun exportar(salida: OutputStream) = withContext(Dispatchers.IO) { cerrojo.withLock {
        comprobarBorrados()
        val temporal = File.createTempFile("copia-", ".zip", contexto.cacheDir)
        try {
            ZipOutputStream(temporal.outputStream().buffered()).use { zip ->
                db.withTransaction {
                    val sqlite = db.openHelper.writableDatabase
                    val tablas = volcar(sqlite)
                    val ideas = tablas.getJSONArray("ideas")
                    val iconos = mutableMapOf<String, String>()
                    var tamanoIconos = 0L
                    for (indice in 0 until ideas.length()) {
                        val idea = ideas.getJSONObject(indice)
                        if (idea.isNull("custom_icon_path")) continue
                        val ruta = idea.getString("custom_icon_path")
                        val archivo = File(ruta).canonicalFile
                        if (archivo.parentFile != directorioIconos || !archivo.isFile) {
                            idea.put("custom_icon_path", JSONObject.NULL)
                            continue
                        }
                        val nombre = iconos[ruta] ?: "iconos/${UUID.randomUUID()}.png".also { nombre ->
                            require(archivo.length() <= MAX_ICONO) { "Un icono supera el tamaño permitido." }
                            comprobarImagen(archivo)
                            tamanoIconos += archivo.length()
                            require(tamanoIconos <= 96L * 1024 * 1024 && iconos.size < 9999) { "La copia supera el tamaño permitido." }
                            zip.putNextEntry(ZipEntry(nombre))
                            archivo.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                            iconos[ruta] = nombre
                        }
                        idea.put("custom_icon_path", nombre)
                    }
                    val secuencias = JSONObject()
                    sqlite.query("SELECT name, seq FROM sqlite_sequence").use { c ->
                        while (c.moveToNext()) if (c.getString(0) in TABLAS) secuencias.put(c.getString(0), c.getLong(1))
                    }
                    val texto = JSONObject().put("aplicacion", "Mindrop").put("formato", 1)
                        .put("versionRoom", 7).put("tablas", tablas).put("secuencias", secuencias).toString()
                    comprobarEstructuraJson(texto)
                    val contenido = texto.toByteArray(Charsets.UTF_8)
                    require(contenido.size <= MAX_JSON) { "La copia supera el tamaño permitido." }
                    zip.putNextEntry(ZipEntry("mindrop.json")); zip.write(contenido); zip.closeEntry()
                }
            }
            temporal.inputStream().use { it.copyTo(salida) }
            salida.flush()
        } finally { temporal.delete() }
    } }

    /** Valida primero en otra base Room; el reemplazo real es una única transacción. */
    suspend fun restaurar(entrada: InputStream): Unit = withContext(Dispatchers.IO) { cerrojo.withLock {
        comprobarBorrados()
        val temporal = File(contexto.cacheDir, "restauracion-${UUID.randomUUID()}").apply { mkdirs() }
        val creados = mutableListOf<File>()
        var aplicada = false
        try {
            extraer(entrada, temporal)
            val archivo = File(temporal, "mindrop.json")
            require(archivo.isFile && archivo.length() <= MAX_JSON) { "No es una copia válida de Mindrop." }
            val texto = archivo.readText(Charsets.UTF_8)
            comprobarEstructuraJson(texto)
            val copia = JSONObject(texto)
            require(copia.getString("aplicacion") == "Mindrop" && copia.getInt("formato") == 1 && copia.getInt("versionRoom") == 7) {
                "La copia no es compatible con esta versión de Mindrop."
            }
            val tablas = copia.getJSONObject("tablas")
            val secuencias = copia.getJSONObject("secuencias")
            validarContenido(tablas)
            val ideas = tablas.getJSONArray("ideas")
            val rutas = mutableMapOf<String, String>()
            for (i in 0 until ideas.length()) {
                val idea = ideas.getJSONObject(i)
                if (idea.isNull("custom_icon_path")) continue
                val nombre = idea.getString("custom_icon_path")
                require(PATRON_ICONO.matches(nombre)) { "La copia contiene una referencia de icono incorrecta." }
                val fuente = File(temporal, nombre)
                require(fuente.isFile) { "Falta un icono en la copia." }
                comprobarImagen(fuente)
                val destino = rutas.getOrPut(nombre) {
                    File(directorioIconos, "${UUID.randomUUID()}.png").also { nuevo ->
                        creados += nuevo
                        fuente.copyTo(nuevo, overwrite = false)
                    }.absolutePath
                }
                idea.put("custom_icon_path", destino)
            }
            val prueba = Room.inMemoryDatabaseBuilder(contexto, MindropDatabase::class.java).build()
            try { prueba.withTransaction { reemplazar(prueba.openHelper.writableDatabase, tablas, secuencias) } }
            finally { prueba.close() }

            // Los trabajos previos no deben poder reclamar un recordatorio restaurado con el mismo id.
            val recordatorios = tablas.getJSONArray("recordatorios")
            val revision = maxOf(System.currentTimeMillis(), db.withTransaction {
                db.openHelper.writableDatabase.query("SELECT COALESCE(MAX(revision), 0) FROM recordatorios").use { c ->
                    c.moveToFirst(); val anterior = c.getLong(0)
                    require(anterior < Long.MAX_VALUE) { "La revisión de un recordatorio no es válida." }; anterior + 1
                }
            })
            for (i in 0 until recordatorios.length()) recordatorios.getJSONObject(i).put("revision", revision)
            val antiguos = db.withTransaction {
                val sqlite = db.openHelper.writableDatabase
                val rutasAntiguas = mutableListOf<String>()
                sqlite.query("SELECT custom_icon_path FROM ideas WHERE custom_icon_path IS NOT NULL").use { c ->
                    while (c.moveToNext()) rutasAntiguas += c.getString(0)
                }
                reemplazar(sqlite, tablas, secuencias)
                rutasAntiguas
            }
            aplicada = true
            db.invalidationTracker.refreshVersionsAsync()
            antiguos.distinct().forEach { ruta ->
                runCatching {
                    val viejo = File(ruta).canonicalFile
                    if (viejo.parentFile == directorioIconos && viejo.extension == "png") viejo.delete()
                }
            }
        } finally {
            if (!aplicada) creados.forEach { it.delete() }
            temporal.deleteRecursively()
        }
    } }

    private fun comprobarBorrados() {
        check(borrados?.pendientes?.value.isNullOrEmpty()) { "Espera a que termine el aviso de borrado o pulsa Deshacer antes de copiar tus datos." }
    }

    private fun volcar(sqlite: SupportSQLiteDatabase): JSONObject = JSONObject().apply {
        TABLAS.forEach { tabla ->
            val filas = JSONArray()
            sqlite.query("SELECT * FROM `$tabla`").use { c ->
                while (c.moveToNext()) {
                    val fila = JSONObject()
                    c.columnNames.forEachIndexed { i, nombre ->
                        fila.put(nombre, when (c.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                            Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                            Cursor.FIELD_TYPE_STRING -> c.getString(i)
                            else -> error("Tipo de dato no compatible con las copias.")
                        })
                    }
                    filas.put(fila)
                }
            }
            put(tabla, filas)
        }
    }

    private fun reemplazar(sqlite: SupportSQLiteDatabase, tablas: JSONObject, secuencias: JSONObject) {
        require(tablas.keys().asSequence().toSet() == TABLAS.toSet()) { "La copia está incompleta." }
        sqlite.execSQL("UPDATE folders SET parent_folder_id = NULL")
        sqlite.execSQL("UPDATE ideas SET parent_idea_id = NULL")
        TABLAS.reversed().forEach { sqlite.execSQL("DELETE FROM `$it`") }
        TABLAS.forEach { tabla ->
            val columnas = mutableMapOf<String, String>()
            sqlite.query("PRAGMA table_info(`$tabla`)").use { c ->
                while (c.moveToNext()) columnas[c.getString(1)] = c.getString(2)
            }
            val filas = tablas.getJSONArray(tabla)
            require(filas.length() <= 100_000) { "La copia contiene demasiados registros." }
            for (i in 0 until filas.length()) {
                val fila = filas.getJSONObject(i)
                require(fila.keys().asSequence().toSet() == columnas.keys) { "La estructura de la copia no es válida." }
                val valores = ContentValues()
                columnas.forEach { (columna, tipo) ->
                    val valor = fila.get(columna)
                    when {
                        (tabla == "folders" && columna == "parent_folder_id") || (tabla == "ideas" && columna == "parent_idea_id") -> valores.putNull(columna)
                        valor == JSONObject.NULL -> valores.putNull(columna)
                        tipo == "INTEGER" -> { require(valor is Int || valor is Long) { "Número no válido en la copia." }; valores.put(columna, (valor as Number).toLong()) }
                        tipo == "TEXT" -> { require(valor is String) { "Texto no válido en la copia." }; valores.put(columna, valor) }
                        else -> error("Columna no compatible con las copias.")
                    }
                }
                sqlite.insert(tabla, SQLiteDatabase.CONFLICT_ABORT, valores)
            }
        }
        listOf("folders" to "parent_folder_id", "ideas" to "parent_idea_id").forEach { (tabla, padre) ->
            val filas = tablas.getJSONArray(tabla)
            for (i in 0 until filas.length()) {
                val fila = filas.getJSONObject(i)
                if (!fila.isNull(padre)) sqlite.execSQL("UPDATE `$tabla` SET `$padre` = ? WHERE id = ?", arrayOf(fila.getLong(padre), fila.getLong("id")))
            }
        }
        require(secuencias.keys().asSequence().all { it in TABLAS }) { "Secuencias incorrectas." }
        secuencias.keys().forEach { tabla ->
            val valor = secuencias.getLong(tabla)
            require(valor >= 0 && valor < Long.MAX_VALUE) { "Secuencia incorrecta." }
            sqlite.execSQL("UPDATE sqlite_sequence SET seq = MAX(seq, ?) WHERE name = ?", arrayOf(valor, tabla))
            sqlite.execSQL("INSERT INTO sqlite_sequence(name, seq) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM sqlite_sequence WHERE name = ?)", arrayOf(tabla, valor, tabla))
        }
        sqlite.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { "La copia contiene relaciones incorrectas." } }
    }

    private fun validarContenido(tablas: JSONObject) {
        require(tablas.keys().asSequence().toSet() == TABLAS.toSet()) { "La copia está incompleta." }
        fun filas(tabla: String): List<JSONObject> = tablas.getJSONArray(tabla).let { a -> List(a.length()) { a.getJSONObject(it) } }
        listOf("folders", "ideas", "idea_suggestions", "eventos", "recordatorios", "proyectos", "paginas_proyecto").forEach { tabla ->
            filas(tabla).forEach { require(it.getLong("id") in 1 until Long.MAX_VALUE) { "Identificador no válido." } }
        }
        fun jerarquia(tabla: String, campo: String) {
            val registros = filas(tabla)
            val padres = registros.associate { it.getLong("id") to if (it.isNull(campo)) null else it.getLong(campo) }
            require(padres.size == registros.size) { "Identificadores duplicados." }
            val comprobados = mutableSetOf<Long>()
            padres.keys.forEach { origen ->
                val camino = mutableSetOf<Long>()
                var actual: Long? = origen
                while (actual != null && actual !in comprobados) {
                    require(actual in padres && camino.add(actual)) { "La copia contiene una jerarquía incorrecta o circular." }
                    actual = padres[actual]
                }
                comprobados.addAll(camino)
            }
        }
        jerarquia("folders", "parent_folder_id"); jerarquia("ideas", "parent_idea_id")
        filas("folders").forEach { require(it.getString("name").isNotBlank()) }
        filas("ideas").forEach {
            require(it.getString("title").isNotBlank() && it.getInt("is_completed") in 0..1 && it.getInt("es_importante") in 0..1)
        }
        filas("idea_suggestions").forEach {
            require(it.getString("text").isNotBlank() && it.isNull("validated_at") == it.isNull("update_number"))
            if (!it.isNull("update_number")) require(it.getLong("update_number") in 1..Int.MAX_VALUE.toLong())
        }
        val paginas = filas("paginas_proyecto")
        filas("proyectos").forEach { proyecto ->
            require(proyecto.getString("nombre").isNotBlank())
            val propias = paginas.filter { it.getLong("proyectoId") == proyecto.getLong("id") }
            require(propias.isNotEmpty() && (proyecto.isNull("ultimaPaginaId") || propias.any { it.getLong("id") == proyecto.getLong("ultimaPaginaId") }))
        }
        filas("eventos").forEach {
            LocalDate.parse(it.getString("fecha")).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (!it.isNull("minutoDelDia")) require(it.getInt("minutoDelDia") in 0..1439)
            if (!it.isNull("minutosAntes")) require(it.getInt("minutosAntes") >= 0)
        }
        filas("recordatorios").forEach {
            LocalDateTime.parse(it.getString("fechaHora")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            require(it.isNull("ideaId") != it.isNull("eventoId") && it.getLong("revision") > 0 && it.getInt("enviado") in 0..1)
        }
    }

    private fun extraer(entrada: InputStream, temporal: File) {
        var total = 0L
        val nombres = mutableSetOf<String>()
        ZipInputStream(entrada.buffered()).use { zip ->
            while (true) {
                val entradaZip = zip.nextEntry ?: break
                val nombre = entradaZip.name
                require(!entradaZip.isDirectory && (nombre == "mindrop.json" || PATRON_ICONO.matches(nombre)) && nombres.add(nombre) && nombres.size <= 10_000) {
                    "El archivo no es una copia válida de Mindrop."
                }
                val destino = File(temporal, nombre)
                destino.parentFile?.mkdirs()
                var tamano = 0L
                destino.outputStream().buffered().use { salida ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val leidos = zip.read(buffer)
                        if (leidos < 0) break
                        tamano += leidos; total += leidos
                        require(tamano <= (if (nombre == "mindrop.json") MAX_JSON.toLong() else MAX_ICONO) && total <= 128L * 1024 * 1024) {
                            "La copia supera el tamaño permitido."
                        }
                        salida.write(buffer, 0, leidos)
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun comprobarImagen(archivo: File) {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.absolutePath, limites)
        require(limites.outWidth in 1..256 && limites.outHeight in 1..256) { "La copia contiene un icono dañado o demasiado grande." }
        val imagen = BitmapFactory.decodeFile(archivo.absolutePath)
        require(imagen != null) { "La copia contiene un icono dañado." }
        imagen.recycle()
    }

    private fun comprobarEstructuraJson(texto: String) {
        var nivel = 0; var objetos = 0; var separadores = 0; var cadena = false; var escape = false
        texto.forEach { c ->
            if (cadena) {
                if (escape) escape = false else if (c == '\\') escape = true else if (c == '"') cadena = false
            } else when (c) {
                '"' -> cadena = true
                '{', '[' -> {
                    nivel++
                    if (c == '{') objetos++
                    require(nivel <= 16 && objetos <= 100_100) { "La copia contiene una estructura demasiado compleja." }
                }
                '}', ']' -> { nivel--; require(nivel >= 0) { "La copia está dañada." } }
                ',' -> { separadores++; require(separadores <= 2_000_000) { "La copia contiene demasiados registros." } }
            }
        }
        require(nivel == 0 && !cadena) { "La copia está dañada." }
    }

    private companion object {
        val TABLAS = listOf("folders", "ideas", "idea_suggestions", "eventos", "recordatorios", "logros",
            "acciones_rpg", "metricas_rpg", "revisiones_ideas", "proyectos", "paginas_proyecto")
        val PATRON_ICONO = Regex("iconos/[a-fA-F0-9-]{36}\\.png")
        const val MAX_JSON = 32 * 1024 * 1024
        const val MAX_ICONO = 512L * 1024
    }
}
