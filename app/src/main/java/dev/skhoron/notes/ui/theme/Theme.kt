package dev.skhoron.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SkhoronNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color = AccentDefault,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            background = BgDark,
            surface = SurfaceDark,
            surfaceVariant = Surface2Dark,
            onBackground = TextDark,
            onSurface = TextDark,
            outline = BorderDark,
        )
    } else {
        lightColorScheme(
            primary = accent,
            background = BgLight,
            surface = SurfaceLight,
            surfaceVariant = Surface2Light,
            onBackground = TextLight,
            onSurface = TextLight,
            outline = BorderLight,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SkhoronTypography,
        content = content
    )
}