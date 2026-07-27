package dev.skhoron.notes.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashingTest {

    @Test
    fun `verify accepts the correct secret`() {
        val hashed = Hashing.hash("1234")
        assertTrue(Hashing.verify("1234", hashed))
    }

    @Test
    fun `verify rejects a wrong secret`() {
        val hashed = Hashing.hash("1234")
        assertFalse(Hashing.verify("0000", hashed))
    }

    @Test
    fun `verify rejects an empty secret when a real one was set`() {
        val hashed = Hashing.hash("mypassword")
        assertFalse(Hashing.verify("", hashed))
    }

    @Test
    fun `same secret produces different hash and salt each time`() {
        // Соль случайная — даже одинаковый PIN не должен давать идентичный hash+salt,
        // иначе два пользователя с одним и тем же PIN были бы отличимы по слепку.
        val first = Hashing.hash("1234")
        val second = Hashing.hash("1234")
        assertNotEquals(first.saltBase64, second.saltBase64)
        assertNotEquals(first.hashBase64, second.hashBase64)
    }

    @Test
    fun `pattern-style secret (dash-joined indices) hashes and verifies correctly`() {
        // Графический ключ сериализуется как "0-4-8-6-2" перед хешированием — проверяем,
        // что это обычная строка и никакого специального пути обработки не требует.
        val patternSecret = listOf(0, 4, 8, 6, 2).joinToString("-")
        val hashed = Hashing.hash(patternSecret)
        assertTrue(Hashing.verify(patternSecret, hashed))
        assertFalse(Hashing.verify("0-4-8-6-3", hashed))
    }
}