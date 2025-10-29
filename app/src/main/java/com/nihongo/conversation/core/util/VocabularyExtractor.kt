package com.nihongo.conversation.core.util

import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.domain.model.VocabularyEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts important vocabulary words from Japanese conversation messages
 */
@Singleton
class VocabularyExtractor @Inject constructor() {

    /**
     * Extract important vocabulary from a list of messages
     * Focuses on AI messages (not user messages)
     */
    fun extractFromMessages(
        messages: List<Message>,
        userId: Long,
        conversationId: Long
    ): List<VocabularyEntry> {
        val aiMessages = messages.filter { !it.isUser && it.content.isNotBlank() }
        val extractedWords = mutableSetOf<String>()
        val vocabularyList = mutableListOf<VocabularyEntry>()

        for (message in aiMessages) {
            val words = extractWordsFromText(message.content)
            for (word in words) {
                // Avoid duplicates
                if (word !in extractedWords && isImportantWord(word)) {
                    extractedWords.add(word)

                    vocabularyList.add(
                        VocabularyEntry(
                            userId = userId,
                            word = word,
                            meaning = "[翻訳が必要]", // Will be translated later
                            exampleSentence = message.content,
                            sourceConversationId = conversationId,
                            difficulty = estimateDifficulty(word)
                        )
                    )
                }
            }
        }

        return vocabularyList
    }

    /**
     * Extract individual words/phrases from Japanese text
     * This is a simplified version - in production, you'd use a proper tokenizer like Kuromoji
     */
    private fun extractWordsFromText(text: String): List<String> {
        // Remove punctuation and split
        val cleaned = text
            .replace(Regex("[。、！？!?,.\\s]+"), " ")
            .trim()

        // Simple splitting by particles and common patterns
        // In production, use MeCab or Kuromoji for proper tokenization
        val words = mutableListOf<String>()
        var currentWord = StringBuilder()

        for (char in cleaned) {
            when {
                // Japanese particles (simple detection)
                char in listOf('は', 'が', 'を', 'に', 'で', 'と', 'の', 'も', 'や', 'か') -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord = StringBuilder()
                    }
                }
                // Space
                char == ' ' -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord = StringBuilder()
                    }
                }
                else -> {
                    currentWord.append(char)
                }
            }
        }

        // Add last word
        if (currentWord.isNotEmpty()) {
            words.add(currentWord.toString())
        }

        return words.filter { it.length >= 2 } // Filter very short words
    }

    /**
     * Check if a word is important enough to be added to vocabulary
     */
    private fun isImportantWord(word: String): Boolean {
        // Filter out very common words that don't need flashcards
        val commonWords = setOf(
            "です", "ます", "ました", "ません", "でした",
            "こと", "もの", "これ", "それ", "あれ",
            "する", "なる", "いる", "ある", "ない",
            "そう", "こう", "ああ", "どう"
        )

        return when {
            word.length < 2 -> false // Too short
            word in commonWords -> false // Too common
            word.all { it in 'ぁ'..'ん' } && word.length < 3 -> false // Short hiragana only
            else -> true
        }
    }

    /**
     * Estimate difficulty level based on word characteristics
     * 1 = Beginner, 2 = Intermediate, 3 = Advanced, 4 = Expert, 5 = Native
     */
    private fun estimateDifficulty(word: String): Int {
        return when {
            // Contains kanji -> likely more advanced
            word.any { it in '\u4E00'..'\u9FFF' } -> {
                val kanjiCount = word.count { it in '\u4E00'..'\u9FFF' }
                when {
                    kanjiCount >= 3 -> 4 // Multiple kanji = advanced
                    kanjiCount == 2 -> 3
                    else -> 2
                }
            }
            // Katakana words (foreign loan words)
            word.any { it in '\u30A0'..'\u30FF' } -> 2
            // Long hiragana words
            word.length >= 5 -> 2
            // Default
            else -> 1
        }
    }

    /**
     * Extract vocabulary from a single phrase/sentence
     * Useful for practicing specific expressions
     */
    fun extractFromPhrase(
        phrase: String,
        userId: Long,
        meaning: String = "[翻訳が必要]"
    ): VocabularyEntry {
        return VocabularyEntry(
            userId = userId,
            word = phrase.trim(),
            meaning = meaning,
            exampleSentence = phrase,
            difficulty = estimateDifficulty(phrase)
        )
    }
}
