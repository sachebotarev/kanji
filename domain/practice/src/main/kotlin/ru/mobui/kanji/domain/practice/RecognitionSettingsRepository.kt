package ru.mobui.kanji.domain.practice

/** Durable user preferences. Null means no preferences have been saved yet. */
interface RecognitionSettingsRepository {
    suspend fun load(): RecognitionSettings?
    suspend fun save(settings: RecognitionSettings)
}
