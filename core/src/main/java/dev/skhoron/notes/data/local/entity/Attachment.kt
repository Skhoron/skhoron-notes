package dev.skhoron.notes.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class AttachmentType {
    IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER
}

/**
 * Вложение любого типа файла (фото, видео, PDF, произвольный файл) к заметке.
 * Сам файл копируется в приватную папку приложения (filesDir/attachments/<noteId>/<id>_<name>),
 * наружу ничего не расшаривается кроме как через FileProvider по явному запросу пользователя.
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class Attachment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "noteId")
    val noteId: String,

    @ColumnInfo(name = "fileName")
    val fileName: String,

    @ColumnInfo(name = "mimeType")
    val mimeType: String,

    @ColumnInfo(name = "type")
    val type: AttachmentType,

    // Путь относительно filesDir приложения, не абсолютный путь ОС
    @ColumnInfo(name = "relativePath")
    val relativePath: String,

    @ColumnInfo(name = "sizeBytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)