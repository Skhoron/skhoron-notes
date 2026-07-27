package dev.skhoron.notes.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Хеширование секрета защиты (PIN / пароль / графический ключ как строка индексов "0-4-8-...").
 * PBKDF2WithHmacSHA256, 120_000 итераций, случайная соль 16 байт — так же, как обсуждали
 * для остальных Skhoron-продуктов (там, где нет полноценного Argon2id в стандартной JDK/Android API).
 */
object Hashing {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256

    fun hash(secret: String, salt: ByteArray = randomSalt()): HashedSecret {
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return HashedSecret(hashBase64 = hash.toBase64(), saltBase64 = salt.toBase64())
    }

    fun verify(secret: String, stored: HashedSecret): Boolean {
        val salt = stored.saltBase64.fromBase64()
        val candidate = hash(secret, salt)
        // Сравнение с постоянным временем, чтобы не давать утечку через тайминг-атаку
        return constantTimeEquals(candidate.hashBase64, stored.hashBase64)
    }

    private fun randomSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    private fun ByteArray.toBase64(): String = java.util.Base64.getEncoder().encodeToString(this)
    private fun String.fromBase64(): ByteArray = java.util.Base64.getDecoder().decode(this)
}

data class HashedSecret(val hashBase64: String, val saltBase64: String)