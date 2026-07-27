package dev.skhoron.notes.data.local

import android.content.Context
import androidx.room.Room
import dev.skhoron.notes.security.DbKeyStore
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

/**
 * Вся логика сборки базы данных (включая шифрование через SQLCipher и цепочку миграций)
 * инкапсулирована здесь, внутри :core. Модуль :app вообще не знает, что БД зашифрована
 * SQLCipher-ом — он просто вызывает DatabaseFactory.build(context) и получает готовый
 * SkhoronNotesDatabase. Это и есть смысл разделения core/app: если завтра решишь сменить
 * механизм шифрования БД, менять нужно только этот файл, ни строчки в :app трогать не придётся.
 */
object DatabaseFactory {
    fun build(context: Context): SkhoronNotesDatabase {
        // SQLCipher требует нативную загрузку своей реализации SQLite вместо системной.
        SQLiteDatabase.loadLibs(context)

        val passphrase = DbKeyStore(context).getOrCreatePassphrase()
        val supportFactory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            SkhoronNotesDatabase::class.java,
            SkhoronNotesDatabase.DATABASE_NAME
        )
            .openHelperFactory(supportFactory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
}