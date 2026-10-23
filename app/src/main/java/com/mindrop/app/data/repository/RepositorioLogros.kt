package com.mindrop.app.data.repository

import androidx.room.withTransaction
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.preferences.RepositorioPreferencias
import com.mindrop.app.data.rpg.*

class RepositorioLogros(private val db: MindropDatabase, private val preferencias: RepositorioPreferencias) {
    fun observarLogros() = db.daoRpg().observarLogros()
    fun observarMetricas() = db.daoRpg().observarMetricas()
    suspend fun celebrar(clave: String) = db.daoRpg().celebrar(clave)
    suspend fun aplicar(categoria: String, clave: String) {
        val disponibles = db.daoRpg().logros().map { it.clave }.toSet()
        require(recompensaPermitida(categoria, clave, disponibles)) { "Recompensa bloqueada." }
        preferencias.seleccionar(categoria, clave)
    }
    suspend fun iniciar() = ProgresoRpg(db).inicializar()
}
