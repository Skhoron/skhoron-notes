package dev.skhoron.notes.ui.editor

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.skhoron.notes.data.local.entity.Attachment
import dev.skhoron.notes.data.local.entity.ChecklistItem
import dev.skhoron.notes.data.local.entity.Note
import dev.skhoron.notes.data.local.entity.NoteType
import dev.skhoron.notes.data.repository.NotesRepository
import dev.skhoron.notes.ui.theme.NoteColorPalette
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun NoteEditorScreen(
    noteId: String,
    repository: NotesRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var note by remember { mutableStateOf<Note?>(null) }
    val attachments by remember(noteId) { repository.attachmentsForNote(noteId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(noteId) {
        repository.markOpened(noteId)
        note = repository.getNote(noteId)
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uris.forEach { uri ->
                    val name = queryDisplayName(uri, context) ?: "файл"
                    val mime = context.contentResolver.getType(uri)
                    repository.attachFile(noteId, uri, name, mime)
                }
            }
        }
    }

    val current = note
    if (current == null) {
        Box(modifier = Modifier.fillMaxSize()) // короткая загрузка из Room
        return
    }

    fun persist(transform: (Note) -> Note) {
        val updated = transform(current)
        note = updated
        scope.launch { repository.saveNote(updated) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Верхняя панель действий
        Row(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { persist { it.copy(isPinned = !it.isPinned) } }) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "Закрепить",
                    tint = if (current.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { persist { it.copy(isFavorite = !it.isFavorite) } }) {
                Icon(
                    if (current.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Избранное",
                    tint = if (current.isFavorite) Color(0xFFE8B339) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { pickFileLauncher.launch(arrayOf("*/*")) }) {
                Icon(Icons.Filled.AttachFile, contentDescription = "Прикрепить файл")
            }
            IconButton(onClick = {
                scope.launch {
                    repository.moveToTrash(current.id)
                    onBack()
                }
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        }

        TextField(
            value = current.title,
            onValueChange = { v -> persist { it.copy(title = v) } },
            placeholder = { Text("Заголовок заметки") },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )

        // Переключатель Текст / Чек-лист
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            listOf(NoteType.TEXT to "Текст", NoteType.CHECKLIST to "Чек-лист").forEach { (type, label) ->
                val selected = current.type == type
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { persist { it.copy(type = type) } }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        when (current.type) {
            NoteType.TEXT -> {
                OutlinedTextField(
                    value = current.content,
                    onValueChange = { v -> persist { it.copy(content = v) } },
                    placeholder = { Text("Начните печатать в Markdown...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
                )
            }
            NoteType.CHECKLIST -> {
                ChecklistEditor(
                    items = current.checklist,
                    onItemsChange = { items -> persist { it.copy(checklist = items) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (attachments.isNotEmpty()) {
            AttachmentsRow(attachments = attachments, onRemove = { a ->
                scope.launch { repository.removeAttachment(a) }
            })
        }

        // Цветовая метка заметки
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NoteColorPalette.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color ?: MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val hex = color?.let { c ->
                                String.format("#%06X", 0xFFFFFF and c.toArgb())
                            }
                            persist { it.copy(colorHex = hex) }
                        }
                )
            }
        }
    }
}

@Composable
private fun ChecklistEditor(
    items: List<ChecklistItem>,
    onItemsChange: (List<ChecklistItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    var newItemText by remember { mutableStateOf("") }

    LazyColumn(modifier = modifier.padding(horizontal = 12.dp)) {
        items(items, key = { it.id }) { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.done,
                    onCheckedChange = { checked ->
                        onItemsChange(items.map { if (it.id == item.id) it.copy(done = checked) else it })
                    }
                )
                TextField(
                    value = item.text,
                    onValueChange = { v ->
                        onItemsChange(items.map { if (it.id == item.id) it.copy(text = v) else it })
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.5.sp,
                        textDecoration = if (item.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                        color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                )
                IconButton(onClick = { onItemsChange(items.filterNot { it.id == item.id }) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Удалить пункт", modifier = Modifier.size(16.dp))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Text("+", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                TextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    placeholder = { Text("Новый пункт...", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
                )
                IconButton(onClick = {
                    if (newItemText.isNotBlank()) {
                        onItemsChange(items + ChecklistItem(text = newItemText))
                        newItemText = ""
                    }
                }) {
                    Text("OK", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AttachmentsRow(attachments: List<Attachment>, onRemove: (Attachment) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(attachments, key = { it.id }) { att ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(att.fileName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Убрать вложение",
                    modifier = Modifier.size(14.dp).clickable { onRemove(att) }
                )
            }
        }
    }
}

private fun queryDisplayName(uri: Uri, context: android.content.Context): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    cursor.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex >= 0) return it.getString(nameIndex)
    }
    return null
}