package com.nihongo.conversation.data.repository

import com.nihongo.conversation.data.local.SentenceCardDao
import com.nihongo.conversation.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class SentenceCardRepository @Inject constructor(
    private val sentenceCardDao: SentenceCardDao
) {

    // ========== CARD MANAGEMENT ==========

    suspend fun createCard(card: SentenceCard): Long {
        return sentenceCardDao.insert(card)
    }

    suspend fun getCard(cardId: Long): SentenceCard? {
        return sentenceCardDao.getCardById(cardId)
    }

    fun getAllCards(userId: Long): Flow<List<SentenceCard>> {
        return sentenceCardDao.getAllCards(userId)
    }

    suspend fun getDueCards(userId: Long, limit: Int = 20): List<SentenceCard> {
        return sentenceCardDao.getDueCards(userId, System.currentTimeMillis(), limit)
    }

    suspend fun getNewCards(userId: Long, limit: Int = 10): List<SentenceCard> {
        return sentenceCardDao.getNewCards(userId, limit)
    }

    fun getCardsFromConversation(userId: Long, conversationId: Long): Flow<List<SentenceCard>> {
        return sentenceCardDao.getCardsFromConversation(userId, conversationId)
    }

    fun getCardsByPattern(userId: Long, pattern: String): Flow<List<SentenceCard>> {
        return sentenceCardDao.getCardsByPattern(userId, pattern)
    }

    suspend fun deleteCard(cardId: Long) {
        sentenceCardDao.deleteById(cardId)
    }

    // ========== SPACED REPETITION SYSTEM (SRS) ==========

    /**
     * Review a card and update its SRS data using SM-2 algorithm
     */
    suspend fun reviewCard(
        cardId: Long,
        difficulty: ReviewDifficulty,
        wasCorrect: Boolean
    ) {
        val card = sentenceCardDao.getCardById(cardId) ?: return

        // Calculate new SRS values
        val (newRepetitions, newEaseFactor, newInterval) = calculateSRS(
            repetitions = card.repetitions,
            easeFactor = card.easeFactor,
            interval = card.interval,
            difficulty = difficulty,
            wasCorrect = wasCorrect
        )

        // Calculate next review date
        val nextReviewDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(newInterval.toLong())

        // Update statistics
        val newCorrectCount = if (wasCorrect) card.correctCount + 1 else card.correctCount
        val newIncorrectCount = if (!wasCorrect) card.incorrectCount + 1 else card.incorrectCount

        sentenceCardDao.updateReviewData(
            cardId = cardId,
            repetitions = newRepetitions,
            easeFactor = newEaseFactor,
            interval = newInterval,
            nextReviewDate = nextReviewDate,
            correctCount = newCorrectCount,
            incorrectCount = newIncorrectCount,
            reviewedAt = System.currentTimeMillis()
        )
    }

    /**
     * SM-2 Algorithm for Spaced Repetition
     */
    private fun calculateSRS(
        repetitions: Int,
        easeFactor: Float,
        interval: Int,
        difficulty: ReviewDifficulty,
        wasCorrect: Boolean
    ): Triple<Int, Float, Int> {
        if (!wasCorrect || difficulty == ReviewDifficulty.AGAIN) {
            // Failed review - reset
            return Triple(0, max(1.3f, easeFactor - 0.2f), 1)
        }

        val quality = when (difficulty) {
            ReviewDifficulty.AGAIN -> 0
            ReviewDifficulty.HARD -> 2
            ReviewDifficulty.GOOD -> 3
            ReviewDifficulty.EASY -> 5
        }

        // Update ease factor
        val newEaseFactor = max(
            1.3f,
            easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        )

        val newRepetitions = repetitions + 1

        // Calculate new interval
        val newInterval = when (newRepetitions) {
            1 -> 1
            2 -> 6
            else -> (interval * newEaseFactor).toInt()
        }

        return Triple(newRepetitions, newEaseFactor, newInterval)
    }

    // ========== FILL-IN-THE-BLANK EXERCISE GENERATION ==========

    /**
     * Generate fill-in-the-blank exercise from a sentence card
     */
    fun generateFillInBlankExercise(card: SentenceCard, blankCount: Int = 1): FillInTheBlankExercise {
        val blanks = mutableListOf<Blank>()
        val sentence = card.sentence

        // Strategy 1: Blank particles if present
        val particles = listOf("は", "が", "を", "に", "で", "と", "から", "まで", "へ", "の")
        val particleBlanks = findParticles(sentence, particles)

        // Strategy 2: Blank verbs
        val verbBlanks = findVerbs(sentence)

        // Strategy 3: Blank pattern-specific words
        val patternBlanks = if (card.pattern != null) {
            findPatternWords(sentence, card.pattern)
        } else emptyList()

        // Combine and select blanks
        val allPotentialBlanks = particleBlanks + verbBlanks + patternBlanks
        val selectedBlanks = allPotentialBlanks
            .shuffled()
            .take(blankCount)
            .sortedBy { it.startIndex }

        if (selectedBlanks.isEmpty()) {
            // Fallback: blank a random word
            val words = sentence.split("").filter { it.isNotEmpty() }
            if (words.isNotEmpty()) {
                val randomIndex = words.indices.random()
                val word = words[randomIndex]
                blanks.add(
                    Blank(
                        position = randomIndex,
                        correctAnswer = word,
                        startIndex = randomIndex,
                        endIndex = randomIndex + 1,
                        blankType = BlankType.NOUN
                    )
                )
            }
        } else {
            blanks.addAll(selectedBlanks)
        }

        // Create blanked sentence
        val blankedSentence = createBlankedSentence(sentence, blanks)

        // Generate distractors
        val distractors = generateDistractors(blanks, card.pattern)

        // Generate hints
        val hints = blanks.map { blank ->
            when (blank.blankType) {
                BlankType.PARTICLE -> "助詞 (particle)"
                BlankType.VERB -> "動詞 (verb)"
                BlankType.ADJECTIVE -> "形容詞 (adjective)"
                BlankType.NOUN -> "名詞 (noun)"
                BlankType.PATTERN -> card.patternExplanation ?: "文法パターン"
                BlankType.ENTIRE_PHRASE -> "フレーズ (phrase)"
            }
        }

        return FillInTheBlankExercise(
            sentenceCardId = card.id,
            sentence = sentence,
            blankedSentence = blankedSentence,
            blanks = blanks,
            hints = hints,
            distractors = distractors
        )
    }

    private fun findParticles(sentence: String, particles: List<String>): List<Blank> {
        val blanks = mutableListOf<Blank>()
        var position = 0

        particles.forEach { particle ->
            var index = sentence.indexOf(particle)
            while (index != -1) {
                blanks.add(
                    Blank(
                        position = position++,
                        correctAnswer = particle,
                        startIndex = index,
                        endIndex = index + particle.length,
                        blankType = BlankType.PARTICLE,
                        hint = "助詞"
                    )
                )
                index = sentence.indexOf(particle, index + 1)
            }
        }

        return blanks
    }

    private fun findVerbs(sentence: String): List<Blank> {
        val blanks = mutableListOf<Blank>()

        // Common verb endings
        val verbEndings = listOf(
            "ます", "ました", "ません", "ませんでした",
            "る", "た", "ない", "なかった",
            "って", "て", "で"
        )

        // Find potential verbs (simplified)
        var position = 0
        verbEndings.forEach { ending ->
            var index = sentence.indexOf(ending)
            while (index != -1) {
                // Try to capture the verb stem (simplified)
                val start = max(0, index - 3)
                val verb = sentence.substring(start, index + ending.length)

                blanks.add(
                    Blank(
                        position = position++,
                        correctAnswer = verb,
                        startIndex = start,
                        endIndex = index + ending.length,
                        blankType = BlankType.VERB,
                        hint = "動詞"
                    )
                )

                index = sentence.indexOf(ending, index + 1)
            }
        }

        return blanks.distinctBy { it.startIndex }
    }

    private fun findPatternWords(sentence: String, pattern: String): List<Blank> {
        val blanks = mutableListOf<Blank>()

        // Extract the key part of the pattern
        val patternKey = pattern.replace("〜", "")

        val index = sentence.indexOf(patternKey)
        if (index != -1) {
            blanks.add(
                Blank(
                    position = 0,
                    correctAnswer = patternKey,
                    startIndex = index,
                    endIndex = index + patternKey.length,
                    blankType = BlankType.PATTERN,
                    hint = pattern
                )
            )
        }

        return blanks
    }

    private fun createBlankedSentence(sentence: String, blanks: List<Blank>): String {
        var blankedSentence = sentence

        // Replace blanks with underscores (in reverse order to maintain indices)
        blanks.sortedByDescending { it.startIndex }.forEach { blank ->
            val blankMarker = "_".repeat(blank.correctAnswer.length)
            blankedSentence = blankedSentence.substring(0, blank.startIndex) +
                             blankMarker +
                             blankedSentence.substring(blank.endIndex)
        }

        return blankedSentence
    }

    private fun generateDistractors(blanks: List<Blank>, pattern: String?): List<String> {
        val distractors = mutableListOf<String>()

        blanks.forEach { blank ->
            when (blank.blankType) {
                BlankType.PARTICLE -> {
                    distractors.addAll(listOf("は", "が", "を", "に", "で").filter { it != blank.correctAnswer })
                }
                BlankType.VERB -> {
                    distractors.addAll(listOf("行きます", "食べます", "見ます", "します").filter { it != blank.correctAnswer })
                }
                else -> {
                    // Add some generic distractors
                    distractors.add("...")
                }
            }
        }

        return distractors.take(4).distinct()
    }

    // ========== AUTO-CREATION FROM CONVERSATIONS ==========

    /**
     * Create sentence card from a conversation message
     */
    suspend fun createCardFromMessage(
        userId: Long,
        messageId: Long,
        conversationId: Long,
        sentence: String,
        translation: String,
        conversationContext: String? = null,
        scenarioTitle: String? = null,
        pattern: String? = null,
        patternExplanation: String? = null
    ): Long {
        val card = SentenceCard(
            userId = userId,
            messageId = messageId,
            conversationId = conversationId,
            sentence = sentence,
            translation = translation,
            conversationContext = conversationContext,
            scenarioTitle = scenarioTitle,
            pattern = pattern,
            patternExplanation = patternExplanation,
            difficulty = determineDifficulty(sentence, pattern),
            tags = extractTags(sentence, pattern)
        )

        return sentenceCardDao.insert(card)
    }

    /**
     * Extract grammar pattern from sentence
     */
    fun extractPattern(sentence: String): String? {
        val commonPatterns = mapOf(
            "てください" to "〜てください",
            "ことができます" to "〜ことができる",
            "ています" to "〜ている",
            "たいです" to "〜たい",
            "と思います" to "〜と思う",
            "ませんか" to "〜ませんか",
            "でしょう" to "〜でしょう",
            "なければなりません" to "〜なければならない",
            "たことがあります" to "〜たことがある",
            "そうです" to "〜そうだ"
        )

        commonPatterns.forEach { (key, pattern) ->
            if (sentence.contains(key)) {
                return pattern
            }
        }

        return null
    }

    private fun determineDifficulty(sentence: String, pattern: String?): CardDifficulty {
        // Simple heuristic based on length and pattern
        return when {
            pattern != null && pattern in listOf("〜なければならない", "〜たことがある") -> CardDifficulty.HARD
            sentence.length > 30 -> CardDifficulty.HARD
            sentence.length > 20 -> CardDifficulty.NORMAL
            else -> CardDifficulty.EASY
        }
    }

    private fun extractTags(sentence: String, pattern: String?): String {
        val tags = mutableListOf<String>()

        if (pattern != null) {
            tags.add(pattern)
        }

        // Add tags based on content
        when {
            sentence.contains("ください") -> tags.add("丁寧")
            sentence.contains("ます") || sentence.contains("です") -> tags.add("敬語")
            sentence.contains("?") || sentence.contains("か") -> tags.add("質問")
        }

        return tags.joinToString(",")
    }

    // ========== STATISTICS ==========

    suspend fun getStudyStats(userId: Long): SentenceStudyStats {
        val totalCards = sentenceCardDao.getTotalCardCount(userId)
        val dueCards = sentenceCardDao.getDueCardCount(userId, System.currentTimeMillis())
        val newCards = sentenceCardDao.getNewCardCount(userId)

        val startOfDay = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(
            java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY).toLong()
        )
        val endOfDay = startOfDay + TimeUnit.DAYS.toMillis(1)

        val reviewedToday = sentenceCardDao.getReviewedTodayCount(userId, startOfDay, endOfDay)
        val averageAccuracy = sentenceCardDao.getAverageAccuracy(userId) ?: 0f

        val difficultyCountsRaw = sentenceCardDao.getCardCountByDifficultyRaw(userId)
        val cardsByDifficulty = difficultyCountsRaw.associate { it.difficulty to it.count }

        val patternCounts = sentenceCardDao.getCardCountByPattern(userId)
        val cardsByPattern = patternCounts.associate { it.pattern to it.count }

        val strongestPatterns = sentenceCardDao.getStrongestPatterns(userId).map { it.pattern }
        val weakestPatterns = sentenceCardDao.getWeakestPatterns(userId).map { it.pattern }

        return SentenceStudyStats(
            totalCards = totalCards,
            dueCards = dueCards,
            newCards = newCards,
            reviewedToday = reviewedToday,
            averageAccuracy = averageAccuracy,
            totalStudyTime = 0, // TODO: Track study time
            currentStreak = 0,  // TODO: Calculate streak
            cardsByDifficulty = cardsByDifficulty,
            cardsByPattern = cardsByPattern,
            strongestPatterns = strongestPatterns,
            weakestPatterns = weakestPatterns
        )
    }
}
