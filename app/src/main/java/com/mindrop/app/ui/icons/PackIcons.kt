package com.mindrop.app.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.ui.rpg.LocalDiseno

enum class IconPack(val key: String) {
    BASIC("clasico"), ADVENTURE("cofre"), FOREST("bosque"), ARCANE("runa"), CYBER("cyberpunk");
    companion object {
        fun fromKey(key: String): IconPack = entries.firstOrNull { it.key == key } ?: BASIC
    }
}

internal fun assetVariant(pack: IconPack, accent: Color): String = if (pack != IconPack.ARCANE) "default" else when (accent) {
    Color(0xFF8DE4A8) -> "forest"
    Color(0xFFC7BEFF) -> "night"
    Color(0xFF55F4E6) -> "cyber"
    Color(0xFFFFD56A) -> "gold"
    else -> "default"
}

internal fun packAssetId(pack: IconPack, role: String, accent: Color): Int? =
    packAssetResources[PackAssetKey(pack, role, assetVariant(pack, accent))]

/** Recursos copiados del arte aprobado, sin reconstruir sus formas con geometría. */
@Composable
fun packPainterResource(@DrawableRes id: Int): Painter {
    val role = iconRole(id) ?: return androidx.compose.ui.res.painterResource(id)
    val asset = packAssetId(IconPack.fromKey(LocalDiseno.current), role, MaterialTheme.colorScheme.primary)
        ?: return androidx.compose.ui.res.painterResource(id)
    val original = androidx.compose.ui.res.painterResource(asset)
    return remember(original, role) { PackAssetPainter(original, role) }
}

/** Conserva los colores del archivo en lugar del tinte automático de Material Icon. */
@Composable
fun PackIcon(painter: Painter, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    if (painter !is PackAssetPainter) {
        androidx.compose.material3.Icon(painter, contentDescription, modifier, tint)
        return
    }
    val scheme = MaterialTheme.colorScheme
    val isError = tint != Color.Unspecified && tint.copy(alpha = 1f) == scheme.error.copy(alpha = 1f)
    val inactiveFlag = painter.role == "flag" && tint != Color.Unspecified && tint.copy(alpha = 1f) != scheme.secondary.copy(alpha = 1f)
    val filter = if (isError || inactiveFlag) ColorFilter.tint(tint) else null
    // Sobre papel claro se añade contraste sin repintar el archivo.
    val onPaper = painter.role == "delete" && tint != Color.Unspecified && tint.luminance() < .15f
    val actualModifier = if (onPaper) modifier.size(24.dp).background(tint, RoundedCornerShape(6.dp)).padding(2.dp) else modifier.size(24.dp)
    Image(painter.original, contentDescription, actualModifier,
        alpha = if (tint == Color.Unspecified) 1f else tint.alpha, colorFilter = filter)
}

@Composable
fun ThemedIcon(@DrawableRes id: Int, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    PackIcon(packPainterResource(id), contentDescription, modifier, tint)
}

internal class PackAssetPainter(val original: Painter, val role: String) : Painter() {
    override val intrinsicSize: Size get() = original.intrinsicSize
    override fun DrawScope.onDraw() { with(original) { draw(size) } }
}

internal fun iconRole(@DrawableRes id: Int): String? = when (id) {
    R.drawable.ic_idea -> "idea"
    R.drawable.ic_folder -> "folder"
    R.drawable.ic_code -> "code"
    R.drawable.ic_mobile -> "mobile"
    R.drawable.ic_computer -> "computer"
    R.drawable.ic_car -> "car"
    R.drawable.ic_tools -> "tools"
    R.drawable.ic_game -> "game"
    R.drawable.ic_home -> "home"
    R.drawable.ic_work -> "work"
    R.drawable.ic_document -> "document"
    R.drawable.ic_star -> "star"
    R.drawable.ic_brain -> "brain"
    R.drawable.ic_money -> "money"
    R.drawable.ic_sport -> "sport"
    R.drawable.ic_terminal -> "terminal"
    R.drawable.ic_calendar -> "calendar"
    R.drawable.ic_flag -> "flag"
    R.drawable.ic_add -> "add"
    R.drawable.ic_edit -> "edit"
    R.drawable.ic_delete -> "delete"
    R.drawable.ic_search -> "search"
    R.drawable.ic_move -> "move"
    R.drawable.ic_arrow_back -> "back"
    R.drawable.ic_arrow_next -> "next"
    R.drawable.ic_more_vert -> "more"
    R.drawable.ic_list -> "list"
    R.drawable.ic_check -> "check"
    R.drawable.ic_page_add -> "page_add"
    R.drawable.ic_image_add -> "image_add"
    R.drawable.ic_pdf_export -> "pdf"
    R.drawable.ic_draw_note -> "draw"
    R.drawable.ic_project -> "project"
    R.drawable.ic_settings -> "settings"
    R.drawable.ic_trophy -> "trophy"
    R.drawable.ic_camera -> "camera"
    R.drawable.ic_image -> "image"
    R.drawable.ic_save -> "save"
    R.drawable.ic_completed -> "completed"
    else -> null
}
