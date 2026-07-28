package dev.skhoron.notes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Графический ключ: сетка 3x3, пользователь проводит пальцем по точкам, порядок точек
 * (0..8) сериализуется в строку "0-4-8-..." и хешируется через тот же PBKDF2, что и PIN/пароль —
 * никакой отдельной "более слабой" ветки защиты для этого способа нет.
 */
@Composable
fun PatternPad(
    modifier: Modifier = Modifier,
    onPatternComplete: (List<Int>) -> Unit
) {
    val dotCount = 9
    var selected by remember { mutableStateOf(listOf<Int>()) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    val accent = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline

    fun dotCenter(index: Int): Offset {
        val col = index % 3
        val row = index / 3
        val cell = canvasSize.width / 3f
        return Offset(cell * col + cell / 2f, cell * row + cell / 2f)
    }

    fun nearestDot(pos: Offset): Int? {
        for (i in 0 until dotCount) {
            val c = dotCenter(i)
            if ((c - pos).getDistance() < canvasSize.width / 7f) return i
        }
        return null
    }

    Box(
        modifier = modifier
            .size(220.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selected = listOfNotNull(nearestDot(offset))
                        dragPosition = offset
                    },
                    onDrag = { change, _ ->
                        dragPosition = change.position
                        nearestDot(change.position)?.let { d ->
                            if (selected.lastOrNull() != d && !selected.contains(d)) {
                                selected = selected + d
                            }
                        }
                    },
                    onDragEnd = {
                        if (selected.size >= 4) onPatternComplete(selected)
                        selected = listOf()
                        dragPosition = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(220.dp).also { }) {
            canvasSize = size

            // линии между уже соединёнными точками
            if (selected.size > 1) {
                for (i in 0 until selected.size - 1) {
                    drawLine(
                        color = accent,
                        start = dotCenter(selected[i]),
                        end = dotCenter(selected[i + 1]),
                        strokeWidth = 5f
                    )
                }
            }
            dragPosition?.let { pos ->
                selected.lastOrNull()?.let { last ->
                    drawLine(color = accent.copy(alpha = 0.5f), start = dotCenter(last), end = pos, strokeWidth = 5f)
                }
            }

            for (i in 0 until dotCount) {
                val c = dotCenter(i)
                val isSelected = selected.contains(i)
                drawCircle(
                    color = if (isSelected) accent else dotColor,
                    radius = if (isSelected) 16f else 12f,
                    center = c
                )
                drawCircle(
                    color = outline,
                    radius = if (isSelected) 16f else 12f,
                    center = c,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}