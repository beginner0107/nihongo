package com.nihongo.conversation.core.export

import android.content.Context
import com.nihongo.conversation.domain.model.VocabularyEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnkiExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Export vocabulary entries to Anki-compatible CSV format
     *
     * Format: Japanese;Reading;Korean;Example
     * Compatible with Anki's CSV import feature
     */
    fun exportToAnkiDeck(vocabulary: List<VocabularyEntry>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "nihongo_vocabulary_$timestamp.csv"

        val csv = StringBuilder()

        // Header (optional, Anki can auto-detect fields)
        csv.appendLine("#separator:;")
        csv.appendLine("#html:false")
        csv.appendLine("#deck:Nihongo Vocabulary")
        csv.appendLine("#tags:nihongo-app")
        csv.appendLine()

        // Data rows
        vocabulary.forEach { entry ->
            val japanese = escapeCsvField(entry.word)
            val reading = escapeCsvField(entry.reading ?: "")
            val korean = escapeCsvField(entry.meaning)
            val example = escapeCsvField(entry.exampleSentence ?: "")

            csv.appendLine("$japanese;$reading;$korean;$example")
        }

        // Save to cache directory (will be shared via Intent)
        val file = File(context.cacheDir, fileName)
        file.writeText(csv.toString())

        return file
    }

    /**
     * Escape special characters in CSV fields
     */
    private fun escapeCsvField(field: String): String {
        return when {
            field.contains(";") || field.contains("\n") || field.contains("\"") -> {
                "\"${field.replace("\"", "\"\"")}\""
            }
            else -> field
        }
    }

    /**
     * Generate a preview of the export (first 5 entries)
     */
    fun generatePreview(vocabulary: List<VocabularyEntry>): String {
        val preview = vocabulary.take(5)
        val sb = StringBuilder()

        sb.appendLine("Preview (first ${preview.size} entries):")
        sb.appendLine()

        preview.forEachIndexed { index, entry ->
            sb.appendLine("${index + 1}. ${entry.word} (${entry.reading ?: "N/A"})")
            sb.appendLine("   Meaning: ${entry.meaning}")
            if (entry.exampleSentence != null) {
                sb.appendLine("   Example: ${entry.exampleSentence}")
            }
            sb.appendLine()
        }

        if (vocabulary.size > 5) {
            sb.appendLine("... and ${vocabulary.size - 5} more entries")
        }

        return sb.toString()
    }
}
