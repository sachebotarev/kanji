package ru.mobui.kanji

import android.app.Application
import android.util.Log
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
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
        bridgeAppLogs()
        japaneseModel.load()
    }

    private val wordStorage by lazy { ru.mobui.kanji.data.catalog.RoomCatalogRepository(this) }
    val recipesRepository: ru.mobui.kanji.domain.catalog.RecipesRepository
        get() = wordStorage.recipesRepository

    val trainingsRepository: ru.mobui.kanji.domain.practice.TrainingsRepository
        get() = wordStorage.trainingsRepository

    val catalog: ru.mobui.kanji.domain.catalog.CatalogRepository
        get() = wordStorage

    val catalogRevision: kotlinx.coroutines.flow.StateFlow<Long>
        get() = wordStorage.revision

    val dictionary: ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository
        get() = wordStorage

    internal val trainingPersistence by lazy { TrainingPersistence(trainingsRepository, persistenceScope) }

    val recognitionSettings: RecognitionSettingsRepository by lazy {
        DataStoreRecognitionSettingsRepository(this)
    }
}

/** Routes java.util.logging output from app, domain and feature components to android.util.Log. */
private fun bridgeAppLogs() {
    val handler = AppLogHandler()
    for (name in listOf("ru.mobui.kanji", "CombinedKanjiChecker")) {
        val logger = Logger.getLogger(name)
        logger.useParentHandlers = false
        if (logger.handlers.none { it is AppLogHandler }) logger.addHandler(handler)
    }
}

private class AppLogHandler : Handler() {
    override fun publish(record: LogRecord) {
        if (!isLoggable(record)) return
        val priority = when {
            record.level.intValue() >= Level.SEVERE.intValue() -> Log.ERROR
            record.level.intValue() >= Level.WARNING.intValue() -> Log.WARN
            record.level.intValue() >= Level.INFO.intValue() -> Log.INFO
            else -> Log.DEBUG
        }
        val tag = record.loggerName?.substringAfterLast('.') ?: "KanjiApp"
        Log.println(priority, tag, record.message.orEmpty())
        record.thrown?.let { Log.println(priority, tag, Log.getStackTraceString(it)) }
    }

    override fun flush() = Unit

    override fun close() = Unit
}
