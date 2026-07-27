package dev.skhoron.notes.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграция версии 1 → 2: добавлены type/checklist/customSaveExtension/freeLayoutX/Y на Note,
 * и целиком новая таблица attachments. Написана вручную по diff'у между версиями схемы Note
 * (см. историю правок Note.kt) — раньше на этом месте стоял fallbackToDestructiveMigration(),
 * который бы стёр все заметки пользователей при обновлении приложения.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN type TEXT NOT NULL DEFAULT 'TEXT'")
        db.execSQL("ALTER TABLE notes ADD COLUMN checklist TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE notes ADD COLUMN customSaveExtension TEXT")
        db.execSQL("ALTER TABLE notes ADD COLUMN freeLayoutX REAL")
        db.execSQL("ALTER TABLE notes ADD COLUMN freeLayoutY REAL")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS attachments (
                id TEXT NOT NULL PRIMARY KEY,
                noteId TEXT NOT NULL,
                fileName TEXT NOT NULL,
                mimeType TEXT NOT NULL,
                type TEXT NOT NULL,
                relativePath TEXT NOT NULL,
                sizeBytes INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(noteId) REFERENCES notes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_noteId ON attachments(noteId)")
    }
}

/**
 * Миграция версии 2 → 3: уникальный индекс на tags.name — раньше ensureTag() плодил
 * дубли строк с одинаковым именем тега, потому что insert() использовал REPLACE по id
 * (который всегда новый UUID), а уникального ограничения на name не было вообще.
 *
 * ВАЖНО: если к моменту этой миграции в БД уже есть дубликаты имён тегов (то есть кто-то
 * успел пожить на version=2 до этого фикса), CREATE UNIQUE INDEX здесь упадёт с ошибкой.
 * Поскольку проект ещё не публиковался (versionCode=1, README прямо говорит "не для прода"),
 * оставляю миграцию простой; если это когда-нибудь всплывёт у реальных пользователей —
 * миграцию нужно будет дополнить предварительным дедупликационным DELETE перед CREATE INDEX.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
    }
}