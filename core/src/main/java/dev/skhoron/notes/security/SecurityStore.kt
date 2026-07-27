package dev.skhoron.notes.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class ProtectionMethod { NONE, PIN, PASSWORD, PATTERN }

/**
 * Три способа защиты, сознательно без биометрии: биометрический сенсор — это доверие
 * стороннему API производителя устройства и потенциальная точка обхода/подмены,
 * что противоречит модели "всё под контролем пользователя, ничего не полагается на доверие
 * к чёрному ящику ОС/вендора".
 */
class SecurityStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "skhoron_security_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var method: ProtectionMethod
        get() = ProtectionMethod.valueOf(prefs.getString(KEY_METHOD, ProtectionMethod.NONE.name)!!)
        set(value) = prefs.edit().putString(KEY_METHOD, value.name).apply()

    var maxAttempts: Int
        get() = prefs.getInt(KEY_MAX_ATTEMPTS, 5)
        set(value) = prefs.edit().putInt(KEY_MAX_ATTEMPTS, value).apply()

    /** Длительность блокировки после исчерпания попыток, в минутах. */
    var lockoutMinutes: Int
        get() = prefs.getInt(KEY_LOCKOUT_MIN, 1)
        set(value) = prefs.edit().putInt(KEY_LOCKOUT_MIN, value).apply()

    var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, value).apply()

    /** Timestamp (millis) до которого приложение заблокировано, 0 = не заблокировано. */
    var lockedUntil: Long
        get() = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_LOCKED_UNTIL, value).apply()

    fun isLockedOut(): Boolean = lockedUntil > System.currentTimeMillis()

    fun setSecret(method: ProtectionMethod, secret: String) {
        val hashed = Hashing.hash(secret)
        prefs.edit()
            .putString(KEY_METHOD, method.name)
            .putString(KEY_HASH, hashed.hashBase64)
            .putString(KEY_SALT, hashed.saltBase64)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .apply()
    }

    fun clearSecret() {
        prefs.edit()
            .putString(KEY_METHOD, ProtectionMethod.NONE.name)
            .remove(KEY_HASH)
            .remove(KEY_SALT)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .apply()
    }

    /** @return true если секрет верный. При ошибке увеличивает счётчик и включает блокировку по достижении лимита. */
    fun verifyAndTrack(secret: String): Boolean {
        val hash = prefs.getString(KEY_HASH, null)
        val salt = prefs.getString(KEY_SALT, null)
        if (hash == null || salt == null) return false

        val ok = Hashing.verify(secret, HashedSecret(hash, salt))
        if (ok) {
            failedAttempts = 0
            lockedUntil = 0L
        } else {
            failedAttempts += 1
            if (failedAttempts >= maxAttempts) {
                lockedUntil = System.currentTimeMillis() + lockoutMinutes * 60_000L
                failedAttempts = 0
            }
        }
        return ok
    }

    fun isConfigured(): Boolean = method != ProtectionMethod.NONE && prefs.contains(KEY_HASH)

    companion object {
        private const val KEY_METHOD = "protection_method"
        private const val KEY_HASH = "secret_hash"
        private const val KEY_SALT = "secret_salt"
        private const val KEY_MAX_ATTEMPTS = "max_attempts"
        private const val KEY_LOCKOUT_MIN = "lockout_minutes"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKED_UNTIL = "locked_until"
    }
}