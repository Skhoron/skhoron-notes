package dev.skhoron.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.skhoron.notes.data.local.entity.Attachment
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun getForNote(noteId: String): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC")
    suspend fun getForNoteOnce(noteId: String): List<Attachment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: Attachment)

    @Delete
    suspend fun delete(attachment: Attachment)

    @Query("DELETE FROM attachments WHERE noteId = :noteId")
    suspend fun deleteAllForNote(noteId: String)

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM attachments")
    suspend fun getTotalSizeBytes(): Long
}