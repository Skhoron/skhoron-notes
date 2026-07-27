package dev.skhoron.notes.data.local

import androidx.room.TypeConverter
import dev.skhoron.notes.data.local.entity.AttachmentType
import dev.skhoron.notes.data.local.entity.ChecklistItem
import dev.skhoron.notes.data.local.entity.CompressionAlgo
import dev.skhoron.notes.data.local.entity.NoteType
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromCompressionAlgo(value: CompressionAlgo): String = value.name

    @TypeConverter
    fun toCompressionAlgo(value: String): CompressionAlgo =
        runCatching { CompressionAlgo.valueOf(value) }.getOrDefault(CompressionAlgo.NONE)

    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String = value.name

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType =
        runCatching { AttachmentType.valueOf(value) }.getOrDefault(AttachmentType.OTHER)

    @TypeConverter
    fun fromNoteType(value: NoteType): String = value.name

    @TypeConverter
    fun toNoteType(value: String): NoteType =
        runCatching { NoteType.valueOf(value) }.getOrDefault(NoteType.TEXT)

    @TypeConverter
    fun fromChecklist(items: List<ChecklistItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("done", item.done)
                }
            )
        }
        return arr.toString()
    }

    @TypeConverter
    fun toChecklist(value: String): List<ChecklistItem> {
        if (value.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(value)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ChecklistItem(
                    id = obj.optString("id"),
                    text = obj.optString("text"),
                    done = obj.optBoolean("done", false)
                )
            }
        }.getOrDefault(emptyList())
    }
}