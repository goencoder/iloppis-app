package se.iloppis.app.utils

import java.security.SecureRandom
import java.time.Instant

/** Generates 26-character, time-sortable ULIDs using Crockford Base32. */
object Ulid {
    // Crockford omits ambiguous characters I, L, O, and U.
    private const val ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private val random = SecureRandom()
    
    /** Returns a new ULID; entropy generation is thread-safe. */
    fun random(): String {
        val timestamp = System.currentTimeMillis()
        val timeChars = encodeTime(timestamp, 10)
        val randomChars = encodeRandom(16)
        return timeChars + randomChars
    }
    
    private fun encodeTime(time: Long, length: Int): String {
        var t = time
        val chars = CharArray(length)
        for (i in length - 1 downTo 0) {
            chars[i] = ENCODING[(t % 32).toInt()]
            t /= 32
        }
        return String(chars)
    }
    
    private fun encodeRandom(length: Int): String {
        val chars = CharArray(length)
        for (i in 0 until length) {
            chars[i] = ENCODING[random.nextInt(32)]
        }
        return String(chars)
    }
}
