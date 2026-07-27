package dev.skhoron.notes.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dev.skhoron.notes.data.local.dao.AttachmentDao
import dev.skhoron.notes.data.local.dao.FolderDao
import dev.skhoron.notes.data.local.dao.NoteDao
import dev.skhoron.notes.data.local.dao.TagDao
import dev.skhoron.notes.data.local.entity.Attachment
import dev.skhoron.notes.data.local.entity.AttachmentType
import dev.skhoron.notes.data.local.entity.Folder
import dev.skhoron.notes.data.local.entity.Note
import dev.skhoron.notes.data.local.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class NotesRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao,
    private val attachmentDao: AttachmentDao,
    private val appContext: Context
) {
    // ---- Заметки ----
    fun activeNotes() = noteDao.getAllActive()
    fun favorites() = noteDao.getFavorites()
    fun pinned() = noteDao.getPinned()
    fun trash() = noteDao.getTrash()
    fun archived() = noteDao.getArchived()
    fun byFolder(folderId: String) = noteDao.getByFolder(folderId)
    fun search(query: String) = noteDao.search(query)
    suspend fun getNote(id: String) = noteDao.getById(id)

    suspend fun saveNote(note: Note) = noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
    suspend fun createNote(note: Note) = noteDao.insert(note)
    suspend fun moveToTrash(id: String) = noteDao.moveToTrash(id)
    suspend fun restoreFromTrash(id: String) = noteDao.restoreFromTrash(id)
    suspend fun deletePermanently(note: Note) {
        attachmentDao.deleteAllForNote(note.id)
        deleteAttachmentFiles(note.id)
        noteDao.delete(note)
    }
    suspend fun emptyTrash() = noteDao.emptyTrash()
    suspend fun markOpened(id: String) = noteDao.markOpened(id)
    suspend fun duplicate(note: Note) = noteDao.duplicate(note)

    // ---- Папки (поддержка вложенности произвольной глубины через parentId) ----
    fun rootFolders() = folderDao.getRootFolders()
    fun subfolders(parentId: String) = folderDao.getSubfolders(parentId)
    fun allFolders() = folderDao.getAll()
    suspend fun createFolder(name: String, parentId: String? = null) =
        folderDao.insert(Folder(name = name, parentId = parentId))
    suspend fun renameFolder(folder: Folder, newName: String) = folderDao.update(folder.copy(name = newName))
    suspend fun deleteFolder(folder: Folder) = folderDao.delete(folder)

    // ---- Теги ----
    fun allTags() = tagDao.getAll()
    fun notesByTag(tagId: String) = tagDao.getNotesByTag(tagId)
    fun tagsForNote(noteId: String) = tagDao.getTagsForNote(noteId)
    suspend fun ensureTag(name: String): Tag {
        tagDao.getByName(name)?.let { return it }
        val tag = Tag(name = name)
        tagDao.insert(tag)
        return tag
    }

    // ---- Вложения: копируем файл из content:// URI в приватную песочницу приложения ----
    fun attachmentsForNote(noteId: String) = attachmentDao.getForNote(noteId)

    suspend fun attachFile(noteId: String, sourceUri: Uri, displayName: String, mimeType: String?): Attachment =
        withContext(Dispatchers.IO) {
            val dir = File(appContext.filesDir, "attachments/$noteId").apply { mkdirs() }
            val safeId = UUID.randomUUID().toString()
            val destFile = File(dir, "${safeId}_${sanitizeFileName(displayName)}")

            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }

            val resolvedMime = mimeType
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(destFile.extension)
                ?: "application/octet-stream"

            val attachment = Attachment(
                id = safeId,
                noteId = noteId,
                fileName = displayName,
                mimeType = resolvedMime,
                type = classify(resolvedMime),
                relativePath = "attachments/$noteId/${destFile.name}",
                sizeBytes = destFile.length()
            )
            attachmentDao.insert(attachment)
            attachment
        }

    suspend fun removeAttachment(attachment: Attachment) = withContext(Dispatchers.IO) {
        File(appContext.filesDir, attachment.relativePath).delete()
        attachmentDao.delete(attachment)
    }

    fun fileForAttachment(attachment: Attachment): File = File(appContext.filesDir, attachment.relativePath)

    private fun deleteAttachmentFiles(noteId: String) {
        File(appContext.filesDir, "attachments/$noteId").deleteRecursively()
    }

    private fun classify(mimeType: String): AttachmentType = when {
        mimeType.startsWith("image/") -> AttachmentType.IMAGE
        mimeType.startsWith("video/") -> AttachmentType.VIDEO
        mimeType.startsWith("audio/") -> AttachmentType.AUDIO
        mimeType == "application/pdf" || mimeType.contains("document") || mimeType.contains("text") -> AttachmentType.DOCUMENT
        else -> AttachmentType.OTHER
    }

    /**
     * Защита от path traversal: имя файла, выбранного пользователем через системный
     * файловый пикер, могло содержать "../", "/", "\" и попытаться записать данные
     * за пределы папки attachments/<noteId>/. Берём только последний сегмент пути
     * и убираем всё, кроме букв/цифр/точки/дефиса/подчёркивания.
     */
    private fun sanitizeFileName(rawName: String): String {
        val lastSegment = rawName.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = lastSegment.replace(Regex("[^A-Za-z0-9._\\-]"), "_")
        return cleaned.ifBlank { "file" }.take(200)
    }
}