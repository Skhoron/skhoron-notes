package dev.skhoron.notes.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skhoron.notes.security.ProtectionMethod
import dev.skhoron.notes.security.SecurityStore
import dev.skhoron.notes.ui.components.PatternPad
import dev.skhoron.notes.ui.components.PinDots
import dev.skhoron.notes.ui.components.PinPad

private enum class SetupStep { CHOOSE, ENTER_FIRST, ENTER_CONFIRM }

@Composable
fun ProtectionSetupScreen(
    securityStore: SecurityStore,
    onDone: () -> Unit,
    onSkip: () -> Unit
) {
    var method by remember { mutableStateOf<ProtectionMethod?>(null) }
    var step by remember { mutableStateOf(SetupStep.CHOOSE) }
    var firstValue by remember { mutableStateOf("") }
    var pinBuffer by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun reset() {
        method = null; step = SetupStep.CHOOSE; firstValue = ""; pinBuffer = ""; passwordValue = ""; error = null
    }

    fun completeWith(secret: String, chosenMethod: ProtectionMethod) {
        if (step == SetupStep.ENTER_FIRST) {
            firstValue = secret
            step = SetupStep.ENTER_CONFIRM
            pinBuffer = ""
        } else {
            if (secret == firstValue) {
                securityStore.setSecret(chosenMethod, secret)
                onDone()
            } else {
                error = "Не совпадает, попробуйте ещё раз"
                step = SetupStep.ENTER_FIRST
                firstValue = ""
                pinBuffer = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(32.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Защитите свои заметки",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Рекомендуем задать способ входа — это единственное, что стоит между вашими заметками и человеком с доступом к телефону",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))

        when (step) {
            SetupStep.CHOOSE -> {
                ProtectionOptionRow("PIN-код", "4 цифры") {
                    method = ProtectionMethod.PIN; step = SetupStep.ENTER_FIRST
                }
                Spacer(modifier = Modifier.height(10.dp))
                ProtectionOptionRow("Пароль", "Буквы, цифры, символы") {
                    method = ProtectionMethod.PASSWORD; step = SetupStep.ENTER_FIRST
                }
                Spacer(modifier = Modifier.height(10.dp))
                ProtectionOptionRow("Графический ключ", "Узор по точкам") {
                    method = ProtectionMethod.PATTERN; step = SetupStep.ENTER_FIRST
                }
            }

            SetupStep.ENTER_FIRST, SetupStep.ENTER_CONFIRM -> {
                Text(
                    if (step == SetupStep.ENTER_FIRST) "Придумайте способ входа" else "Повторите ещё раз",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                error?.let {
                    Text(it, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color(0xFFE05555))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                when (method) {
                    ProtectionMethod.PIN -> {
                        PinDots(length = pinBuffer.length)
                        Spacer(modifier = Modifier.height(20.dp))
                        PinPad(
                            onDigit = { d ->
                                if (pinBuffer.length < 4) pinBuffer += d
                                if (pinBuffer.length == 4) {
                                    completeWith(pinBuffer, ProtectionMethod.PIN)
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
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                if (passwordValue.length >= 4) {
                                    val v = passwordValue; passwordValue = ""
                                    completeWith(v, ProtectionMethod.PASSWORD)
                                } else error = "Минимум 4 символа"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Продолжить") }
                    }
                    ProtectionMethod.PATTERN -> {
                        PatternPad { sequence ->
                            completeWith(sequence.joinToString("-"), ProtectionMethod.PATTERN)
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "← Выбрать другой способ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { reset() }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Text(
            "Пропустить",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .clickable { onSkip() }
        )
    }
}

@Composable
private fun ProtectionOptionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}