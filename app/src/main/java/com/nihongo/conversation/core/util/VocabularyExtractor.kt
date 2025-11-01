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
     * Improved tokenization using character type boundaries and particle detection
     */
    private fun extractWordsFromText(text: String): List<String> {
        // Remove punctuation but preserve sentence structure
        val cleaned = text
            .replace(Regex("[。、！？!?,.\\s]+"), "|")
            .trim()

        val words = mutableListOf<String>()
        var currentWord = StringBuilder()
        var previousCharType: CharType? = null

        for (char in cleaned) {
            val currentCharType = getCharType(char)

            when {
                // Sentence boundary marker
                char == '|' -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord = StringBuilder()
                    }
                    previousCharType = null
                }
                // Particles that mark word boundaries
                isParticle(char) -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord = StringBuilder()
                    }
                    // Include particle with following content for some cases
                    previousCharType = currentCharType
                }
                // Auxiliary verbs that should be separated
                isAuxiliaryVerbStart(currentWord.toString(), char) -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord = StringBuilder()
                    }
                    currentWord.append(char)
                    previousCharType = currentCharType
                }
                // Character type boundary (e.g., kanji -> hiragana)
                shouldSplitOnCharTypeChange(previousCharType, currentCharType, char) -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord = StringBuilder()
                    }
                    currentWord.append(char)
                    previousCharType = currentCharType
                }
                else -> {
                    currentWord.append(char)
                    previousCharType = currentCharType
                }
            }
        }

        // Add last word
        if (currentWord.isNotEmpty()) {
            words.add(currentWord.toString())
        }

        // Filter and clean results
        return words
            .filter { it.length >= 2 }
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Character type classification for tokenization
     */
    private enum class CharType {
        KANJI, HIRAGANA, KATAKANA, ALPHABET, NUMBER, OTHER
    }

    /**
     * Determine the type of a character
     */
    private fun getCharType(char: Char): CharType {
        return when (char) {
            in '\u4E00'..'\u9FFF' -> CharType.KANJI
            in 'ぁ'..'ん' -> CharType.HIRAGANA
            in 'ァ'..'ン' -> CharType.KATAKANA
            in 'a'..'z', in 'A'..'Z' -> CharType.ALPHABET
            in '0'..'9' -> CharType.NUMBER
            else -> CharType.OTHER
        }
    }

    /**
     * Check if character is a Japanese particle
     */
    private fun isParticle(char: Char): Boolean {
        // Common single-character particles that mark word boundaries
        return char in listOf('は', 'が', 'を', 'に', 'で', 'と', 'の', 'も', 'や', 'へ', 'か')
    }

    /**
     * Check if we should split on character type change
     * Only split when transitioning from Kanji to Hiragana (verb/adjective stems)
     */
    private fun shouldSplitOnCharTypeChange(
        previousType: CharType?,
        currentType: CharType,
        currentChar: Char
    ): Boolean {
        if (previousType == null) return false

        return when {
            // Kanji compound -> Hiragana (likely verb/adjective ending)
            previousType == CharType.KANJI && currentType == CharType.HIRAGANA -> {
                // Don't split if it's a verb ending we want to keep
                !currentChar.toString().matches(Regex("[うくぐすつぬむるぶ]")) // Common verb endings to keep
            }
            // Hiragana -> Kanji (new word starting)
            previousType == CharType.HIRAGANA && currentType == CharType.KANJI -> true
            // Katakana -> non-Katakana
            previousType == CharType.KATAKANA && currentType != CharType.KATAKANA -> true
            // non-Katakana -> Katakana
            previousType != CharType.KATAKANA && currentType == CharType.KATAKANA -> true
            else -> false
        }
    }

    /**
     * Check if this is the start of an auxiliary verb that should be split
     */
    private fun isAuxiliaryVerbStart(currentWord: String, nextChar: Char): Boolean {
        // Common auxiliary verbs: ている、てある、ておく、てくる、ていく、てしまう
        if (currentWord.endsWith("て") || currentWord.endsWith("で")) {
            return nextChar in listOf('い', 'お', 'く', 'し')
        }
        return false
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
