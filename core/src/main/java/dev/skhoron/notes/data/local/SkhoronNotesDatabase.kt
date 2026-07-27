package dev.skhoron.notes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.skhoron.notes.data.local.dao.AttachmentDao
import dev.skhoron.notes.data.local.dao.FolderDao
import dev.skhoron.notes.data.local.dao.NoteDao
import dev.skhoron.notes.data.local.dao.TagDao
import dev.skhoron.notes.data.local.entity.Attachment
import dev.skhoron.notes.data.local.entity.Folder
import dev.skhoron.notes.data.local.entity.Note
import dev.skhoron.notes.data.local.entity.NoteTagCrossRef
import dev.skhoron.notes.data.local.entity.Tag

@Database(
    entities = [Note::class, Folder::class, Tag::class, NoteTagCrossRef::class, Attachment::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SkhoronNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        const val DATABASE_NAME = "skhoron_notes.db"
    }
}