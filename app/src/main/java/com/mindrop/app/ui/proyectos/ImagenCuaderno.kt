package com.mindrop.app.ui.proyectos

import com.mindrop.app.ui.icons.PackIcon as Icon

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.mindrop.app.ui.icons.packPainterResource as painterResource
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.data.local.entity.ImagenPagina
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ImagenCuaderno(imagen: ImagenPagina, editando: Boolean, habilitada: Boolean, tinta: Color, eliminar: (Long) -> Unit) {
    var cargando by remember(imagen.ruta) { mutableStateOf(true) }
    val bitmap by produceState<android.graphics.Bitmap?>(null, imagen.ruta) {
        val cargada = withContext(Dispatchers.IO) {
            runCatching {
                val archivo = File(imagen.ruta)
                if (!archivo.isFile || archivo.length() > 3L * 1024 * 1024) return@runCatching null
                val datos = archivo.readBytes()
                val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(datos, 0, datos.size, limites)
                if (limites.outWidth !in 1..1600 || limites.outHeight !in 1..1600) null else {
                    var muestra = 1
                    while (maxOf(limites.outWidth, limites.outHeight) / muestra > 1024) muestra *= 2
                    BitmapFactory.decodeByteArray(datos, 0, datos.size, BitmapFactory.Options().apply { inSampleSize = muestra })
                }
            }.getOrNull()
        }
        value = cargada
        cargando = false
        awaitDispose { cargada?.recycle() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        bitmap?.let { imagenBitmap ->
            Image(imagenBitmap.asImageBitmap(), "Imagen de la página", Modifier.fillMaxWidth().heightIn(max = 400.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
        } ?: if (cargando) Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp), color = tinta)
        } else Text("La imagen no está disponible.", color = tinta, style = MaterialTheme.typography.bodySmall)
        if (editando) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { eliminar(imagen.id) }, enabled = habilitada) { Icon(painterResource(R.drawable.ic_delete), "Eliminar imagen", tint = tinta) }
        }
    }
}
