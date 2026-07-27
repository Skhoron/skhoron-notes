package dev.skhoron.notes.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class CompressionAlgo {
    NONE, ZSTD, BROTLI, LZMA2, GZIP
}

enum class NoteType { TEXT, CHECKLIST }

/** Пункт чек-листа. Хранится не отдельной Room-таблицей, а сериализованным списком
 *  на самой заметке (через Converters/org.json) — для чек-листов из нескольких пунктов
 *  отдельная таблица с JOIN'ами была бы избыточна. */
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val done: Boolean = false
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("folderId"), Index("updatedAt"), Index("isPinned"), Index("isFavorite")]
)
data class Note(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "type", defaultValue = "TEXT")
    val type: NoteType = NoteType.TEXT,

    @ColumnInfo(name = "checklist")
    val checklist: List<ChecklistItem> = emptyList(),

    // Хранится как есть, если isCompressed = false.
    // Если true — это base64/бинарь после сжатия, распаковка идёт в репозитории, не в UI.
    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "folderId")
    val folderId: String? = null,

    @ColumnInfo(name = "colorHex")
    val colorHex: String? = null,

    @ColumnInfo(name = "isPinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "isInTrash")
    val isInTrash: Boolean = false,

    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "reminderAt")
    val reminderAt: Long? = null,

    // Поля под фичу автосжатия из обсуждения — заложены на уровне схемы сразу,
    // чтобы не ломать миграцией позже.
    @ColumnInfo(name = "isCompressed", defaultValue = "0")
    val isCompressed: Boolean = false,

    @ColumnInfo(name = "compressionAlgo", defaultValue = "NONE")
    val compressionAlgo: CompressionAlgo = CompressionAlgo.NONE,

    @ColumnInfo(name = "uncompressedSizeBytes")
    val uncompressedSizeBytes: Long? = null,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    // Последний раз когда заметку реально открывали — нужно для триггера
    // "не открывалась 24ч -> сжать", отдельно от updatedAt (который меняется при редактировании)
    @ColumnInfo(name = "lastOpenedAt")
    val lastOpenedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "manualOrder")
    val manualOrder: Int = 0,

    // null = использовать формат по умолчанию из глобальных настроек (.md/.txt/.json/.yml/...).
    // Если задано — эта конкретная заметка при экспорте/сохранении в файл использует
    // именно это расширение, вплоть до произвольного пользовательского ("hdhdhd" и т.п.)
    @ColumnInfo(name = "customSaveExtension")
    val customSaveExtension: String? = null,

    // Позиция карточки в режиме свободной раскладки (drag&drop в любое место экрана)
    @ColumnInfo(name = "freeLayoutX")
    val freeLayoutX: Float? = null,

    @ColumnInfo(name = "freeLayoutY")
    val freeLayoutY: Float? = null
)