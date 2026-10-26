package ru.mobui.kanji

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.mobui.kanji.feature.dashboard.TrainingOverview
import java.util.UUID

/** UI prototype state. A persistent training repository and multiword sessions are separate work. */
internal class DashboardTrainingViewModel(
    private val savedState: SavedStateHandle,
    defaultName: String,
) : ViewModel() {
    private val initial = savedState.get<ArrayList<String>>("training.ids")?.mapIndexed { index, id ->
        TrainingOverview(id, savedState.get<ArrayList<String>>("training.names")!![index],
            savedState.get<IntArray>("training.words")!![index], savedState.get<IntArray>("training.seconds")!![index])
    } ?: listOf(TrainingOverview("default", defaultName))
    private val mutableTrainings = MutableStateFlow(initial)
    val trainings = mutableTrainings.asStateFlow()
    private val mutableSelected = MutableStateFlow(savedState.get<String>("training.selected")
        ?.takeIf { id -> initial.any { it.id == id } } ?: initial.first().id)
    val selected = mutableSelected.asStateFlow()

    fun select(id: String) {
        if (mutableTrainings.value.none { it.id == id }) return
        mutableSelected.value = id
        save()
    }

    fun submit(existingId: String?, name: String, words: Int, seconds: Int) {
        if (name.isBlank() || words !in 1..100 || seconds !in 10..600) return
        if (existingId != null && mutableTrainings.value.none { it.id == existingId }) return
        val entry = TrainingOverview(existingId ?: UUID.randomUUID().toString(), name.trim(), words, seconds)
        mutableTrainings.value = if (existingId == null) mutableTrainings.value + entry
            else mutableTrainings.value.map { if (it.id == existingId) entry else it }
        if (existingId == null) mutableSelected.value = entry.id
        save()
    }

    private fun save() {
        val entries = mutableTrainings.value
        savedState["training.ids"] = ArrayList(entries.map { it.id })
        savedState["training.names"] = ArrayList(entries.map { it.name })
        savedState["training.words"] = entries.map { it.wordsPerSession }.toIntArray()
        savedState["training.seconds"] = entries.map { it.secondsPerWord }.toIntArray()
        savedState["training.selected"] = mutableSelected.value
    }
}
