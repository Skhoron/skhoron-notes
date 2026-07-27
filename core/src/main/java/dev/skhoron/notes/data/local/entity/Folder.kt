package dev.skhoron.notes.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
data class Folder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    // null = папка верхнего уровня
    @ColumnInfo(name = "parentId")
    val parentId: String? = null,

    @ColumnInfo(name = "iconName")
    val iconName: String? = null,

    @ColumnInfo(name = "colorHex")
    val colorHex: String? = null,

    @ColumnInfo(name = "sortOrder")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "isExpanded")
    val isExpanded: Boolean = true,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)