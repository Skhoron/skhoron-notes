package dev.skhoron.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

/**
 * Логотип приложения. Если пользователь загрузил свой файл (Настройки → Логотип),
 * показывается он (через Coil — работает и с PNG, и с JPG; растровый SVG Android без
 * дополнительной конвертации не отрисует, см. пояснение в README про PNG vs SVG).
 * Если файла нет — показывается временный плейсхолдер.
 */
@Composable
fun AppLogo(logoFile: File?, size: Dp = 64.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .then(
                if (logoFile == null || !logoFile.exists())
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(size / 4))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (logoFile != null && logoFile.exists()) {
            AsyncImage(
                model = logoFile,
                contentDescription = "Логотип Skhoron Notes",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(RoundedCornerShape(size / 4))
            )
        } else {
            Text(
                text = "место\nдля\nлого",
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}