package dev.skhoron.notes.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.skhoron.notes.SkhoronNotesApp
import dev.skhoron.notes.security.ProtectionMethod
import dev.skhoron.notes.ui.editor.NoteEditorScreen
import dev.skhoron.notes.ui.lock.LockScreen
import dev.skhoron.notes.ui.notes.NotesListScreen
import dev.skhoron.notes.ui.onboarding.ProtectionSetupScreen
import dev.skhoron.notes.ui.onboarding.WelcomeScreen
import dev.skhoron.notes.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import java.io.File

private object Routes {
    const val WELCOME = "welcome"
    const val PROTECTION_SETUP = "protection_setup"
    const val LOCK = "lock"
    const val NOTES_LIST = "notes_list"
    const val EDITOR = "editor/{noteId}"
    const val SETTINGS = "settings"
    fun editor(noteId: String) = "editor/$noteId"
}

@Composable
fun SkhoronNavGraph(app: SkhoronNotesApp) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val settings by app.settingsStore.settingsFlow.collectAsStateWithLifecycle(initialValue = null)

    // Файл логотипа — единственный источник правды для AppLogo на всех экранах.
    var logoVersion by remember { mutableStateOf(0) } // форсируем перечитывание AsyncImage после замены файла
    val logoFile: File? = remember(settings?.customLogoPath, logoVersion) {
        settings?.customLogoPath?.let { File(app.filesDir, it) }
    }

    fun copyLogo(uri: Uri) {
        scope.launch {
            val dir = File(app.filesDir, "branding").apply { mkdirs() }
            val dest = File(dir, "logo.png")
            app.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            app.settingsStore.update { it.copy(customLogoPath = "branding/logo.png") }
            logoVersion++
        }
    }

    val startDestination = when {
        settings == null -> null // ждём первого значения из DataStore
        settings?.onboardingComplete != true -> Routes.WELCOME
        app.securityStore.isConfigured() -> Routes.LOCK
        else -> Routes.NOTES_LIST
    }

    if (startDestination == null) return // короткий кадр ожидания настроек

    val shouldShowLock by app.appLockController.shouldShowLock.collectAsStateWithLifecycle(initialValue = false)
    LaunchedEffect(shouldShowLock) {
        if (shouldShowLock) {
            val current = navController.currentBackStackEntry?.destination?.route
            // Не перебиваем сам экран блокировки, приветствие и настройку защиты —
            // там показывать поверх Lock бессмысленно или опасно (например, во время
            // первого ввода PIN на онбординге).
            if (current != Routes.LOCK && current != Routes.WELCOME && current != Routes.PROTECTION_SETUP) {
                navController.navigate(Routes.LOCK) { popUpTo(0) }
            }
            app.appLockController.consume()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                logoFile = logoFile,
                onNext = { navController.navigate(Routes.PROTECTION_SETUP) }
            )
        }

        composable(Routes.PROTECTION_SETUP) {
            ProtectionSetupScreen(
                securityStore = app.securityStore,
                onDone = {
                    scope.launch { app.settingsStore.update { it.copy(onboardingComplete = true) } }
                    navController.navigate(Routes.NOTES_LIST) { popUpTo(0) }
                },
                onSkip = {
                    app.securityStore.method = ProtectionMethod.NONE
                    scope.launch { app.settingsStore.update { it.copy(onboardingComplete = true) } }
                    navController.navigate(Routes.NOTES_LIST) { popUpTo(0) }
                }
            )
        }

        composable(Routes.LOCK) {
            LockScreen(
                securityStore = app.securityStore,
                logoFile = logoFile,
                onUnlocked = { navController.navigate(Routes.NOTES_LIST) { popUpTo(0) } }
            )
        }

        composable(Routes.NOTES_LIST) {
            NotesListScreen(
                repository = app.repository,
                onOpenNote = { id -> navController.navigate(Routes.editor(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenFolders = { /* TODO: экран папок — следующий шаг, список уже поддерживает вложенность в данных */ }
            )
        }

        composable(Routes.EDITOR) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
            NoteEditorScreen(
                noteId = noteId,
                repository = app.repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsStore = app.settingsStore,
                securityStore = app.securityStore,
                logoFile = logoFile,
                onCopyLogo = { uri -> copyLogo(uri) },
                onBack = { navController.popBackStack() },
                onReconfigureProtection = { navController.navigate(Routes.PROTECTION_SETUP) }
            )
        }
    }
}