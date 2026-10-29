package com.mindrop.app.ui.icons

import androidx.compose.ui.graphics.Color
import com.mindrop.app.data.rpg.recompensaPermitida
import org.junit.Assert.*
import org.junit.Test

class IconPackTest {
    @Test fun clavesAnterioresConservanSuPackYLasDesconocidasUsanBasico() {
        assertEquals(IconPack.BASIC, IconPack.fromKey("clasico"))
        assertEquals(IconPack.ADVENTURE, IconPack.fromKey("cofre"))
        assertEquals(IconPack.ARCANE, IconPack.fromKey("runa"))
        assertEquals(IconPack.BASIC, IconPack.fromKey("desconocido"))
    }

    @Test fun arcanoSigueElTemaEnTodasSusVariantes() {
        listOf(0xFF92BEFF, 0xFF8DE4A8, 0xFFC7BEFF, 0xFF55F4E6, 0xFFFFD56A).forEach {
            val color = Color(it)
            assertNotNull(packAssetId(IconPack.ARCANE, "folder", color))
        }
        assertEquals(packAssetId(IconPack.BASIC, "folder", Color.Blue), packAssetId(IconPack.BASIC, "folder", Color.Green))
        val variants = listOf(Color.Blue, Color(0xFF8DE4A8), Color(0xFFC7BEFF), Color(0xFF55F4E6), Color(0xFFFFD56A))
            .map { packAssetId(IconPack.ARCANE, "folder", it) }
        assertEquals(5, variants.toSet().size)
    }

    @Test fun losPacksConservanLosRequisitosDeDesbloqueo() {
        assertTrue(recompensaPermitida("diseno", "clasico", emptySet()))
        listOf("cofre" to "subideas_10", "runa" to "profundidad_5", "bosque" to "carpetas_5", "cyberpunk" to "ideas_100").forEach { (pack, logro) ->
            assertFalse(recompensaPermitida("diseno", pack, emptySet()))
            assertTrue(recompensaPermitida("diseno", pack, setOf(logro)))
        }
    }

    @Test fun todasLasCategoriasDelSelectorTienenDibujoEnLosPacks() {
        assertEquals(21, mindropIconOptions.size)
        IconPack.entries.forEach { pack ->
            mindropIconOptions.forEach { option ->
                val role = iconRole(option.drawableRes)
                assertNotNull(option.key, role)
                assertNotNull("$pack: ${option.key}", packAssetId(pack, role!!, Color.Blue))
            }
        }
    }
}
