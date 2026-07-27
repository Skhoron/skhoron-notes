package dev.skhoron.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.skhoron.notes.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActive(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isArchived = 0 AND isFavorite = 1 ORDER BY isPinned DESC, updatedAt DESC")
    fun getFavorites(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isArchived = 0 AND isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinned(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isInTrash = 0 ORDER BY isPinned DESC, manualOrder ASC, updatedAt DESC")
    fun getByFolder(folderId: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 1 ORDER BY updatedAt DESC")
    fun getTrash(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchived(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Note?

    @Query("""
        SELECT * FROM notes
        WHERE isInTrash = 0
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun search(query: String): Flow<List<Note>>

    // Кандидаты на автосжатие: не открывались дольше threshold, ещё не сжаты
    @Query("""
        SELECT * FROM notes
        WHERE isCompressed = 0
        AND isInTrash = 0
        AND lastOpenedAt < :cutoffTimestamp
    """)
    suspend fun getCompressionCandidates(cutoffTimestamp: Long): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("UPDATE notes SET isInTrash = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isInTrash = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restoreFromTrash(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE isInTrash = 1")
    suspend fun emptyTrash()

    @Query("UPDATE notes SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun markOpened(id: String, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun duplicate(original: Note) {
        insert(original.copy(id = java.util.UUID.randomUUID().toString(), title = "${original.title} (копия)"))
    }
}