package com.mindrop.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException
import androidx.datastore.core.DataStore

private val Context.almacenRpg by preferencesDataStore("preferencias_rpg")
data class PreferenciasRpg(
    val tema: String = "clasico", val diseno: String = "clasico", val efecto: String = "chispa",
    val reducirMovimiento: Boolean = false, val diasRedescubrimiento: Int = 0,
    val silencioDesde: Int = 22, val silencioHasta: Int = 9,
    val tamanoLectura: Int = 18, val papelOscuro: Boolean = false,
)

class RepositorioPreferencias(contexto: Context, private val almacen: DataStore<Preferences> = contexto.almacenRpg) {
    val preferencias = almacen.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }.map { p ->
        PreferenciasRpg(
            p[stringPreferencesKey("tema")] ?: "clasico", p[stringPreferencesKey("diseno")] ?: "clasico",
            p[stringPreferencesKey("efecto")] ?: "chispa", p[booleanPreferencesKey("reducir_movimiento")] ?: false,
            p[intPreferencesKey("dias")] ?: 0, p[intPreferencesKey("silencio_desde")] ?: 22,
            p[intPreferencesKey("silencio_hasta")] ?: 9,
            p[intPreferencesKey("tamano_lectura")]?.coerceIn(16, 24) ?: 18,
            p[booleanPreferencesKey("papel_oscuro")] ?: false,
        )
    }
    // La selección cosmética se valida en RepositorioLogros, no desde las vistas.
    internal suspend fun seleccionar(categoria: String, clave: String) {
        require(categoria in listOf("tema", "diseno", "efecto"))
        almacen.edit { it[stringPreferencesKey(categoria)] = clave }
    }
    suspend fun movimiento(reducir: Boolean) { almacen.edit { it[booleanPreferencesKey("reducir_movimiento")] = reducir } }
    suspend fun lectura(tamano: Int, oscuro: Boolean) {
        require(tamano in listOf(16, 18, 20, 22, 24))
        almacen.edit {
            it[intPreferencesKey("tamano_lectura")] = tamano
            it[booleanPreferencesKey("papel_oscuro")] = oscuro
        }
    }
    suspend fun redescubrimiento(dias: Int, desde: Int, hasta: Int) {
        require(dias in listOf(0, 1, 3, 7, 14) && desde in 0..23 && hasta in 0..23 && desde != hasta)
        almacen.edit {
            it[intPreferencesKey("dias")] = dias
            it[intPreferencesKey("silencio_desde")] = desde
            it[intPreferencesKey("silencio_hasta")] = hasta
        }
    }
}
