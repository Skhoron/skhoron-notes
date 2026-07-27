package dev.skhoron.notes.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.skhoron.notes.data.local.entity.ChecklistItem
import dev.skhoron.notes.data.local.entity.CompressionAlgo
import dev.skhoron.notes.data.local.entity.NoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun checklistRoundTripPreservesOrderAndDoneState() {
        val items = listOf(
            ChecklistItem(id = "1", text = "Хлеб", done = false),
            ChecklistItem(id = "2", text = "Молоко", done = true),
            ChecklistItem(id = "3", text = "Кириллица и emoji 🥖", done = false)
        )

        val json = converters.fromChecklist(items)
        val restored = converters.toChecklist(json)

        assertEquals(items.size, restored.size)
        assertEquals(items.map { it.id }, restored.map { it.id })
        assertEquals(items.map { it.text }, restored.map { it.text })
        assertEquals(items.map { it.done }, restored.map { it.done })
    }

    @Test
    fun emptyChecklistRoundTripsToEmptyList() {
        val json = converters.fromChecklist(emptyList())
        val restored = converters.toChecklist(json)
        assertTrue(restored.isEmpty())
    }

    @Test
    fun malformedJsonFallsBackToEmptyListInsteadOfCrashing() {
        // Защита от повреждённых данных (например, после ручного редактирования файла БД) —
        // конвертер не должен ронять приложение, а должен деградировать в пустой список.
        val restored = converters.toChecklist("это не json вообще")
        assertTrue(restored.isEmpty())
    }

    @Test
    fun noteTypeRoundTrip() {
        assertEquals(NoteType.CHECKLIST, converters.toNoteType(converters.fromNoteType(NoteType.CHECKLIST)))
        assertEquals(NoteType.TEXT, converters.toNoteType(converters.fromNoteType(NoteType.TEXT)))
    }

    @Test
    fun unknownCompressionAlgoStringFallsBackToNone() {
        assertEquals(CompressionAlgo.NONE, converters.toCompressionAlgo("НЕЧТО_НЕСУЩЕСТВУЮЩЕЕ"))
    }
}