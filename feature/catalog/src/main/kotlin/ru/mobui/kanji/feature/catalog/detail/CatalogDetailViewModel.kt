package ru.mobui.kanji.feature.catalog.detail

import ru.mobui.kanji.feature.catalog.presentation.CatalogStateViewModel

import ru.mobui.kanji.feature.catalog.components.CatalogSearchState
import ru.mobui.kanji.feature.catalog.components.numbers
import ru.mobui.kanji.feature.catalog.presentation.KanjiRows

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSelection
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing

internal class CatalogDetailViewModel(
    saved: SavedStateHandle,
    repository: CatalogRepository,
    val request: DetailRequest,
    private val language: String,
    private val loadDrawing: suspend (Int) -> ReferenceDrawing?,
    compute: CoroutineDispatcher = Dispatchers.Default,
) : CatalogStateViewModel<String, DetailContent>(repository, compute) {
    val search = CatalogSearchState(saved, "detail.search")
    private var drawingLoaded = false
    private var drawing: ReferenceDrawing? = null

    init {
        retry()
    }

    override fun inputs() = search.state.map { it.query }

    override suspend fun content(snapshot: CatalogSnapshot, input: String): DetailContent {
        val source = request.sourceId?.let { snapshot.sourceById[it] }
        return when (val target = request) {
            is DetailRequest.Word -> {
                val word = snapshot.wordById[target.wordId]
                DetailContent.Word(word, word?.let(snapshot::literalKanji).orEmpty(), source)
            }
            is DetailRequest.Kanji -> {
                if (!drawingLoaded) {
                    drawing = loadDrawing(target.kanjiId)
                    drawingLoaded = true
                }
                val kanji = snapshot.kanjiById[target.kanjiId]
                DetailContent.Kanji(
                    kanji,
                    drawing,
                    kanji?.radicalId?.let { snapshot.radicalById[it] },
                    snapshot
                        .words(
                            CatalogSelection(target.sourceId, kanjiId = target.kanjiId),
                            "",
                            language,
                        )
                        .size,
                    source,
                )
            }
            is DetailRequest.Radical -> {
                val entries =
                    snapshot.kanji(
                        CatalogSelection(target.sourceId, radicalId = target.radicalId),
                        input,
                        language,
                    )
                DetailContent.Radical(
                    snapshot.radicalById[target.radicalId],
                    KanjiRows(entries, entries.size, snapshot.numbers(target.sourceId)),
                    source,
                )
            }
        }
    }
}
