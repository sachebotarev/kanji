package ru.mobui.kanji.feature.catalog.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.domain.catalog.JlptFilter
import ru.mobui.kanji.feature.catalog.R

@Composable
internal fun jlptLabel(value: JlptFilter): String =
    when (value) {
        JlptFilter.AnyLevel -> stringResource(R.string.catalog_all_jlpt)
        JlptFilter.Missing -> stringResource(R.string.catalog_jlpt_missing)
        is JlptFilter.Exact -> stringResource(R.string.catalog_jlpt_level, value.level.number)
    }
