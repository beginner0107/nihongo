package com.nihongo.conversation.domain.analyzer

import com.nihongo.conversation.domain.model.*
import kotlin.math.*

/**
 * Analyzes speaking speed and rhythm patterns
 * Compares with native Japanese speakers
 */
class SpeedRhythmAnalyzer {

    companion object {
        // Native Japanese mora timing (milliseconds)
        private const val NATIVE_MORA_DURATION_AVG = 120 // Average
        private const val NATIVE_MORA_DURATION_MIN = 80
        private const val NATIVE_MORA_DURATION_MAX = 180

        // Speed thresholds
        private const val VERY_SLOW_THRESHOLD = 0.6f // 60% of native speed
        private const val SLOW_THRESHOLD = 0.8f // 80% of native speed
        private const val FAST_THRESHOLD = 1.2f // 120% of native speed
        private const val VERY_FAST_THRESHOLD = 1.5f // 150% of native speed

        // Rhythm consistency threshold
        private const val GOOD_RHYTHM_CV = 0.3f // Coefficient of variation
        private const val FAIR_RHYTHM_CV = 0.5f
    }

    /**
     * Analyze speed and rhythm from mora data
     */
    fun analyze(
        morae: List<Mora>,
        nativeRecording: FloatArray? = null,
        nativeMorae: List<Mora>? = null
    ): RhythmAnalysis {
        // Calculate total duration
        val totalDuration = morae.sumOf { it.duration }

        // Extract mora durations
        val moraDurations = morae.map { it.duration }

        // Calculate average mora duration
        val averageMoraDuration = if (moraDurations.isNotEmpty()) {
            moraDurations.average().toInt()
        } else 0

        // Classify speaking speed
        val speedRating = classifySpeed(averageMoraDuration, NATIVE_MORA_DURATION_AVG)

        // Calculate rhythm score (consistency)
        val rhythmScore = calculateRhythmScore(moraDurations)

        // Find pause locations
        val pauseLocations = findPauses(morae)

        // Calculate naturalness
        val naturalness = calculateNaturalness(
            averageMoraDuration,
            rhythmScore,
            pauseLocations.size,
            morae.size
        )

        // Compare with native if available
        val comparison = if (nativeMorae != null) {
            compareWithNative(morae, nativeMorae)
        } else null

        return RhythmAnalysis(
            totalDuration = totalDuration,
            moraDurations = moraDurations,
            averageMoraDuration = averageMoraDuration,
            speedRating = speedRating,
            rhythmScore = rhythmScore,
            pauseLocations = pauseLocations,
            naturalness = naturalness,
            comparison = comparison
        )
    }

    /**
     * Classify speaking speed based on mora duration
     */
    private fun classifySpeed(userMoraDuration: Int, nativeMoraDuration: Int): SpeedRating {
        val speedRatio = userMoraDuration.toFloat() / nativeMoraDuration

        return when {
            speedRatio < VERY_SLOW_THRESHOLD -> SpeedRating.TOO_SLOW
            speedRatio < SLOW_THRESHOLD -> SpeedRating.SLOW
            speedRatio > VERY_FAST_THRESHOLD -> SpeedRating.TOO_FAST
            speedRatio > FAST_THRESHOLD -> SpeedRating.FAST
            else -> SpeedRating.NATURAL
        }
    }

    /**
     * Calculate rhythm consistency score (0-100)
     * Higher score = more consistent timing
     */
    private fun calculateRhythmScore(moraDurations: List<Int>): Float {
        if (moraDurations.size < 2) return 100f

        // Calculate coefficient of variation (CV)
        val mean = moraDurations.average().toFloat()
        val variance = moraDurations.map { (it - mean).pow(2) }.average().toFloat()
        val stdDev = sqrt(variance)
        val cv = stdDev / mean

        // Convert to 0-100 score
        return when {
            cv < GOOD_RHYTHM_CV -> 100f
            cv < FAIR_RHYTHM_CV -> 100f - ((cv - GOOD_RHYTHM_CV) / (FAIR_RHYTHM_CV - GOOD_RHYTHM_CV) * 30f)
            else -> maxOf(0f, 70f - ((cv - FAIR_RHYTHM_CV) * 50f))
        }
    }

    /**
     * Find pause locations (long gaps between morae)
     */
    private fun findPauses(morae: List<Mora>): List<Int> {
        val pauses = mutableListOf<Int>()

        for (i in 0 until morae.size - 1) {
            val currentEnd = morae[i].startTime + morae[i].duration
            val nextStart = morae[i + 1].startTime
            val gap = nextStart - currentEnd

            // Gap > 200ms is considered a pause
            if (gap > 200) {
                pauses.add(currentEnd)
            }
        }

        return pauses
    }

    /**
     * Calculate overall naturalness (0-100)
     */
    private fun calculateNaturalness(
        avgMoraDuration: Int,
        rhythmScore: Float,
        pauseCount: Int,
        moraCount: Int
    ): Float {
        // Speed naturalness (how close to native)
        val speedNaturalness = calculateSpeedNaturalness(avgMoraDuration)

        // Rhythm naturalness (consistency)
        val rhythmNaturalness = rhythmScore

        // Pause naturalness (not too many, not too few)
        val expectedPauses = moraCount / 8 // Rough estimate
        val pauseDiff = abs(pauseCount - expectedPauses)
        val pauseNaturalness = maxOf(0f, 100f - (pauseDiff * 10f))

        // Weighted average
        return (speedNaturalness * 0.4f +
                rhythmNaturalness * 0.4f +
                pauseNaturalness * 0.2f)
    }

    /**
     * Calculate speed naturalness score
     */
    private fun calculateSpeedNaturalness(avgMoraDuration: Int): Float {
        val diff = abs(avgMoraDuration - NATIVE_MORA_DURATION_AVG)
        val tolerance = 30 // ms tolerance for "natural"

        return when {
            diff < tolerance -> 100f
            diff < tolerance * 2 -> 100f - ((diff - tolerance) / tolerance * 30f)
            else -> maxOf(0f, 70f - ((diff - tolerance * 2) / 10f))
        }
    }

    /**
     * Compare user's speech with native speaker
     */
    private fun compareWithNative(
        userMorae: List<Mora>,
        nativeMorae: List<Mora>
    ): NativeComparison {
        val userDuration = userMorae.sumOf { it.duration }
        val nativeDuration = nativeMorae.sumOf { it.duration }

        val speedRatio = userDuration.toFloat() / nativeDuration

        // Calculate rhythm similarity using Dynamic Time Warping (DTW)
        val rhythmSimilarity = calculateRhythmSimilarity(
            userMorae.map { it.duration },
            nativeMorae.map { it.duration }
        )

        // Calculate pitch pattern similarity
        val pitchSimilarity = calculatePitchSimilarity(userMorae, nativeMorae)

        return NativeComparison(
            nativeDuration = nativeDuration,
            userDuration = userDuration,
            speedRatio = speedRatio,
            rhythmSimilarity = rhythmSimilarity,
            pitchSimilarity = pitchSimilarity
        )
    }

    /**
     * Calculate rhythm similarity using simplified DTW
     */
    private fun calculateRhythmSimilarity(
        userDurations: List<Int>,
        nativeDurations: List<Int>
    ): Float {
        if (userDurations.isEmpty() || nativeDurations.isEmpty()) return 0f

        // Normalize durations
        val userNorm = normalizeDurations(userDurations)
        val nativeNorm = normalizeDurations(nativeDurations)

        // Calculate DTW distance
        val distance = dtwDistance(userNorm, nativeNorm)

        // Convert distance to similarity (0-100)
        val maxDistance = maxOf(userNorm.size, nativeNorm.size).toFloat()
        val similarity = maxOf(0f, 100f - (distance / maxDistance * 100f))

        return similarity
    }

    /**
     * Normalize duration values to 0-1 range
     */
    private fun normalizeDurations(durations: List<Int>): List<Float> {
        val max = durations.maxOrNull()?.toFloat() ?: 1f
        val min = durations.minOrNull()?.toFloat() ?: 0f
        val range = max - min

        return if (range > 0) {
            durations.map { (it - min) / range }
        } else {
            durations.map { 0.5f }
        }
    }

    /**
     * Dynamic Time Warping distance
     */
    private fun dtwDistance(seq1: List<Float>, seq2: List<Float>): Float {
        val n = seq1.size
        val m = seq2.size

        // Create cost matrix
        val dtw = Array(n + 1) { FloatArray(m + 1) { Float.MAX_VALUE } }
        dtw[0][0] = 0f

        // Fill matrix
        for (i in 1..n) {
            for (j in 1..m) {
                val cost = abs(seq1[i - 1] - seq2[j - 1])
                dtw[i][j] = cost + minOf(
                    dtw[i - 1][j],     // insertion
                    dtw[i][j - 1],     // deletion
                    dtw[i - 1][j - 1]  // match
                )
            }
        }

        return dtw[n][m]
    }

    /**
     * Calculate pitch pattern similarity
     */
    private fun calculatePitchSimilarity(
        userMorae: List<Mora>,
        nativeMorae: List<Mora>
    ): Float {
        val minSize = minOf(userMorae.size, nativeMorae.size)
        if (minSize == 0) return 0f

        // Compare high/low patterns
        var matches = 0
        for (i in 0 until minSize) {
            if (userMorae[i].isHigh == nativeMorae[i].isHigh) {
                matches++
            }
        }

        return (matches.toFloat() / minSize) * 100f
    }

    /**
     * Detect if speech is too monotone
     */
    fun detectMonotone(morae: List<Mora>): Boolean {
        if (morae.size < 3) return false

        // Check if all morae have similar pitch
        val pitches = morae.map { it.pitch }
        val avgPitch = pitches.average().toFloat()
        val variations = pitches.map { abs(it - avgPitch) / avgPitch }

        // If 90% of morae have less than 10% pitch variation, it's monotone
        val monotoneCount = variations.count { it < 0.1f }
        return monotoneCount.toFloat() / morae.size > 0.9f
    }

    /**
     * Detect rushed speech (too fast with poor articulation)
     */
    fun detectRushedSpeech(morae: List<Mora>): Boolean {
        if (morae.isEmpty()) return false

        val avgDuration = morae.map { it.duration }.average()
        val hasShortMorae = morae.count { it.duration < 60 } > morae.size / 2

        return avgDuration < NATIVE_MORA_DURATION_MIN && hasShortMorae
    }

    /**
     * Suggest improvements based on rhythm analysis
     */
    fun getSuggestions(analysis: RhythmAnalysis): List<String> {
        val suggestions = mutableListOf<String>()

        when (analysis.speedRating) {
            SpeedRating.TOO_SLOW -> suggestions.add(
                "もっと速く話してみましょう。ネイティブのスピードに近づけましょう。"
            )
            SpeedRating.SLOW -> suggestions.add(
                "少し速度を上げてみましょう。"
            )
            SpeedRating.TOO_FAST -> suggestions.add(
                "少しゆっくり話してください。明瞭さが大切です。"
            )
            SpeedRating.FAST -> suggestions.add(
                "少し落ち着いて話すと、さらに良くなります。"
            )
            SpeedRating.NATURAL -> suggestions.add(
                "スピードは自然です！"
            )
        }

        if (analysis.rhythmScore < 70) {
            suggestions.add(
                "リズムがやや不規則です。モーラを均等に発音しましょう。"
            )
        }

        if (analysis.naturalness < 70) {
            suggestions.add(
                "ネイティブの音声をよく聞いて、リズムを真似してみましょう。"
            )
        }

        val pauseRatio = analysis.pauseLocations.size.toFloat() /
                         (analysis.moraDurations.size / 8f)

        when {
            pauseRatio < 0.3f -> suggestions.add(
                "適切な位置で息継ぎをしましょう。"
            )
            pauseRatio > 2f -> suggestions.add(
                "ポーズが多すぎます。もう少し流暢に話しましょう。"
            )
        }

        return suggestions
    }
}
