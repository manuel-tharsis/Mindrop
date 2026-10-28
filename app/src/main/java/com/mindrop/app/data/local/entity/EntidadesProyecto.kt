package com.mindrop.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "proyectos")
data class Proyecto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val ultimaPaginaId: Long? = null,
)

@Entity(
    tableName = "paginas_proyecto",
    foreignKeys = [ForeignKey(entity = Proyecto::class, parentColumns = ["id"],
        childColumns = ["proyectoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["proyectoId", "orden"])],
)
data class PaginaProyecto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val proyectoId: Long,
    val titulo: String = "",
    val texto: String = "",
    val orden: Long = 0,
)

@Entity(
    tableName = "imagenes_pagina",
    foreignKeys = [ForeignKey(entity = PaginaProyecto::class, parentColumns = ["id"],
        childColumns = ["paginaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["paginaId", "orden"])],
)
data class ImagenPagina(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paginaId: Long,
    val ruta: String,
    val orden: Long = 0,
)
