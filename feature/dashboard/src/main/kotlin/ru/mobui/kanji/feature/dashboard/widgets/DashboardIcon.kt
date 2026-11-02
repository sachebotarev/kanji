package ru.mobui.kanji.feature.dashboard.widgets

import ru.mobui.kanji.feature.dashboard.R

internal enum class DashboardIcon(val resource: Int, val opticalScale: Float = 1f) {
    Settings(R.drawable.art_settings),
    Textbooks(R.drawable.art_personal, STANDARD_ILLUSTRATION_SCALE),
    Personal(R.drawable.art_textbooks_v4, STANDARD_ILLUSTRATION_SCALE),
    History(R.drawable.art_history_v4, STANDARD_ILLUSTRATION_SCALE),
    Builder(R.drawable.art_builder, BUILDER_ILLUSTRATION_SCALE),
    Support(R.drawable.art_support, SUPPORT_ILLUSTRATION_SCALE),
    Start(R.drawable.art_start_v4),
    Expand(R.drawable.art_paw_v4),
}

private const val STANDARD_ILLUSTRATION_SCALE = 1.1f

private const val BUILDER_ILLUSTRATION_SCALE = 1.15f

private const val SUPPORT_ILLUSTRATION_SCALE = 1.3f
