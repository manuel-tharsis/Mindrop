package com.mindrop.app.ui.proyectos

import com.mindrop.app.data.cuaderno.PuntoDibujo
import org.junit.Assert.*
import org.junit.Test

class DibujoViewModelTest {
    @Test fun cancelarPideConfirmacionYPermiteConservarElBorrador() {
        val vm = DibujoViewModel(); vm.abrir(3)
        vm.punto(PuntoDibujo(.1f,.2f)); vm.punto(PuntoDibujo(.8f,.9f)); vm.terminarTrazo()
        vm.cancelar()
        assertTrue(vm.confirmarDescarte); assertEquals(3L, vm.paginaId); assertEquals(1, vm.trazos.size)
        vm.continuar(); assertFalse(vm.confirmarDescarte); assertEquals(1, vm.trazos.size)
        vm.cerrar(); assertNull(vm.paginaId); assertTrue(vm.trazos.isEmpty())
    }
    @Test fun deshacerYLimpiarNoModificanLaCopiaPreparadaParaGuardar() {
        val vm = DibujoViewModel(); vm.abrir(1)
        vm.punto(PuntoDibujo(-1f,2f)); val copia = vm.copia()
        assertEquals(PuntoDibujo(0f,1f), copia.single().single())
        vm.deshacer(); assertTrue(vm.trazos.isEmpty()); assertEquals(1, copia.size)
        vm.punto(PuntoDibujo(.2f,.3f)); vm.terminarTrazo(); vm.limpiar()
        assertTrue(vm.trazos.isEmpty()); assertFalse(vm.limiteAlcanzado)
    }
}
