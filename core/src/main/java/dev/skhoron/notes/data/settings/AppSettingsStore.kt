package dev.skhoron.notes.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "skhoron_notes_settings")

enum class SaveFormat(val extension: String, val label: String) {
    MARKDOWN("md", "Markdown (.md)"),
    PLAIN_TEXT("txt", "Обычный текст (.txt)"),
    JSON("json", "JSON (.json)"),
    YAML("yml", "YAML (.yml)"),
    CUSTOM("", "Своё расширение…")
}

enum class CompressionAlgoSetting { NONE, ZSTD, BROTLI, LZMA2, GZIP, LZ4 }
enum class CompressionTrigger { NEVER, IMMEDIATELY, AFTER_24H, AFTER_3D, AFTER_7D, AFTER_30D, MANUAL_ONLY }
enum class CompressionScope { TEXT_ONLY, TEXT_AND_ATTACHMENTS, ATTACHMENTS_ONLY, WHOLE_STORAGE }
enum class SortMode { UPDATED, CREATED, TITLE, MANUAL }
enum class StorageLocation { APP_PRIVATE, USER_CHOSEN_FOLDER }

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val theme: String = "dark",
    val accentHex: String = "#16C172",
    val fontSizeSp: Int = 14,
    val animationsEnabled: Boolean = true,
    val sortMode: SortMode = SortMode.UPDATED,

    // Формат сохранения по умолчанию — пользователь может выбрать пресет или задать
    // полностью произвольное расширение (customExtension), например "hdhdhd".
    val saveFormat: SaveFormat = SaveFormat.MARKDOWN,
    val customExtension: String = "",

    // Где физически лежат файлы заметок: приватная песочница приложения (по умолчанию,
    // не требует разрешений) или папка, выбранная пользователем через SAF (ACTION_OPEN_DOCUMENT_TREE).
    val storageLocation: StorageLocation = StorageLocation.APP_PRIVATE,
    val userStorageUri: String? = null,

    // Путь (относительно filesDir) к логотипу, который пользователь загрузил своим фото/PNG/SVG.
    // null = показывается встроенный плейсхолдер-заглушка.
    val customLogoPath: String? = null,

    // Настройки автосжатия
    val compressionAlgo: CompressionAlgoSetting = CompressionAlgoSetting.ZSTD,
    val compressionLevel: Int = 3, // 1..5
    val compressionTrigger: CompressionTrigger = CompressionTrigger.AFTER_24H,
    val compressionScope: CompressionScope = CompressionScope.TEXT_ONLY
) {
    /** Итоговое расширение файла с учётом "своего" варианта, без ведущей точки. */
    fun resolvedExtension(): String =
        if (saveFormat == SaveFormat.CUSTOM) customExtension.trim().ifBlank { "md" } else saveFormat.extension
}

class AppSettingsStore(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            onboardingComplete = p[Keys.ONBOARDING] ?: false,
            theme = p[Keys.THEME] ?: "dark",
            accentHex = p[Keys.ACCENT] ?: "#16C172",
            fontSizeSp = p[Keys.FONT_SIZE] ?: 14,
            animationsEnabled = p[Keys.ANIM] ?: true,
            sortMode = runCatching { SortMode.valueOf(p[Keys.SORT] ?: "") }.getOrDefault(SortMode.UPDATED),
            saveFormat = runCatching { SaveFormat.valueOf(p[Keys.SAVE_FORMAT] ?: "") }.getOrDefault(SaveFormat.MARKDOWN),
            customExtension = p[Keys.CUSTOM_EXT] ?: "",
            storageLocation = runCatching { StorageLocation.valueOf(p[Keys.STORAGE_LOC] ?: "") }.getOrDefault(StorageLocation.APP_PRIVATE),
            userStorageUri = p[Keys.STORAGE_URI],
            customLogoPath = p[Keys.CUSTOM_LOGO_PATH],
            compressionAlgo = runCatching { CompressionAlgoSetting.valueOf(p[Keys.COMPRESSION_ALGO] ?: "") }.getOrDefault(CompressionAlgoSetting.ZSTD),
            compressionLevel = p[Keys.COMPRESSION_LEVEL] ?: 3,
            compressionTrigger = runCatching { CompressionTrigger.valueOf(p[Keys.COMPRESSION_TRIGGER] ?: "") }.getOrDefault(CompressionTrigger.AFTER_24H),
            compressionScope = runCatching { CompressionScope.valueOf(p[Keys.COMPRESSION_SCOPE] ?: "") }.getOrDefault(CompressionScope.TEXT_ONLY)
        )
    }

    /** Атомарно читает текущие настройки и записывает результат transform(). */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val existing = AppSettings(
                onboardingComplete = p[Keys.ONBOARDING] ?: false,
                theme = p[Keys.THEME] ?: "dark",
                accentHex = p[Keys.ACCENT] ?: "#16C172",
                fontSizeSp = p[Keys.FONT_SIZE] ?: 14,
                animationsEnabled = p[Keys.ANIM] ?: true,
                sortMode = runCatching { SortMode.valueOf(p[Keys.SORT] ?: "") }.getOrDefault(SortMode.UPDATED),
                saveFormat = runCatching { SaveFormat.valueOf(p[Keys.SAVE_FORMAT] ?: "") }.getOrDefault(SaveFormat.MARKDOWN),
                customExtension = p[Keys.CUSTOM_EXT] ?: "",
                storageLocation = runCatching { StorageLocation.valueOf(p[Keys.STORAGE_LOC] ?: "") }.getOrDefault(StorageLocation.APP_PRIVATE),
                userStorageUri = p[Keys.STORAGE_URI],
                customLogoPath = p[Keys.CUSTOM_LOGO_PATH],
                compressionAlgo = runCatching { CompressionAlgoSetting.valueOf(p[Keys.COMPRESSION_ALGO] ?: "") }.getOrDefault(CompressionAlgoSetting.ZSTD),
                compressionLevel = p[Keys.COMPRESSION_LEVEL] ?: 3,
                compressionTrigger = runCatching { CompressionTrigger.valueOf(p[Keys.COMPRESSION_TRIGGER] ?: "") }.getOrDefault(CompressionTrigger.AFTER_24H),
                compressionScope = runCatching { CompressionScope.valueOf(p[Keys.COMPRESSION_SCOPE] ?: "") }.getOrDefault(CompressionScope.TEXT_ONLY)
            )
            val updated = transform(existing)
            p[Keys.ONBOARDING] = updated.onboardingComplete
            p[Keys.THEME] = updated.theme
            p[Keys.ACCENT] = updated.accentHex
            p[Keys.FONT_SIZE] = updated.fontSizeSp
            p[Keys.ANIM] = updated.animationsEnabled
            p[Keys.SORT] = updated.sortMode.name
            p[Keys.SAVE_FORMAT] = updated.saveFormat.name
            p[Keys.CUSTOM_EXT] = updated.customExtension
            p[Keys.STORAGE_LOC] = updated.storageLocation.name
            updated.userStorageUri?.let { p[Keys.STORAGE_URI] = it }
            updated.customLogoPath?.let { p[Keys.CUSTOM_LOGO_PATH] = it } ?: p.remove(Keys.CUSTOM_LOGO_PATH)
            p[Keys.COMPRESSION_ALGO] = updated.compressionAlgo.name
            p[Keys.COMPRESSION_LEVEL] = updated.compressionLevel
            p[Keys.COMPRESSION_TRIGGER] = updated.compressionTrigger.name
            p[Keys.COMPRESSION_SCOPE] = updated.compressionScope.name
        }
    }

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val THEME = stringPreferencesKey("theme")
        val ACCENT = stringPreferencesKey("accent_hex")
        val FONT_SIZE = intPreferencesKey("font_size_sp")
        val ANIM = booleanPreferencesKey("animations_enabled")
        val SORT = stringPreferencesKey("sort_mode")
        val SAVE_FORMAT = stringPreferencesKey("save_format")
        val CUSTOM_EXT = stringPreferencesKey("custom_extension")
        val STORAGE_LOC = stringPreferencesKey("storage_location")
        val STORAGE_URI = stringPreferencesKey("storage_uri")
        val CUSTOM_LOGO_PATH = stringPreferencesKey("custom_logo_path")
        val COMPRESSION_ALGO = stringPreferencesKey("compression_algo")
        val COMPRESSION_LEVEL = intPreferencesKey("compression_level")
        val COMPRESSION_TRIGGER = stringPreferencesKey("compression_trigger")
        val COMPRESSION_SCOPE = stringPreferencesKey("compression_scope")
    }
}