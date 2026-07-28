package dev.skhoron.notes.ui.lock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skhoron.notes.security.ProtectionMethod
import dev.skhoron.notes.security.SecurityStore
import dev.skhoron.notes.ui.components.AppLogo
import dev.skhoron.notes.ui.components.PatternPad
import dev.skhoron.notes.ui.components.PinDots
import dev.skhoron.notes.ui.components.PinPad
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun LockScreen(securityStore: SecurityStore, logoFile: File?, onUnlocked: () -> Unit) {
    var pinBuffer by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var secondsLeft by remember { mutableStateOf(0) }

    fun refreshLockCountdown() {
        val remainMs = securityStore.lockedUntil - System.currentTimeMillis()
        secondsLeft = if (remainMs > 0) (remainMs / 1000).toInt() + 1 else 0
    }

    LaunchedEffect(Unit) { refreshLockCountdown() }
    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000)
            refreshLockCountdown()
        }
    }

    fun attempt(secret: String) {
        if (securityStore.isLockedOut()) return
        val ok = securityStore.verifyAndTrack(secret)
        if (ok) {
            onUnlocked()
        } else {
            pinBuffer = ""
            passwordValue = ""
            refreshLockCountdown()
            error = if (securityStore.isLockedOut()) {
                "Слишком много попыток. Подождите ${secondsLeft} с"
            } else {
                val left = securityStore.maxAttempts - securityStore.failedAttempts
                "Неверно. Осталось попыток: $left"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        AppLogo(logoFile = logoFile, size = 64.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Skhoron Notes",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            when (securityStore.method) {
                ProtectionMethod.PIN -> "Введите PIN-код"
                ProtectionMethod.PASSWORD -> "Введите пароль"
                ProtectionMethod.PATTERN -> "Введите графический ключ"
                ProtectionMethod.NONE -> ""
            },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        error?.let {
            Text(it, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color(0xFFE05555))
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (secondsLeft <= 0) {
            when (securityStore.method) {
                ProtectionMethod.PIN -> {
                    PinDots(length = pinBuffer.length)
                    Spacer(modifier = Modifier.height(20.dp))
                    PinPad(
                        onDigit = { d ->
                            if (pinBuffer.length < 4) pinBuffer += d
                            if (pinBuffer.length == 4) {
                                val v = pinBuffer
                                attempt(v)
                            }
                        },
                        onBackspace = { pinBuffer = pinBuffer.dropLast(1) }
                    )
                }
                ProtectionMethod.PASSWORD -> {
                    OutlinedTextField(
                        value = passwordValue,
                        onValueChange = { passwordValue = it },
                        label = { Text("Пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.Button(
                        onClick = { attempt(passwordValue) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Войти") }
                }
                ProtectionMethod.PATTERN -> {
                    PatternPad { sequence -> attempt(sequence.joinToString("-")) }
                }
                ProtectionMethod.NONE -> {
                    // Раньше onUnlocked() вызывался прямо в теле @Composable — побочный эффект
                    // без LaunchedEffect мог сработать повторно при каждой рекомпозиции этого блока.
                    LaunchedEffect(Unit) { onUnlocked() }
                }
            }
        }
    }
}