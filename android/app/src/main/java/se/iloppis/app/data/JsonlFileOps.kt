package se.iloppis.app.data

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Shared utilities for JSONL (JSON Lines) file operations.
 *
 * Eliminates duplicated read/write/init patterns across JSONL-based stores
 * (PendingItemsStore, PendingScansStore, CommittedScansStore).
 */
internal object JsonlFileOps {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Create (or return existing) event-scoped file.
     * Ensures the parent directory exists.
     */
    fun createEventFile(context: Context, eventId: String, filename: String): File {
        val eventDir = File(context.filesDir, "events/$eventId")
        eventDir.mkdirs()
        return File(eventDir, filename)
    }

    /**
     * Read and decode all non-blank JSONL lines from [file].
     * Malformed lines are silently skipped unless [onError] is provided.
     */
    inline fun <reified T> readAll(
        file: File,
        onError: (line: String, error: Exception) -> Unit = { _, _ -> }
    ): List<T> {
        if (!file.exists()) return emptyList()
        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                try {
                    json.decodeFromString<T>(line)
                } catch (e: Exception) {
                    onError(line, e)
                    null
                }
            }
    }

    /**
     * Append a single item as a JSONL line.
     */
    inline fun <reified T> appendOne(file: File, item: T) {
        file.appendText(json.encodeToString(item) + "\n")
    }

    /**
     * Append multiple items as JSONL lines.
     */
    inline fun <reified T> appendAll(file: File, items: List<T>) {
        if (items.isEmpty()) return
        file.appendText(items.joinToString("\n") { json.encodeToString(it) } + "\n")
    }

    /**
     * Rewrite the entire file with the given items.
     */
    inline fun <reified T> rewriteAll(file: File, items: List<T>) {
        file.writeText(
            items.joinToString("\n") { json.encodeToString(it) } +
                if (items.isNotEmpty()) "\n" else ""
        )
    }
}
