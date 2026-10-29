package com.mindrop.app.data.rpg

data class DefinicionLogro(val clave: String, val nombre: String, val metrica: String, val objetivo: Long,
    val descripcion: String, val dificil: Boolean = false)

val catalogoLogros = buildList {
    fun serie(metrica: String, metas: List<Int>, nombres: List<String>, verbo: String) {
        metas.forEachIndexed { indice, meta ->
            val unidad = when (metrica) { "ideas", "completadas" -> "idea"; "validadas" -> "sugerencia"; else -> "evento" }
            add(DefinicionLogro("${metrica}_$meta", nombres[indice], metrica, meta.toLong(),
                "$verbo $meta $unidad${if (meta == 1) "" else "s"}.", meta >= 100))
        }
    }
    serie("ideas", listOf(1, 10, 50, 100, 500), listOf("Primera chispa", "Mente inquieta", "Coleccionista", "Fábrica de ideas", "Mente legendaria"), "Crea")
    add(DefinicionLogro("carpetas_5", "Ordenado", "carpetas", 5, "Crea 5 carpetas."))
    add(DefinicionLogro("subideas_10", "Arquitecto", "subideas", 10, "Relaciona 10 ideas como subideas."))
    add(DefinicionLogro("profundidad_5", "Árbol de pensamientos", "profundidad", 5, "Construye una jerarquía de 5 niveles."))
    serie("completadas", listOf(1, 10, 50, 100), listOf("Primer objetivo", "Productivo", "Imparable", "Maestro de proyectos"), "Completa")
    serie("validadas", listOf(1, 25, 100), listOf("Evolución", "Mejorando ideas", "Evolución constante"), "Valida")
    serie("eventos", listOf(1, 10, 100), listOf("Primer compromiso", "Agenda activa", "Organizador experto"), "Crea")
    listOf(7, 30, 100, 365).zip(listOf("Constante", "Disciplinado", "Leyenda de Mindrop", "Año de ideas")).forEach { (dias, nombre) ->
        add(DefinicionLogro("racha_$dias", nombre, "racha_maxima", dias.toLong(), "Registra actividad real durante $dias días consecutivos.", dias >= 100))
    }
}

data class Recompensa(val clave: String, val categoria: String, val nombre: String, val logro: String?, val detalle: String)
val recompensas = listOf(
    Recompensa("clasico", "tema", "Clásico", null, "Azul noche y oro"),
    Recompensa("bosque", "tema", "Bosque encantado", "carpetas_5", "Verdes de bosque"),
    Recompensa("nocturno", "tema", "Nocturno", "racha_7", "Estrellas y luna"),
    Recompensa("cyberpunk", "tema", "Cyberpunk", "ideas_100", "Cian y violeta"),
    Recompensa("dorado", "tema", "Dorado", "completadas_100", "Oro legendario"),
    Recompensa("clasico", "diseno", "Básico", null, "Iconos 2D sencillos para toda la app"),
    Recompensa("cofre", "diseno", "Aventurero", "subideas_10", "Cofres, cuadernos y detalles de latón"),
    Recompensa("bosque", "diseno", "Bosque", "carpetas_5", "Formas naturales y un pequeño detalle de hoja"),
    Recompensa("runa", "diseno", "Arcano", "profundidad_5", "Formas rúnicas que adoptan el color del tema"),
    Recompensa("cyberpunk", "diseno", "Cyberpunk", "ideas_100", "Grafito y acentos cian sin recargar la interfaz"),
    Recompensa("chispa", "efecto", "Chispa", null, "Aparición discreta"),
    Recompensa("magia", "efecto", "Magia", "validadas_25", "Destellos mágicos"),
    Recompensa("dorado", "efecto", "Partículas doradas", "racha_100", "Polvo de estrellas dorado"),
)

fun recompensaPermitida(categoria: String, clave: String, logros: Set<String>): Boolean {
    val recompensa = recompensas.firstOrNull { it.categoria == categoria && it.clave == clave } ?: return false
    return recompensa.logro == null || recompensa.logro in logros
}
