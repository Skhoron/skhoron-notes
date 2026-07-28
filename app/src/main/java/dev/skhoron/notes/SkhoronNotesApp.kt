package dev.skhoron.notes

import android.app.Application
import dev.skhoron.notes.data.local.DatabaseFactory
import dev.skhoron.notes.data.local.SkhoronNotesDatabase
import dev.skhoron.notes.data.repository.NotesRepository
import dev.skhoron.notes.data.settings.AppSettingsStore
import dev.skhoron.notes.security.AppLockController
import dev.skhoron.notes.security.SecurityStore

/**
 * Application-класс модуля :app — сознательно тонкий. Вся сборка БД (включая то, что
 * она зашифрована через SQLCipher) спрятана в DatabaseFactory.build() внутри :core;
 * этот класс просто просит готовые объекты, не зная деталей их реализации.
 */
class SkhoronNotesApp : Application() {

    lateinit var database: SkhoronNotesDatabase
        private set

    lateinit var repository: NotesRepository
        private set

    lateinit var securityStore: SecurityStore
        private set

    lateinit var settingsStore: AppSettingsStore
        private set

    lateinit var appLockController: AppLockController
        private set

    override fun onCreate() {
        super.onCreate()

        database = DatabaseFactory.build(applicationContext)

        repository = NotesRepository(
            noteDao = database.noteDao(),
            folderDao = database.folderDao(),
            tagDao = database.tagDao(),
            attachmentDao = database.attachmentDao(),
            appContext = applicationContext
        )

        securityStore = SecurityStore(applicationContext)
        settingsStore = AppSettingsStore(applicationContext)

        appLockController = AppLockController(securityStore)
        appLockController.register()
    }
}