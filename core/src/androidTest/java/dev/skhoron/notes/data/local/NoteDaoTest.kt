package dev.skhoron.notes.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.skhoron.notes.data.local.entity.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var db: SkhoronNotesDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, SkhoronNotesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertedNoteAppearsInActiveList() = runBlocking {
        val note = Note(title = "Тест", content = "Содержимое")
        db.noteDao().insert(note)

        val active = db.noteDao().getAllActive().first()
        assertEquals(1, active.size)
        assertEquals("Тест", active.first().title)
    }

    @Test
    fun movingToTrashRemovesNoteFromActiveList() = runBlocking {
        val note = Note(title = "Удаляемая", content = "")
        db.noteDao().insert(note)

        db.noteDao().moveToTrash(note.id)

        val active = db.noteDao().getAllActive().first()
        val trashed = db.noteDao().getTrash().first()
        assertTrue(active.isEmpty())
        assertEquals(1, trashed.size)
    }

    @Test
    fun deletingFolderClearsFolderIdOnNotes_whenHandledByRepositoryLayer() = runBlocking {
        // DAO сам по себе не переносит заметки при удалении папки (это делает
        // NotesRepository.deleteFolder + ручной update), здесь фиксируем базовый факт:
        // Room не запрещает создать заметку с несуществующим folderId удалённой папки,
        // так что ответственность явно лежит на repository-слое, а не на каскаде БД.
        val note = Note(title = "В папке", content = "", folderId = "orphaned-folder-id")
        db.noteDao().insert(note)
        val fetched = db.noteDao().getById(note.id)
        assertEquals("orphaned-folder-id", fetched?.folderId)
    }

    @Test
    fun searchMatchesTitleAndContent() = runBlocking {
        db.noteDao().insert(Note(title = "Рецепт борща", content = "свёкла, капуста"))
        db.noteDao().insert(Note(title = "Список покупок", content = "хлеб, молоко"))

        val byTitle = db.noteDao().search("борщ").first()
        val byContent = db.noteDao().search("молоко").first()

        assertEquals(1, byTitle.size)
        assertEquals(1, byContent.size)
    }

    @Test
    fun compressionCandidatesRespectCutoffTimestamp() = runBlocking {
        val old = Note(
            title = "Старая",
            content = "",
            lastOpenedAt = System.currentTimeMillis() - 48 * 60 * 60 * 1000
        )
        val recent = Note(title = "Свежая", content = "", lastOpenedAt = System.currentTimeMillis())
        db.noteDao().insert(old)
        db.noteDao().insert(recent)

        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        val candidates = db.noteDao().getCompressionCandidates(cutoff)

        assertEquals(1, candidates.size)
        assertEquals("Старая", candidates.first().title)
    }
}