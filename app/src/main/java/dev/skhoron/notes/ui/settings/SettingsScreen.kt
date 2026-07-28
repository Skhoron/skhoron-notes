package dev.skhoron.notes.ui.settings

import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.skhoron.notes.data.settings.AppSettingsStore
import dev.skhoron.notes.data.settings.CompressionAlgoSetting
import dev.skhoron.notes.data.settings.CompressionTrigger
import dev.skhoron.notes.data.settings.SaveFormat
import dev.skhoron.notes.security.ProtectionMethod
import dev.skhoron.notes.security.SecurityStore
import dev.skhoron.notes.ui.components.AppLogo
import dev.skhoron.notes.ui.theme.AccentPalette
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    settingsStore: AppSettingsStore,
    securityStore: SecurityStore,
    logoFile: File?,
    onCopyLogo: (Uri) -> Unit,
    onBack: () -> Unit,
    onReconfigureProtection: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return

    var maxAttemptsState by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(securityStore.maxAttempts) }
    var lockoutMinutesState by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(securityStore.lockoutMinutes) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onCopyLogo) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
                Text("Настройки", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            SettingsGroup("Логотип") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppLogo(logoFile = logoFile, size = 52.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            "Своя фотография вместо стандартного значка",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Загрузить фото",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { pickImage.launch(arrayOf("image/*")) }
                        )
                    }
                }
            }
        }

        item {
            SettingsGroup("Цвет акцента") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccentPalette.forEach { color ->
                        val hex = String.format("#%06X", 0xFFFFFF and color.toArgbInt())
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    scope.launch { settingsStore.update { it.copy(accentHex = hex) } }
                                }
                        )
                    }
                }
            }
        }

        item {
            SettingsGroup("Тема") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("dark" to "Тёмная", "light" to "Светлая").forEach { (value, label) ->
                        val selected = s.theme == value
                        Text(
                            label,
                            fontSize = 12.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { scope.launch { settingsStore.update { it.copy(theme = value) } } }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            SettingsGroup("Защита") {
                SettingsRow(
                    title = "Способ входа",
                    subtitle = when (securityStore.method) {
                        ProtectionMethod.NONE -> "Не задан"
                        ProtectionMethod.PIN -> "PIN-код"
                        ProtectionMethod.PASSWORD -> "Пароль"
                        ProtectionMethod.PATTERN -> "Графический ключ"
                    }
                ) {
                    Text(
                        "Изменить",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onReconfigureProtection() }
                    )
                }
                SettingsRow(title = "Попыток до блокировки", subtitle = "$maxAttemptsState") {
                    StepperInline(
                        value = maxAttemptsState,
                        options = listOf(3, 5, 10),
                        onChange = { securityStore.maxAttempts = it; maxAttemptsState = it }
                    )
                }
                SettingsRow(title = "Время блокировки", subtitle = "$lockoutMinutesState мин") {
                    StepperInline(
                        value = lockoutMinutesState,
                        options = listOf(1, 5, 30),
                        onChange = { securityStore.lockoutMinutes = it; lockoutMinutesState = it }
                    )
                }
            }
        }

        item {
            SettingsGroup("Формат сохранения заметок") {
                Column {
                    SaveFormat.values().forEach { fmt ->
                        val selected = s.saveFormat == fmt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { settingsStore.update { it.copy(saveFormat = fmt) } } }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioDot(selected)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(fmt.label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    if (s.saveFormat == SaveFormat.CUSTOM) {
                        androidx.compose.material3.OutlinedTextField(
                            value = s.customExtension,
                            onValueChange = { v -> scope.launch { settingsStore.update { it.copy(customExtension = v) } } },
                            label = { Text("Своё расширение, например hdhdhd") },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            SettingsGroup("Сжатие хранилища") {
                Text("Алгоритм", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    CompressionAlgoSetting.values().forEach { algo ->
                        val selected = s.compressionAlgo == algo
                        Text(
                            algo.name,
                            fontSize = 10.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { scope.launch { settingsStore.update { it.copy(compressionAlgo = algo) } } }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                Text("Уровень сжатия", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { level ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (level <= s.compressionLevel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { scope.launch { settingsStore.update { it.copy(compressionLevel = level) } } }
                        )
                    }
                }

                Text("Когда сжимать", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column {
                    CompressionTrigger.values().forEach { trig ->
                        val selected = s.compressionTrigger == trig
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { settingsStore.update { it.copy(compressionTrigger = trig) } } }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioDot(selected)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(triggerLabel(trig), fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }

        item {
            SettingsGroup("Дополнительно") {
                Text(
                    "Приложение не запрашивает доступ к сети — это решение на уровне сборки, а не переключатель в этом экране",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            title.uppercase(),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
    )
}

@Composable
private fun StepperInline(value: Int, options: List<Int>, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { opt ->
            val selected = opt == value
            Text(
                "$opt",
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onChange(opt) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private fun triggerLabel(t: CompressionTrigger): String = when (t) {
    CompressionTrigger.NEVER -> "Никогда"
    CompressionTrigger.IMMEDIATELY -> "Сразу после закрытия"
    CompressionTrigger.AFTER_24H -> "Через 24 часа"
    CompressionTrigger.AFTER_3D -> "Через 3 дня"
    CompressionTrigger.AFTER_7D -> "Через 7 дней"
    CompressionTrigger.AFTER_30D -> "Через 30 дней"
    CompressionTrigger.MANUAL_ONLY -> "Только вручную"
}

private fun Color.toArgbInt(): Int = this.toArgb()