package com.nihongo.conversation.domain.analyzer

import com.nihongo.conversation.domain.model.*
import kotlin.math.*

/**
 * Analyzes pitch accent patterns in Japanese speech
 * Uses fundamental frequency (F0) extraction and mora segmentation
 */
class PitchAccentAnalyzer {

    companion object {
        private const val SAMPLE_RATE = 16000 // Hz
        private const val MIN_F0 = 80f // Minimum fundamental frequency
        private const val MAX_F0 = 400f // Maximum fundamental frequency
        private const val MORA_MIN_DURATION = 80 // Milliseconds
        private const val MORA_MAX_DURATION = 250 // Milliseconds
        private const val PITCH_THRESHOLD = 0.5f // Confidence threshold
    }

    /**
     * Analyze pitch accent from audio data
     */
    fun analyze(
        audioData: FloatArray,
        sampleRate: Int = SAMPLE_RATE,
        text: String,
        expectedPattern: String? = null
    ): PitchAccentAnalysis {
        // 1. Extract pitch contour
        val pitchPoints = extractPitchContour(audioData, sampleRate)

        // 2. Segment into morae
        val morae = segmentIntoMorae(audioData, pitchPoints, text, sampleRate)

        // 3. Classify accent type
        val accentType = classifyAccentType(morae)

        // 4. Determine accent position
        val accentPosition = findAccentNucleus(morae)

        // 5. Compare with expected pattern if provided
        val matchesNative = expectedPattern?.let {
            checkPatternMatch(morae, it)
        } ?: false

        // 6. Calculate confidence
        val confidence = calculateConfidence(pitchPoints, morae)

        return PitchAccentAnalysis(
            morae = morae,
            accentType = accentType,
            accentPosition = accentPosition,
            pitchPattern = pitchPoints,
            matchesNative = matchesNative,
            confidence = confidence
        )
    }

    /**
     * Extract fundamental frequency (F0) over time
     * Uses autocorrelation method for pitch detection
     */
    private fun extractPitchContour(
        audioData: FloatArray,
        sampleRate: Int
    ): List<PitchPoint> {
        val pitchPoints = mutableListOf<PitchPoint>()
        val frameSize = (sampleRate * 0.03).toInt() // 30ms frames
        val hopSize = (sampleRate * 0.01).toInt() // 10ms hop

        var frameStart = 0
        while (frameStart + frameSize < audioData.size) {
            val frame = audioData.copyOfRange(frameStart, frameStart + frameSize)
            val pitch = detectPitch(frame, sampleRate)

            if (pitch > 0) {
                val timeMs = (frameStart * 1000) / sampleRate
                pitchPoints.add(
                    PitchPoint(
                        time = timeMs,
                        frequency = pitch,
                        mora = "" // Will be assigned during segmentation
                    )
                )
            }

            frameStart += hopSize
        }

        return pitchPoints
    }

    /**
     * Detect pitch using autocorrelation
     * Returns frequency in Hz, or -1 if no pitch detected
     */
    private fun detectPitch(frame: FloatArray, sampleRate: Int): Float {
        val minLag = (sampleRate / MAX_F0).toInt()
        val maxLag = (sampleRate / MIN_F0).toInt()

        // Apply window function
        val windowed = applyHammingWindow(frame)

        // Autocorrelation
        var maxCorr = 0f
        var bestLag = minLag

        for (lag in minLag..maxLag) {
            var corr = 0f
            for (i in 0 until windowed.size - lag) {
                corr += windowed[i] * windowed[i + lag]
            }

            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }

        // Check if pitch is reliable
        val r0 = windowed.sumOf { (it * it).toDouble() }.toFloat()
        val confidence = if (r0 > 0) maxCorr / r0 else 0f

        return if (confidence > PITCH_THRESHOLD) {
            sampleRate.toFloat() / bestLag
        } else {
            -1f
        }
    }

    /**
     * Apply Hamming window to reduce spectral leakage
     */
    private fun applyHammingWindow(data: FloatArray): FloatArray {
        val n = data.size
        return FloatArray(n) { i ->
            val window = 0.54 - 0.46 * cos(2.0 * PI * i / (n - 1))
            (data[i] * window).toFloat()
        }
    }

    /**
     * Segment audio into morae based on pitch and intensity
     */
    private fun segmentIntoMorae(
        audioData: FloatArray,
        pitchPoints: List<PitchPoint>,
        text: String,
        sampleRate: Int
    ): List<Mora> {
        val moraTexts = extractMoraText(text)
        val morae = mutableListOf<Mora>()

        // Calculate energy contour
        val energyContour = calculateEnergyContour(audioData, sampleRate)

        // Find mora boundaries based on energy and pitch changes
        val boundaries = findMoraBoundaries(energyContour, pitchPoints, moraTexts.size)

        // Create Mora objects
        for (i in moraTexts.indices) {
            val startTime = if (i < boundaries.size) boundaries[i] else 0
            val endTime = if (i + 1 < boundaries.size) boundaries[i + 1] else pitchPoints.lastOrNull()?.time ?: 0
            val duration = endTime - startTime

            // Get average pitch and intensity for this mora
            val moraPoints = pitchPoints.filter { it.time in startTime..endTime }
            val avgPitch = moraPoints.map { it.frequency }.average().toFloat()
            val avgIntensity = calculateAverageIntensity(audioData, startTime, endTime, sampleRate)

            // Determine if this mora is high or low pitch
            val isHigh = if (i > 0) {
                avgPitch > morae[i - 1].pitch
            } else {
                avgPitch > pitchPoints.map { it.frequency }.average()
            }

            morae.add(
                Mora(
                    text = moraTexts[i],
                    isHigh = isHigh,
                    duration = duration,
                    startTime = startTime,
                    pitch = avgPitch,
                    intensity = avgIntensity
                )
            )
        }

        return morae
    }

    /**
     * Extract mora text from Japanese text
     * Example: "こんにちは" -> ["こ", "ん", "に", "ち", "は"]
     */
    private fun extractMoraText(text: String): List<String> {
        val morae = mutableListOf<String>()
        var i = 0

        while (i < text.length) {
            val char = text[i]

            // Check if next character is a small kana (ゃ, ゅ, ょ, ャ, ュ, ョ, っ, ッ)
            val isSmallKana = if (i + 1 < text.length) {
                text[i + 1] in "ゃゅょャュョっッ"
            } else false

            if (isSmallKana) {
                // Combine current char with small kana
                morae.add(text.substring(i, i + 2))
                i += 2
            } else if (char == 'ん' || char == 'ン') {
                // ん/ン is always separate mora
                morae.add(char.toString())
                i++
            } else if (char == 'っ' || char == 'ッ') {
                // Double consonant (sokuon) is separate mora
                morae.add(char.toString())
                i++
            } else {
                // Regular character
                morae.add(char.toString())
                i++
            }
        }

        return morae
    }

    /**
     * Calculate energy contour for segmentation
     */
    private fun calculateEnergyContour(audioData: FloatArray, sampleRate: Int): List<Pair<Int, Float>> {
        val frameSize = (sampleRate * 0.02).toInt() // 20ms frames
        val hopSize = (sampleRate * 0.01).toInt() // 10ms hop
        val contour = mutableListOf<Pair<Int, Float>>()

        var frameStart = 0
        while (frameStart + frameSize < audioData.size) {
            val frame = audioData.copyOfRange(frameStart, frameStart + frameSize)
            val energy = frame.sumOf { (it * it).toDouble() }.toFloat()
            val timeMs = (frameStart * 1000) / sampleRate

            contour.add(timeMs to energy)
            frameStart += hopSize
        }

        return contour
    }

    /**
     * Find mora boundaries based on energy dips and pitch changes
     */
    private fun findMoraBoundaries(
        energyContour: List<Pair<Int, Float>>,
        pitchPoints: List<PitchPoint>,
        expectedMoraCount: Int
    ): List<Int> {
        val boundaries = mutableListOf(0) // Start at 0

        // Find energy minima as potential boundaries
        val minima = findLocalMinima(energyContour)

        // Select most prominent minima based on expected count
        val selectedBoundaries = minima
            .sortedByDescending { energyContour[it].second }
            .take(expectedMoraCount - 1)
            .sorted()
            .map { energyContour[it].first }

        boundaries.addAll(selectedBoundaries)
        boundaries.sort()

        return boundaries
    }

    /**
     * Find local minima in energy contour
     */
    private fun findLocalMinima(contour: List<Pair<Int, Float>>): List<Int> {
        val minima = mutableListOf<Int>()

        for (i in 1 until contour.size - 1) {
            if (contour[i].second < contour[i - 1].second &&
                contour[i].second < contour[i + 1].second) {
                minima.add(i)
            }
        }

        return minima
    }

    /**
     * Calculate average intensity in time range
     */
    private fun calculateAverageIntensity(
        audioData: FloatArray,
        startTimeMs: Int,
        endTimeMs: Int,
        sampleRate: Int
    ): Float {
        val startSample = (startTimeMs * sampleRate) / 1000
        val endSample = (endTimeMs * sampleRate) / 1000

        if (startSample >= audioData.size || endSample >= audioData.size) return 0f

        val segment = audioData.copyOfRange(
            maxOf(0, startSample),
            minOf(audioData.size, endSample)
        )

        return sqrt(segment.sumOf { (it * it).toDouble() }.toFloat() / segment.size)
    }

    /**
     * Classify accent type based on mora pitch patterns
     */
    private fun classifyAccentType(morae: List<Mora>): AccentType {
        if (morae.size < 2) return AccentType.HEIBAN

        // Check if pitch drops from first to second mora
        val firstHigh = morae[0].isHigh
        val hasEarlyDrop = !morae[1].isHigh && firstHigh

        // Find where pitch drops (if any)
        val dropPosition = morae.indices.firstOrNull { i ->
            i > 0 && morae[i - 1].isHigh && !morae[i].isHigh
        }

        return when {
            dropPosition == null -> AccentType.HEIBAN // No drop (flat)
            dropPosition == 1 -> AccentType.ATAMADAKA // Drop after first
            dropPosition == morae.size - 1 -> AccentType.ODAKA // Drop at end
            else -> AccentType.NAKADAKA // Drop in middle
        }
    }

    /**
     * Find accent nucleus position (where pitch drops)
     */
    private fun findAccentNucleus(morae: List<Mora>): Int? {
        for (i in 1 until morae.size) {
            if (morae[i - 1].isHigh && !morae[i].isHigh) {
                return i
            }
        }
        return null // HEIBAN (no drop)
    }

    /**
     * Check if pattern matches expected (e.g., "LHHLL")
     */
    private fun checkPatternMatch(morae: List<Mora>, expected: String): Boolean {
        if (morae.size != expected.length) return false

        return morae.indices.all { i ->
            val expectedHigh = expected[i] == 'H'
            morae[i].isHigh == expectedHigh
        }
    }

    /**
     * Calculate overall confidence of analysis
     */
    private fun calculateConfidence(pitchPoints: List<PitchPoint>, morae: List<Mora>): Float {
        if (pitchPoints.isEmpty() || morae.isEmpty()) return 0f

        // Confidence based on:
        // 1. Number of pitch points detected
        // 2. Pitch variation (clear high/low distinction)
        // 3. Mora count consistency

        val pitchDensity = pitchPoints.size.toFloat() / morae.size
        val pitchVariation = calculatePitchVariation(morae)

        return minOf(1f, (pitchDensity * 0.3f + pitchVariation * 0.7f))
    }

    /**
     * Calculate pitch variation (clear high/low distinction)
     */
    private fun calculatePitchVariation(morae: List<Mora>): Float {
        if (morae.size < 2) return 0f

        val highPitches = morae.filter { it.isHigh }.map { it.pitch }
        val lowPitches = morae.filter { !it.isHigh }.map { it.pitch }

        if (highPitches.isEmpty() || lowPitches.isEmpty()) return 0.5f

        val avgHigh = highPitches.average().toFloat()
        val avgLow = lowPitches.average().toFloat()

        // Calculate semitone difference
        val semitones = 12 * log2(avgHigh / avgLow)

        // Good pitch accent has 3-8 semitones difference
        return when {
            semitones in 3f..8f -> 1f
            semitones in 2f..10f -> 0.8f
            else -> 0.5f
        }
    }
}
