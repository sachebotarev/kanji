package ru.mobui.kanji

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.mobui.kanji.checker.readability.DigitalInkKanjiChecker
import ru.mobui.kanji.data.settings.DataStoreRecognitionSettingsRepository
import ru.mobui.kanji.domain.practice.RecognitionSettingsRepository

/** Application lifetime dependencies, injected into screen state owners by the Activity. */
class KanjiApplication : Application() {
    val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    internal val readability = DigitalInkKanjiChecker()
    internal val japaneseModel =
        JapaneseModelLoader(
            persistenceScope,
            readability::isModelDownloaded,
            readability::downloadModel,
        )

    override fun onCreate() {
        super.onCreate()
        japaneseModel.load()
    }

    private val wordStorage by lazy { ru.mobui.kanji.data.catalog.RoomCatalogRepository(this) }
    val recipesRepository: ru.mobui.kanji.domain.catalog.RecipesRepository
        get() = wordStorage.recipesRepository
    val catalog: ru.mobui.kanji.domain.catalog.CatalogRepository
        get() = wordStorage

    val catalogRevision: kotlinx.coroutines.flow.StateFlow<Long>
        get() = wordStorage.revision

    val dictionary: ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository
        get() = wordStorage

    val recognitionSettings: RecognitionSettingsRepository by lazy {
        DataStoreRecognitionSettingsRepository(this)
    }
}
