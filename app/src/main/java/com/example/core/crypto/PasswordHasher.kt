package com.example.core.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordHashResult(
    val hashHex: String,
    val saltHex: String,
    val iterations: Int = PasswordHasher.DEFAULT_ITERATIONS
) {
    /**
     * Formato versionado: v1$<iterations>$<saltHex>$<hashHex>
     */
    fun toStoredString(): String = "v1\$$iterations\$$saltHex\$$hashHex"
}

object PasswordHasher {
    // Guía OWASP Password Storage Cheat Sheet recomendada para PBKDF2-HMAC-SHA256: 600.000 iteraciones
    const val DEFAULT_ITERATIONS = 600000
    const val LEGACY_ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    private val secureRandom = SecureRandom()

    @Volatile
    var lastVerificationThread: Thread? = null

    @Volatile
    var lastHashingThread: Thread? = null

    private fun getSecretKeyFactory(): SecretKeyFactory {
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (_: Exception) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }
    }

    fun hashPassword(
        password: CharArray,
        salt: ByteArray? = null,
        iterations: Int = DEFAULT_ITERATIONS
    ): PasswordHashResult {
        lastHashingThread = Thread.currentThread()
        val actualSalt = salt ?: ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val spec = PBEKeySpec(password, actualSalt, iterations, KEY_LENGTH)
        val skf = getSecretKeyFactory()
        val hash = skf.generateSecret(spec).encoded
        return PasswordHashResult(
            hashHex = hash.joinToString("") { "%02x".format(it) },
            saltHex = actualSalt.joinToString("") { "%02x".format(it) },
            iterations = iterations
        )
    }

    data class ParsedHash(
        val iterations: Int,
        val saltHex: String,
        val hashHex: String,
        val isLegacy: Boolean
    )

    fun parseStoredPassword(storedString: String, legacySaltHex: String? = null): ParsedHash {
        // Formato versionado: v1$<iterations>$<saltHex>$<hashHex>
        if (storedString.startsWith("v1$")) {
            val parts = storedString.split("$")
            if (parts.size == 4) {
                val iters = parts[1].toIntOrNull() ?: DEFAULT_ITERATIONS
                val salt = parts[2]
                val hash = parts[3]
                return ParsedHash(
                    iterations = iters,
                    saltHex = salt,
                    hashHex = hash,
                    isLegacy = iters < DEFAULT_ITERATIONS
                )
            }
        }
        // Formato heredado: storedString es sólo el hashHex, y legacySaltHex es la sal
        return ParsedHash(
            iterations = LEGACY_ITERATIONS,
            saltHex = legacySaltHex ?: "",
            hashHex = storedString,
            isLegacy = true
        )
    }

    fun verifyPassword(
        password: CharArray,
        saltHex: String,
        expectedHashHex: String,
        iterations: Int = DEFAULT_ITERATIONS
    ): Boolean {
        lastVerificationThread = Thread.currentThread()
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val result = hashPassword(password, salt, iterations)
        val computedBytes = result.hashHex.lowercase().toByteArray(Charsets.UTF_8)
        val expectedBytes = expectedHashHex.lowercase().toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(computedBytes, expectedBytes)
    }

    fun isValidEmail(email: String): Boolean {
        val regex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.isNotBlank() && regex.matches(email.trim())
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }
}
