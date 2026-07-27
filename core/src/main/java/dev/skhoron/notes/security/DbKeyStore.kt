package dev.skhoron.notes.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Ключ шифрования самой базы данных (SQLCipher passphrase).
 *
 * Сознательно НЕ привязан к PIN/паролю/графическому ключу пользователя: если завязать
 * passphrase на PIN, то а) пользователь, пропустивший защиту при онбординге, останется
 * с незашифрованной БД, б) смена/сброс PIN потребовала бы полного перешифрования базы.
 * Вместо этого генерируется случайный 256-битный ключ при первом запуске, который сам
 * хранится зашифрованным через Android Keystore (EncryptedSharedPreferences) — то есть
 * БД зашифрована всегда, вне зависимости от того, включена ли защита входа в приложение.
 */
class DbKeyStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "skhoron_db_key_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Возвращает passphrase для SQLCipher, создавая его при первом обращении. */
    fun getOrCreatePassphrase(): ByteArray {
        val existing = prefs.getString(KEY_PASSPHRASE, null)
        if (existing != null) return existing.decodeHex()

        val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_PASSPHRASE, random.toHex()).apply()
        return random
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.decodeHex(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        private const val KEY_PASSPHRASE = "db_passphrase_hex"
    }
}