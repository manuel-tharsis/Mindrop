package com.mindrop.app.ui.icons

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mindrop.app.R

data class MindropIconOption(
    val key: String,
    @param:DrawableRes val drawableRes: Int,
    @param:StringRes val labelRes: Int,
)

val mindropIconOptions = listOf(
    MindropIconOption("idea", R.drawable.ic_idea, R.string.idea_icon_option),
    MindropIconOption("code", R.drawable.ic_code, R.string.code_icon_option),
    MindropIconOption("mobile", R.drawable.ic_mobile, R.string.mobile_icon_option),
    MindropIconOption("computer", R.drawable.ic_computer, R.string.computer_icon_option),
    MindropIconOption("car", R.drawable.ic_car, R.string.car_icon_option),
    MindropIconOption("tools", R.drawable.ic_tools, R.string.tools_icon_option),
    MindropIconOption("game", R.drawable.ic_game, R.string.game_icon_option),
    MindropIconOption("home", R.drawable.ic_home, R.string.home_icon_option),
    MindropIconOption("work", R.drawable.ic_work, R.string.work_icon_option),
    MindropIconOption("document", R.drawable.ic_document, R.string.document_icon_option),
    MindropIconOption("folder", R.drawable.ic_folder, R.string.folder_icon_option),
    MindropIconOption("star", R.drawable.ic_star, R.string.star_icon_option),
    MindropIconOption("brain", R.drawable.ic_brain, R.string.brain_icon_option),
    MindropIconOption("money", R.drawable.ic_money, R.string.money_icon_option),
    MindropIconOption("sport", R.drawable.ic_sport, R.string.sport_icon_option),
    MindropIconOption("terminal", R.drawable.ic_terminal, R.string.terminal_icon_option),
)

fun mindropIcon(key: String): MindropIconOption {
    val normalizedKey = when (key.lowercase()) {
        "android" -> "mobile"
        "cli" -> "terminal"
        else -> key.lowercase()
    }
    return mindropIconOptions.firstOrNull { it.key == normalizedKey }
        ?: mindropIconOptions.first()
}
