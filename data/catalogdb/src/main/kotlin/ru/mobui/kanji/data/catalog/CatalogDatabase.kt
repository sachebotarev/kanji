package ru.mobui.kanji.data.catalog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities =
        [
            CatalogMetadataRow::class,
            RadicalsRow::class,
            RadicalsTranslationsRow::class,
            KanjiRow::class,
            KanjiTranslationsRow::class,
            KanjiReadingsRow::class,
            WordsRow::class,
            WordsTranslationsRow::class,
            WordReadingPartsRow::class,
            SourcesRow::class,
            SourcesTranslationsRow::class,
            SourceKanjiRow::class,
            SourceWordsRow::class,
            InstallationRow::class,
            DictionaryTagsRow::class,
            DictionaryTagsTranslationsRow::class,
            WordTagsRow::class,
            RecipesRow::class,
            RecipesTranslationsRow::class,
            RecipeMaterialsRow::class,
            RecipeMaterialChaptersRow::class,
            RecipeMaterialKanjiRow::class,
            RecipeMaterialWordsRow::class,
            RecipeMaterialTagsRow::class,
            RecipeMaterialJlptRow::class,
            TrainingsRow::class,
            TrainingsRecipeTranslationsRow::class,
            TrainingTasksRow::class,
            TrainingTaskTranslationsRow::class,
            TrainingTaskSymbolsRow::class,
        ],
    version = 10,
    exportSchema = true,
)
internal abstract class CatalogDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun recipesDao(): RecipesDao
    abstract fun trainingsDao(): TrainingsDao

    companion object {
        private fun installOriginChecks(db: SupportSQLiteDatabase) {
            for (action in listOf("INSERT", "UPDATE")) db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS check_words_origin_${action.lowercase()} BEFORE $action ON words " +
                    "WHEN NEW.is_user_created NOT IN (0,1) BEGIN SELECT RAISE(ABORT, 'Invalid word origin'); END",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS preserve_words_origin BEFORE UPDATE OF is_user_created ON words " +
                    "WHEN NEW.is_user_created <> OLD.is_user_created BEGIN SELECT RAISE(ABORT, 'Word origin is immutable'); END",
            )
        }

        /** Triggers/indexes are static schema; install once at create (incl. destructive recreate). */
        private fun installStaticChecks(context: Context, db: SupportSQLiteDatabase) {
            installOriginChecks(db)
            installRecipeChecks(db)
            installTrainingChecks(db)
            context.assets.open("catalog-checks.sql").bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.forEach {
                    db.execSQL(it.replace("CREATE TRIGGER ", "CREATE TRIGGER IF NOT EXISTS "))
                }
            }
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS source_words_with_kanji " +
                    "ON source_words(source_id,word_id,kanji_id) WHERE kanji_id IS NOT NULL",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS source_words_without_kanji " +
                    "ON source_words(source_id,word_id) WHERE kanji_id IS NULL",
            )
        }

        fun open(context: Context, name: String = "kanji.db"): CatalogDatabase =
            Room.databaseBuilder(context.applicationContext, CatalogDatabase::class.java, name)
                // Pre-release database replacement, explicitly authorized; no migrations.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(
                    object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            installStaticChecks(context, db)
                        }
                    },
                )
                .build()
    }
}
