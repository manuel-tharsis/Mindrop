package com.mindrop.app.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.ui.rpg.LocalDiseno

/** Las claves antiguas se conservan para no cambiar las preferencias ya guardadas. */
enum class IconPack(val key: String) {
    BASIC("clasico"), ADVENTURE("cofre"), FOREST("bosque"), ARCANE("runa"), CYBER("cyberpunk");

    companion object {
        fun fromKey(key: String): IconPack = entries.firstOrNull { it.key == key } ?: BASIC
    }
}

internal data class PackPalette(val accent: Color, val body: Color, val detail: Color)

internal fun packPalette(pack: IconPack, themeAccent: Color): PackPalette = when (pack) {
    IconPack.BASIC -> PackPalette(Color(0xFF92BEFF), Color(0xFF7D90B5), Color(0xFFE4E9F4))
    IconPack.ADVENTURE -> PackPalette(Color(0xFFF1C366), Color(0xFFAD784C), Color(0xFFF1DEB4))
    IconPack.FOREST -> PackPalette(Color(0xFF92CC79), Color(0xFF659859), Color(0xFFDCE7C8))
    IconPack.ARCANE -> PackPalette(themeAccent, Color(0xFF626F92), Color(0xFFDCE1EF))
    IconPack.CYBER -> PackPalette(Color(0xFF55E9DE), Color(0xFF73829D), Color(0xFFDCE4F0))
}

/** Un único punto de entrada para categorías, acciones y navegación. Sin bitmaps ni cargas. */
@Composable
fun packPainterResource(@DrawableRes id: Int): Painter {
    val role = iconRole(id) ?: return androidx.compose.ui.res.painterResource(id)
    val pack = IconPack.fromKey(LocalDiseno.current)
    val palette = packPalette(pack, MaterialTheme.colorScheme.primary)
    return remember(role, pack, palette) { PackPainter(role, pack, palette) }
}

/** Material Icon aplicaría un tinte único y borraría los colores del pack. */
@Composable
fun PackIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    if (painter is PackPainter) {
        val error = MaterialTheme.colorScheme.error
        // También se respeta la tinta oscura de las acciones sobre el papel claro.
        val onFilled = tint != Color.Unspecified && (tint == MaterialTheme.colorScheme.onPrimary || tint.luminance() < .15f)
        val actual = remember(painter, tint, error, onFilled) {
            painter.withTint(tint, error, onFilled)
        }
        Image(actual, contentDescription, modifier.size(24.dp), alpha = if (tint == Color.Unspecified) 1f else tint.alpha)
    } else {
        androidx.compose.material3.Icon(painter, contentDescription, modifier, tint)
    }
}

@Composable
fun ThemedIcon(@DrawableRes id: Int, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    PackIcon(packPainterResource(id), contentDescription, modifier, tint)
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

internal class PackPainter(
    private val role: String,
    private val pack: IconPack,
    private val palette: PackPalette,
    private val foreground: Color? = null,
) : Painter() {
    override val intrinsicSize = Size(24f, 24f)

    fun withTint(tint: Color, error: Color, onFilled: Boolean = false): PackPainter {
        // Prioridad conserva su señal amarilla y su estado apagado en todos los packs.
        val override = when {
            onFilled -> tint
            role == "flag" -> tint.takeUnless { it == Color.Unspecified }
            tint != Color.Unspecified && tint.copy(alpha = 1f) == error.copy(alpha = 1f) -> error
            else -> null
        }
        return if (override == foreground) this else PackPainter(role, pack, palette, override)
    }

    override fun DrawScope.onDraw() {
        val side = size.minDimension
        translate((size.width - side) / 2, (size.height - side) / 2) {
            scale(side / 24f, side / 24f, Offset.Zero) {
                Glyph(this, role, pack, palette, foreground).draw()
            }
        }
    }
}

/** Geometría 2D original: pocos planos, sin texturas, sombras ni detalles microscópicos. */
private class Glyph(
    private val scope: DrawScope,
    private val role: String,
    private val pack: IconPack,
    palette: PackPalette,
    foreground: Color?,
) {
    private val a = foreground ?: palette.accent
    private val b = foreground ?: palette.body
    private val d = foreground ?: palette.detail
    private val gold = foreground ?: if (pack == IconPack.BASIC) Color(0xFFFFD56A) else a
    private val green = foreground ?: Color(0xFF8DCE91)
    private val neutral = Color(0xFF172039)

    private fun rect(x: Float, y: Float, w: Float, h: Float, color: Color, r: Float = 1.5f) =
        scope.drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r))
    private fun circle(x: Float, y: Float, r: Float, color: Color) = scope.drawCircle(color, r, Offset(x, y))
    private fun line(x: Float, y: Float, x2: Float, y2: Float, color: Color, width: Float = 1.8f) =
        scope.drawLine(color, Offset(x, y), Offset(x2, y2), width, StrokeCap.Round)
    private fun shape(color: Color, vararg points: Float) {
        val p = Path().apply {
            moveTo(points[0], points[1]); var i = 2
            while (i < points.size) { lineTo(points[i], points[i + 1]); i += 2 }
            close()
        }
        scope.drawPath(p, color)
    }
    private fun outline(color: Color, vararg points: Float) {
        val p = Path().apply {
            moveTo(points[0], points[1]); var i = 2
            while (i < points.size) { lineTo(points[i], points[i + 1]); i += 2 }
        }
        scope.drawPath(p, color, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    private fun ring(x: Float, y: Float, r: Float, color: Color, width: Float = 1.8f) =
        scope.drawCircle(color, r, Offset(x, y), style = Stroke(width))
    private fun diamond(x: Float, y: Float, s: Float, color: Color) = shape(color, x, y - s, x + s * .7f, y, x, y + s, x - s * .7f, y)
    private fun leaf(x: Float, y: Float, s: Float) {
        val p = Path().apply {
            moveTo(x - s, y + s)
            cubicTo(x - s * 1.1f, y - s, x + s * .4f, y - s, x + s, y - s)
            cubicTo(x + s, y + s * .6f, x, y + s * 1.2f, x - s, y + s)
        }
        scope.drawPath(p, a)
        line(x - s * .4f, y + s * .4f, x + s * .4f, y - s * .3f, b, .6f)
    }
    private fun motif(x: Float, y: Float, s: Float = 2f) {
        when (pack) {
            IconPack.ARCANE -> diamond(x, y, s, a)
            IconPack.FOREST -> leaf(x, y, s)
            IconPack.CYBER -> outline(a, x - s, y + s * .4f, x, y + s * .4f, x + s, y - s * .4f)
            IconPack.ADVENTURE -> rect(x - s, y - s * .6f, s * 2, s * 1.2f, a, .6f)
            IconPack.BASIC -> Unit
        }
    }
    private fun folder() {
        if (pack == IconPack.ADVENTURE) {
            rect(2f, 6f, 20f, 15f, b, 2f)
            rect(2f, 5f, 20f, 7f, a, 2f)
            rect(4f, 6f, 16f, 4f, b, 1f)
            rect(10f, 10f, 4f, 5f, a, .8f)
            circle(12f, 12f, .65f, neutral)
        } else {
            val col = if (pack == IconPack.BASIC) gold else b
            shape(col, 2f, 5f, 9f, 5f, 11f, 8f, 22f, 8f, 22f, 20f, 2f, 20f)
            rect(2f, 9f, 20f, 11f, if (pack == IconPack.BASIC) Color(0xFFFFC847) else b, 1.5f)
            if (pack == IconPack.BASIC) rect(2f, 9f, 20f, 2f, gold, .8f) else motif(12f, 14f, 2.8f)
        }
    }
    private fun page() {
        shape(d, 5f, 2f, 15f, 2f, 20f, 7f, 20f, 22f, 5f, 22f)
        shape(b, 15f, 2f, 15f, 7f, 20f, 7f)
    }
    private fun star(x: Float = 12f, y: Float = 12f, r: Float = 10f, color: Color = gold) {
        val points = FloatArray(20)
        repeat(10) { i ->
            val angle = -Math.PI / 2 + i * Math.PI / 5
            val radius = if (i % 2 == 0) r else r * .45f
            points[i * 2] = x + kotlin.math.cos(angle).toFloat() * radius
            points[i * 2 + 1] = y + kotlin.math.sin(angle).toFloat() * radius
        }
        shape(color, *points)
    }
    private fun cog() {
        repeat(8) { i ->
            val angle = i * Math.PI / 4
            line(12f + kotlin.math.cos(angle).toFloat() * 7, 12f + kotlin.math.sin(angle).toFloat() * 7,
                12f + kotlin.math.cos(angle).toFloat() * 9.5f, 12f + kotlin.math.sin(angle).toFloat() * 9.5f, b, 4f)
        }
        ring(12f, 12f, 6.8f, b, 4f)
        if (pack == IconPack.ARCANE) diamond(12f, 12f, 2.4f, a)
        else if (pack != IconPack.BASIC) ring(12f, 12f, 3f, a, 1.2f)
    }
    fun draw() {
        when (role) {
            "folder" -> folder()
            "idea" -> {
                if (pack == IconPack.ADVENTURE) {
                    outline(a, 7f, 7f, 7f, 4f, 9f, 2f, 15f, 2f, 17f, 4f, 17f, 7f)
                    shape(b, 7f, 7f, 17f, 7f, 20f, 20f, 4f, 20f)
                    shape(a, 9f, 8f, 15f, 8f, 17f, 18f, 7f, 18f)
                    line(6f, 21f, 18f, 21f, b, 2f)
                } else {
                    circle(12f, 9f, 7f, if (pack == IconPack.BASIC) gold else a)
                    rect(8f, 13f, 8f, 5f, if (pack == IconPack.BASIC) gold else a)
                    rect(8f, 18f, 8f, 3f, b, 1f); rect(10f, 21f, 4f, 1.5f, b, .8f)
                    if (pack == IconPack.ARCANE) diamond(12f, 9f, 2.6f, neutral)
                    else if (pack == IconPack.FOREST) leaf(12f, 10f, 2.4f)
                    else outline(d, 10f, 9f, 12f, 11f, 14f, 9f, 12f, 11f, 12f, 16f)
                }
            }
            "project" -> {
                rect(4f, 2f, 16f, 20f, b, 2f)
                rect(5f, 2f, 2f, 20f, a, .3f)
                line(3f, 7f, 6f, 7f, a); line(3f, 12f, 6f, 12f, a); line(3f, 17f, 6f, 17f, a)
                if (pack == IconPack.BASIC) rect(10f, 6f, 7f, 4f, d, 1f) else motif(13f, 11f, 3f)
            }
            "calendar" -> {
                rect(3f, 5f, 18f, 17f, d, 2f); rect(3f, 5f, 18f, 5f, a, 1f)
                line(7f, 3f, 7f, 7f, b, 2f); line(17f, 3f, 17f, 7f, b, 2f)
                repeat(2) { y -> repeat(3) { x -> rect(6f + x * 4.5f, 12f + y * 4.5f, 3f, 3f, if (x == 2 && y == 0) a else b, .6f) } }
            }
            "trophy" -> {
                outline(gold, 5f, 5f, 2f, 5f, 2f, 9f, 5f, 12f, 8f, 12f)
                outline(gold, 19f, 5f, 22f, 5f, 22f, 9f, 19f, 12f, 16f, 12f)
                shape(gold, 5f, 3f, 19f, 3f, 18f, 13f, 14f, 16f, 14f, 20f, 18f, 20f, 18f, 22f, 6f, 22f, 6f, 20f, 10f, 20f, 10f, 16f, 6f, 13f)
                star(12f, 9f, 3.5f, b)
            }
            "settings" -> cog()
            "code" -> { outline(a, 7f, 6f, 2f, 12f, 7f, 18f); outline(a, 17f, 6f, 22f, 12f, 17f, 18f); line(14f, 4f, 10f, 20f, a, 2.2f) }
            "terminal" -> { rect(2f, 4f, 20f, 16f, b); outline(d, 6f, 9f, 9f, 12f, 6f, 15f); line(12f, 16f, 18f, 16f, a) }
            "mobile" -> { rect(6f, 1f, 12f, 22f, b, 2f); rect(7.5f, 4f, 9f, 15f, neutral, .7f); line(10f, 2.5f, 14f, 2.5f, d, .8f); circle(12f, 21f, .8f, a); motif(12f, 11f, 2.2f) }
            "computer" -> { rect(2f, 3f, 20f, 14f, b); rect(3.5f, 4.5f, 17f, 11f, neutral, .5f); rect(10f, 17f, 4f, 3f, b, .2f); rect(6f, 20f, 12f, 2f, a, .8f); motif(12f, 10f, 2.7f) }
            "car" -> { shape(b, 4f, 10f, 7f, 4f, 17f, 4f, 20f, 10f); shape(neutral, 7f, 9f, 8.5f, 5.5f, 15.5f, 5.5f, 17f, 9f); rect(2f, 10f, 20f, 9f, a, 2f); rect(3f, 18f, 4f, 4f, b, 1f); rect(17f, 18f, 4f, 4f, b, 1f); rect(4f, 12f, 4f, 2.5f, d); rect(16f, 12f, 4f, 2.5f, d); rect(9f, 16f, 6f, 1.5f, b); motif(12f, 12f, 1.6f) }
            "tools" -> { line(6f, 19f, 16f, 9f, b, 4f); circle(5f, 20f, 2.2f, b); circle(5f, 20f, .7f, d); val p = Path().apply { moveTo(13f, 2f); lineTo(13f, 7f); lineTo(17f, 9f); lineTo(21f, 6f); cubicTo(23f, 14f, 12f, 16f, 11f, 8f); close() }; scope.drawPath(p, a) }
            "game" -> { val p = Path().apply { moveTo(7f, 6f); lineTo(17f, 6f); cubicTo(21f, 6f, 23f, 16f, 21f, 20f); cubicTo(19f, 22f, 16f, 17f, 15f, 17f); lineTo(9f, 17f); cubicTo(8f, 17f, 5f, 22f, 3f, 20f); cubicTo(1f, 16f, 3f, 6f, 7f, 6f); close() }; scope.drawPath(p, b); line(7f, 9f, 7f, 14f, a, 2f); line(4.5f, 11.5f, 9.5f, 11.5f, a, 2f); circle(17f, 10f, 1f, a); circle(19f, 13f, 1f, d); motif(12f, 11f, 1.4f) }
            "home" -> { shape(a, 1f, 11f, 12f, 2f, 23f, 11f, 21f, 13f, 12f, 5f, 3f, 13f); rect(5f, 11f, 14f, 11f, d, .8f); rect(10f, 15f, 4f, 7f, b, .5f); motif(12f, 11f, 1.5f) }
            "work" -> { rect(8f, 3f, 8f, 5f, a, 1f); rect(9.5f, 4.5f, 5f, 3f, neutral, .3f); rect(2f, 7f, 20f, 15f, b, 2f); rect(2f, 7f, 20f, 7f, d, 2f); rect(10f, 12f, 4f, 4f, a, .8f); if (pack != IconPack.BASIC) motif(12f, 18f, 1.8f) }
            "document", "page_add", "pdf", "save" -> {
                if (role == "save") { rect(3f, 2f, 18f, 20f, a); rect(6f, 2f, 12f, 7f, d, .5f); rect(14f, 3f, 2f, 5f, b, .2f); rect(6f, 13f, 12f, 9f, d, .5f) }
                else {
                    page()
                    when (role) {
                        "page_add" -> { line(9f, 14f, 17f, 14f, a, 2f); line(13f, 10f, 13f, 18f, a, 2f) }
                        "pdf" -> { rect(7f, 10f, 11f, 8f, b, .8f); outline(d, 8.5f, 16f, 8.5f, 12f, 10.5f, 12f, 10.5f, 14f, 8.5f, 14f); outline(d, 12f, 12f, 14f, 12f, 14f, 16f, 12f, 16f, 12f, 12f); outline(d, 16f, 16f, 16f, 12f, 18f, 12f); line(16f, 14f, 17.5f, 14f, d, .7f) }
                        else -> { line(8f, 12f, 17f, 12f, b, 1.4f); line(8f, 16f, 15f, 16f, b, 1.4f); motif(11f, 7f, 1.6f) }
                    }
                }
            }
            "star" -> if (pack == IconPack.ARCANE) { outline(a, 12f, 2f, 16f, 8f, 22f, 12f, 16f, 16f, 12f, 22f, 8f, 16f, 2f, 12f, 8f, 8f, 12f, 2f); diamond(12f, 12f, 4f, a) } else star()
            "brain" -> { circle(8f, 6f, 3.5f, a); circle(6f, 11f, 4f, a); circle(7f, 16f, 4f, a); circle(9f, 19f, 3f, a); circle(16f, 6f, 3.5f, a); circle(18f, 11f, 4f, a); circle(17f, 16f, 4f, a); circle(15f, 19f, 3f, a); line(12f, 4f, 12f, 21f, b, 1f); outline(b, 7f, 7f, 9f, 9f, 8f, 11f); outline(b, 17f, 7f, 15f, 9f, 16f, 11f); line(7f, 16f, 9f, 17f, b, 1f); line(17f, 16f, 15f, 17f, b, 1f) }
            "money" -> { repeat(3) { i -> rect(2f, 13f + i * 3f, 9f, 2.5f, gold, 1f) }; repeat(5) { i -> rect(13f, 7f + i * 3f, 9f, 2.5f, gold, 1f) }; motif(17.5f, 7f, 1.4f) }
            "sport" -> { line(6f, 12f, 18f, 12f, d, 3f); rect(2f, 7f, 3f, 10f, a); rect(5f, 4f, 4f, 16f, a); rect(15f, 4f, 4f, 16f, a); rect(19f, 7f, 3f, 10f, a) }
            "camera" -> { rect(2f, 6f, 20f, 15f, b, 2f); rect(7f, 3f, 8f, 5f, b); circle(12f, 13f, 5f, d); circle(12f, 13f, 3.4f, a); circle(19f, 9f, .8f, d) }
            "image", "image_add" -> { rect(2f, 3f, 20f, 18f, d); rect(4f, 5f, 16f, 14f, a, .4f); circle(16f, 8f, 1.6f, gold); shape(b, 4f, 18f, 9f, 11f, 13f, 15f, 16f, 12f, 20f, 18f); if (role == "image_add") { circle(19f, 5f, 4.5f, neutral); line(16f, 5f, 22f, 5f, a); line(19f, 2f, 19f, 8f, a) } }
            "draw" -> { shape(a, 10f, 15f, 19f, 2f, 22f, 4f, 14f, 18f); shape(d, 9f, 15f, 12f, 12f, 16f, 15f, 14f, 18f); val p = Path().apply { moveTo(11f, 16f); cubicTo(3f, 16f, 7f, 22f, 2f, 22f); cubicTo(11f, 24f, 17f, 21f, 11f, 16f); close() }; scope.drawPath(p, a) }
            "flag" -> { line(5f, 2f, 5f, 22f, b, 1.8f); shape(a, 6f, 3f, 21f, 4f, 18f, 9f, 21f, 14f, 6f, 13f) }
            "completed" -> { circle(12f, 12f, 10f, green); outline(neutral, 6f, 12f, 10f, 16f, 18f, 8f) }
            "check" -> outline(green, 4f, 12f, 9f, 17f, 20f, 6f)
            "add" -> { line(12f, 4f, 12f, 20f, a, 2.6f); line(4f, 12f, 20f, 12f, a, 2.6f) }
            "edit" -> { shape(a, 5f, 16f, 16f, 3f, 21f, 7f, 10f, 20f); shape(d, 5f, 16f, 10f, 20f, 3f, 22f); line(15f, 5f, 19f, 8f, b, 1f) }
            "delete" -> { rect(5f, 7f, 14f, 15f, b, 1f); rect(3f, 4f, 18f, 2f, a, .8f); outline(a, 9f, 4f, 9f, 2f, 15f, 2f, 15f, 4f); line(9f, 10f, 9f, 19f, d, 1.2f); line(15f, 10f, 15f, 19f, d, 1.2f) }
            "move" -> { folder(); line(8f, 14f, 18f, 14f, d, 2f); outline(d, 14f, 10f, 18f, 14f, 14f, 18f) }
            "search" -> { ring(10f, 10f, 7f, a, 2.2f); line(15f, 15f, 22f, 22f, b, 3f) }
            "back" -> { line(3f, 12f, 21f, 12f, a, 2.2f); outline(a, 10f, 5f, 3f, 12f, 10f, 19f) }
            "list" -> repeat(3) { i -> circle(4f, 6f + i * 6f, 1.4f, a); line(9f, 6f + i * 6f, 21f, 6f + i * 6f, d) }
            "more" -> repeat(3) { i -> circle(12f, 5f + i * 7f, 1.6f, b) }
        }
    }
}
