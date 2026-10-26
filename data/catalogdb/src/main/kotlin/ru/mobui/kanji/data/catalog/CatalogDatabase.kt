package ru.mobui.kanji.data.catalog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [
    CatalogMetadataRow::class, RadicalsRow::class, RadicalsTranslationsRow::class, KanjiRow::class, KanjiTranslationsRow::class, KanjiReadingsRow::class, WordsRow::class, WordsTranslationsRow::class, WordReadingPartsRow::class, SourcesRow::class, SourcesTranslationsRow::class, SourceKanjiRow::class, SourceWordsRow::class, InstallationRow::class
], version = 2, exportSchema = true)
internal abstract class CatalogDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao

    companion object {
        private fun installOriginChecks(db: SupportSQLiteDatabase) {
            for (action in listOf("INSERT", "UPDATE")) db.execSQL(
                "CREATE TRIGGER check_words_origin_${action.lowercase()} BEFORE $action ON words " +
                    "WHEN NEW.is_user_created NOT IN (0,1) BEGIN SELECT RAISE(ABORT, 'Invalid word origin'); END")
            db.execSQL("CREATE TRIGGER preserve_words_origin BEFORE UPDATE OF is_user_created ON words " +
                "WHEN NEW.is_user_created <> OLD.is_user_created BEGIN SELECT RAISE(ABORT, 'Word origin is immutable'); END")
        }

        fun open(context: Context, name: String = "learning.db"): CatalogDatabase =
            Room.databaseBuilder(context.applicationContext, CatalogDatabase::class.java, name)
                .addMigrations(object : Migration(1, 2) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        // Existing entries have no proven manual origin: retain read-only ownership.
                        db.execSQL("ALTER TABLE words ADD COLUMN is_user_created INTEGER NOT NULL DEFAULT 0")
                        installOriginChecks(db)
                        // Room cannot represent partial indexes in its exported entity schema.
                        // Remove them only for validation; onOpen restores them before exposing the DB.
                        db.execSQL("DROP INDEX IF EXISTS source_words_with_kanji")
                        db.execSQL("DROP INDEX IF EXISTS source_words_without_kanji")
                    }
                })
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS source_words_with_kanji ON source_words(source_id,word_id,kanji_id) WHERE kanji_id IS NOT NULL")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS source_words_without_kanji ON source_words(source_id,word_id) WHERE kanji_id IS NULL")
                    }
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        installOriginChecks(db)
                        context.assets.open("catalog-checks.sql").bufferedReader().useLines { lines ->
                            lines.filter { it.isNotBlank() }.forEach { db.execSQL(it) }
                        }
                        db.execSQL("CREATE UNIQUE INDEX source_words_with_kanji ON source_words(source_id,word_id,kanji_id) WHERE kanji_id IS NOT NULL")
                        db.execSQL("CREATE UNIQUE INDEX source_words_without_kanji ON source_words(source_id,word_id) WHERE kanji_id IS NULL")
                    }
                }).build()
    }
}
