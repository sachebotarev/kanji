package ru.mobui.kanji.feature.catalog

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.JlptFilter
import ru.mobui.kanji.domain.catalog.JlptLevel
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing

public data class CatalogReviewOptions(val scenario: Int, val language: String, val dark: Boolean, val variant: String)
