package se.iloppis.app.utils

import java.security.SecureRandom
import java.time.Instant

/**
 * Simple ULID (Universally Unique Lexicographically Sortable Identifier) generator.
 * 
 * Format: 26-character string (time-ordered + random)
 * - First 10 chars: Timestamp (milliseconds since epoch, base32)
 * - Last 16 chars: Random entropy (80 bits)
 * 
 * Character set: 0-9, A-Z excluding I, L, O, U (Crockford's Base32)
 * Pattern: ^[0-9A-HJKMNP-TV-Z]{26}$
 */
object Ulid {
    // Crockford's Base32 alphabet (excludes I, L, O, U to avoid confusion with 1, 1, 0, V)
    private const val ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private val random = SecureRandom()
    
    /**
     * Generate a new ULID.
     * Thread-safe via SecureRandom.
     */
    fun random(): String {
        val timestamp = System.currentTimeMillis()
        val timeChars = encodeTime(timestamp, 10)
        val randomChars = encodeRandom(16)
        return timeChars + randomChars
    }
    
    /**
     * Encode a timestamp (milliseconds) into base32 string.
     */
    private fun encodeTime(time: Long, length: Int): String {
        var t = time
        val chars = CharArray(length)
        for (i in length - 1 downTo 0) {
            chars[i] = ENCODING[(t % 32).toInt()]
            t /= 32
        }
        return String(chars)
    }
    
    /**
     * Generate random base32 string of specified length.
     */
    private fun encodeRandom(length: Int): String {
        val chars = CharArray(length)
        for (i in 0 until length) {
            chars[i] = ENCODING[random.nextInt(32)]
        }
        return String(chars)
    }
}
