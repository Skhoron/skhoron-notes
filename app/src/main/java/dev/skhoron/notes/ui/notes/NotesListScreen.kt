package dev.skhoron.notes.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.skhoron.notes.data.local.entity.Note
import dev.skhoron.notes.data.repository.NotesRepository
import kotlinx.coroutines.launch

@Composable
fun NotesListScreen(
    repository: NotesRepository,
    onOpenNote: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolders: () -> Unit
) {
    var tab by remember { mutableStateOf(NotesTab.ALL) }
    var view by remember { mutableStateOf(NotesViewMode.LIST) }
    var search by remember { mutableStateOf("") }
    val scope = rememberCoroutineScopeSafe()

    val notesFlow = remember(tab, search) {
        when {
            search.isNotBlank() -> repository.search(search)
            tab == NotesTab.FAVORITES -> repository.favorites()
            tab == NotesTab.PINNED -> repository.pinned()
            tab == NotesTab.ARCHIVED -> repository.archived()
            tab == NotesTab.TRASH -> repository.trash()
            else -> repository.activeNotes()
        }
    }
    val notes by notesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        val id = java.util.UUID.randomUUID().toString()
                        repository.createNote(Note(id = id, title = "", content = ""))
                        onOpenNote(id)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Новая заметка", tint = androidx.compose.ui.graphics.Color(0xFF08110C))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TopBar(
                tab = tab,
                view = view,
                onTabChange = { tab = it },
                onViewChange = { view = it },
                onOpenSettings = onOpenSettings,
                onOpenFolders = onOpenFolders
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Поиск по заметкам...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                singleLine = true
            )

            if (notes.isEmpty()) {
                EmptyState(isTrash = tab == NotesTab.TRASH)
            } else {
                when (view) {
                    NotesViewMode.LIST -> ListView(notes, onOpenNote)
                    NotesViewMode.GRID -> GridView(notes, onOpenNote)
                    NotesViewMode.FREE -> FreeView(notes, repository, onOpenNote)
                }
            }
        }
    }
}

@Composable
private fun rememberCoroutineScopeSafe() = androidx.compose.runtime.rememberCoroutineScope()

@Composable
private fun TopBar(
    tab: NotesTab,
    view: NotesViewMode,
    onTabChange: (NotesTab) -> Unit,
    onViewChange: (NotesViewMode) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolders: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenFolders) {
            Icon(Icons.Filled.Menu, contentDescription = "Папки")
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp)
        ) {
            listOf(
                NotesTab.ALL to "Все",
                NotesTab.FAVORITES to "Избранное",
                NotesTab.PINNED to "Закреплённые"
            ).forEach { (t, label) ->
                val selected = tab == t
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onTabChange(t) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        listOf(
            NotesViewMode.LIST to "☰",
            NotesViewMode.GRID to "▦",
            NotesViewMode.FREE to "⛶"
        ).forEach { (v, icon) ->
            val selected = view == v
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onViewChange(v) },
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 14.sp, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Настройки")
        }
    }
}

@Composable
private fun EmptyState(isTrash: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (isTrash) "Корзина пуста" else "Здесь пока пусто",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (!isTrash) {
            Text(
                "Нажмите + чтобы создать первую заметку",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ListView(notes: List<Note>, onOpenNote: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
        items(notes, key = { it.id }) { note ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenNote(note.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                note.colorHex?.let {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(34.dp)
                            .background(androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(it)))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        (if (note.isPinned) "📌 " else "") + note.title.ifBlank { "Без названия" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        note.content.take(120).ifBlank { "Пустая заметка" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (note.isFavorite) {
                    Text("★", color = androidx.compose.ui.graphics.Color(0xFFE8B339), fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun GridView(notes: List<Note>, onOpenNote: (String) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(12.dp)) {
        items(notes, key = { it.id }) { note ->
            Column(
                modifier = Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onOpenNote(note.id) }
                    .padding(14.dp)
            ) {
                Text(
                    (if (note.isPinned) "📌 " else "") + note.title.ifBlank { "Без названия" },
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    note.content.take(90).ifBlank { "Пустая заметка" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Свободная раскладка: карточки можно перетащить в любое место холста, позиция
 * сохраняется в Note.freeLayoutX/Y. Это то самое "1 2 3 4 или 2х2 или как угодно" —
 * расположение не привязано ни к какой сетке.
 */
@Composable
private fun FreeView(notes: List<Note>, repository: NotesRepository, onOpenNote: (String) -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var positions by remember(notes) {
        mutableStateOf(
            notes.withIndex().associate { (index, n) ->
                n.id to Offset(n.freeLayoutX ?: (20f + (index % 3) * 220f), n.freeLayoutY ?: (20f + (index / 3) * 160f))
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        notes.forEach { note ->
            val pos = positions[note.id] ?: Offset.Zero
            Column(
                modifier = Modifier
                    .size(width = 180.dp, height = 130.dp)
                    .then(
                        Modifier.graphicsLayerOffset(pos)
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(note.id) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val current = positions[note.id] ?: Offset.Zero
                                positions = positions.toMutableMap().apply {
                                    put(note.id, current + dragAmount)
                                }
                            },
                            onDragEnd = {
                                val p = positions[note.id] ?: Offset.Zero
                                scope.launch {
                                    repository.getNote(note.id)?.let { n ->
                                        repository.saveNote(n.copy(freeLayoutX = p.x, freeLayoutY = p.y))
                                    }
                                }
                            }
                        )
                    }
                    .clickable { onOpenNote(note.id) }
                    .padding(12.dp)
            ) {
                Text(
                    (if (note.isPinned) "📌 " else "") + note.title.ifBlank { "Без названия" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    note.content.take(60).ifBlank { "Пустая заметка" },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun Modifier.graphicsLayerOffset(offset: Offset): Modifier = this.then(
    Modifier.offset { androidx.compose.ui.unit.IntOffset(offset.x.toInt(), offset.y.toInt()) }
)