package dev.skhoron.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.skhoron.notes.data.local.entity.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY sortOrder ASC")
    fun getRootFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY sortOrder ASC")
    fun getSubfolders(parentId: String): Flow<List<Folder>>

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<Folder>>

    @Query("SELECT COUNT(*) FROM notes WHERE folderId = :folderId AND isInTrash = 0")
    fun getNoteCount(folderId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: Folder)

    @Update
    suspend fun update(folder: Folder)

    @Delete
    suspend fun delete(folder: Folder)
}