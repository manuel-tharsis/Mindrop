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
            assertEquals(color, packPalette(IconPack.ARCANE, color).accent)
        }
        assertEquals(packPalette(IconPack.BASIC, Color.Blue), packPalette(IconPack.BASIC, Color.Green))
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
        mindropIconOptions.forEach { assertNotNull(it.key, iconRole(it.drawableRes)) }
    }
}
