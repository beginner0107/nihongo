package com.nihongo.conversation.domain.analyzer

import com.nihongo.conversation.domain.model.*
import kotlin.math.*

/**
 * Detects common pronunciation problems for Japanese learners
 * Focuses on sounds that are difficult for non-native speakers
 */
class ProblematicSoundsDetector {

    companion object {
        // Duration thresholds
        private const val LONG_VOWEL_MIN_RATIO = 1.8f // Long vowel should be ~2x normal
        private const val DOUBLE_CONSONANT_MIN = 80 // ms minimum for っ
        private const val DOUBLE_CONSONANT_MAX = 200 // ms maximum for っ

        // Frequency ranges for specific sounds
        private val RA_GYOU_F2_RANGE = 1200f..1800f // Hz (R sound has lower F2 than L)
        private val N_SOUND_NASAL_FREQ = 200f..400f // Hz (nasal resonance)
    }

    /**
     * Detect problematic sounds in user's speech
     */
    fun detect(
        morae: List<Mora>,
        text: String,
        audioData: FloatArray,
        sampleRate: Int
    ): List<ProblematicSound> {
        val problems = mutableListOf<ProblematicSound>()

        // Check each mora for potential issues
        morae.forEachIndexed { index, mora ->
            val moraText = mora.text

            // 1. ら行 (R/L confusion)
            if (moraText.matches(Regex("[らりるれろラリルレロ]"))) {
                detectRaGyouIssue(mora, index, audioData, sampleRate)?.let { problems.add(it) }
            }

            // 2. つ vs ちゅ distinction
            if (moraText.matches(Regex("[つツ]")) || moraText.matches(Regex("ちゅ|チュ"))) {
                detectTsuChuIssue(mora, index)?.let { problems.add(it) }
            }

            // 3. Long vowels
            if (index > 0 && isLongVowel(morae[index - 1].text, moraText)) {
                detectLongVowelIssue(morae[index - 1], mora, index, audioData, sampleRate)
                    ?.let { problems.add(it) }
            }

            // 4. Double consonant (っ)
            if (moraText.matches(Regex("[っッ]"))) {
                detectDoubleConsonantIssue(mora, index)?.let { problems.add(it) }
            }

            // 5. ん sound (syllabic N)
            if (moraText.matches(Regex("[んン]"))) {
                detectNSoundIssue(mora, index, audioData, sampleRate)?.let { problems.add(it) }
            }

            // 6. し vs ち distinction
            if (moraText.matches(Regex("[しちシチ]"))) {
                detectShiChiIssue(mora, index)?.let { problems.add(it) }
            }

            // 7. つ vs す distinction
            if (moraText.matches(Regex("[つすツス]"))) {
                detectTsuSuIssue(mora, index)?.let { problems.add(it) }
            }

            // 8. ふ sound (neither F nor H)
            if (moraText.matches(Regex("[ふフ]"))) {
                detectFuHuIssue(mora, index)?.let { problems.add(it) }
            }
        }

        return problems.sortedByDescending { it.severity }
    }

    /**
     * Detect ら行 (R sound) issues
     * Japanese R is a tap/flap, not L or English R
     */
    private fun detectRaGyouIssue(
        mora: Mora,
        position: Int,
        audioData: FloatArray,
        sampleRate: Int
    ): ProblematicSound? {
        // Check duration - Japanese R should be short (tap)
        if (mora.duration > 120) {
            return ProblematicSound(
                soundType = JapaneseSound.RA_GYOU,
                mora = mora.text,
                position = position,
                issue = PronunciationIssue.WRONG_LENGTH,
                severity = IssueSeverity.MEDIUM,
                suggestion = "日本語の「ら行」は短く、舌を軽く弾きます。英語のRやLではありません。",
                nativeExample = "「らっこ」をゆっくり聞いて真似してください。"
            )
        }

        // Check if too tense (like English R)
        // English R has lower formants
        // This is simplified - real implementation would need formant analysis
        if (mora.pitch < 150) {
            return ProblematicSound(
                soundType = JapaneseSound.RA_GYOU,
                mora = mora.text,
                position = position,
                issue = PronunciationIssue.WRONG_SOUND,
                severity = IssueSeverity.HIGH,
                suggestion = "英語のRのように強く巻きません。舌先を軽く口蓋に触れる程度です。",
                nativeExample = "「りんご」の音声を聞いて練習しましょう。"
            )
        }

        return null
    }

    /**
     * Detect つ vs ちゅ confusion
     */
    private fun detectTsuChuIssue(mora: Mora, position: Int): ProblematicSound? {
        val isTs = mora.text.matches(Regex("[つツ]"))

        // つ should be shorter and more explosive
        // ちゅ should be longer with more friction
        if (isTs && mora.duration > 140) {
            return ProblematicSound(
                soundType = JapaneseSound.TSU_CHU,
                mora = mora.text,
                position = position,
                issue = PronunciationIssue.WRONG_LENGTH,
                severity = IssueSeverity.MEDIUM,
                suggestion = "「つ」は短く発音します。「ちゅ」と混同しないように。",
                nativeExample = "「つくえ」と「注意（ちゅうい）」を比べて聞いてください。"
            )
        }

        return null
    }

    /**
     * Check if second mora creates a long vowel
     */
    private fun isLongVowel(firstMora: String, secondMora: String): Boolean {
        return when {
            // あー、いー、うー、えー、おー patterns
            secondMora.matches(Regex("[ーー]")) -> true

            // ああ、いい、うう、ええ、おお patterns
            firstMora.matches(Regex("[あアaａ]")) && secondMora.matches(Regex("[あア]")) -> true
            firstMora.matches(Regex("[いイiｉ]")) && secondMora.matches(Regex("[いイ]")) -> true
            firstMora.matches(Regex("[うウuｕ]")) && secondMora.matches(Regex("[うウ]")) -> true
            firstMora.matches(Regex("[えエeｅ]")) && secondMora.matches(Regex("[えエいイ]")) -> true
            firstMora.matches(Regex("[おオoｏ]")) && secondMora.matches(Regex("[おオうウ]")) -> true

            else -> false
        }
    }

    /**
     * Detect long vowel issues
     */
    private fun detectLongVowelIssue(
        firstMora: Mora,
        secondMora: Mora,
        position: Int,
        audioData: FloatArray,
        sampleRate: Int
    ): ProblematicSound? {
        val totalDuration = firstMora.duration + secondMora.duration
        val durationRatio = totalDuration.toFloat() / firstMora.duration

        // Long vowel should be roughly 2x normal vowel
        if (durationRatio < LONG_VOWEL_MIN_RATIO) {
            val severity = when {
                durationRatio < 1.3f -> IssueSeverity.CRITICAL // Barely any lengthening
                durationRatio < 1.5f -> IssueSeverity.HIGH
                else -> IssueSeverity.MEDIUM
            }

            return ProblematicSound(
                soundType = JapaneseSound.LONG_VOWEL,
                mora = "${firstMora.text}${secondMora.text}",
                position = position - 1,
                issue = PronunciationIssue.WRONG_LENGTH,
                severity = severity,
                suggestion = "長音はしっかり伸ばしてください。「おばさん」と「おばあさん」は違う意味です。",
                nativeExample = "「おばさん（叔母）」vs「おばあさん（祖母）」を聞き比べてください。"
            )
        }

        return null
    }

    /**
     * Detect double consonant (促音) issues
     */
    private fun detectDoubleConsonantIssue(mora: Mora, position: Int): ProblematicSound? {
        // っ should be a clear pause/silence
        when {
            mora.duration < DOUBLE_CONSONANT_MIN -> {
                return ProblematicSound(
                    soundType = JapaneseSound.DOUBLE_CONSONANT,
                    mora = mora.text,
                    position = position,
                    issue = PronunciationIssue.WRONG_LENGTH,
                    severity = IssueSeverity.HIGH,
                    suggestion = "促音「っ」は小さいですが、しっかりとポーズを取ってください。",
                    nativeExample = "「かた（肩）」と「かった（買った）」を聞き比べてください。"
                )
            }
            mora.duration > DOUBLE_CONSONANT_MAX -> {
                return ProblematicSound(
                    soundType = JapaneseSound.DOUBLE_CONSONANT,
                    mora = mora.text,
                    position = position,
                    issue = PronunciationIssue.WRONG_LENGTH,
                    severity = IssueSeverity.MEDIUM,
                    suggestion = "「っ」のポーズが長すぎます。自然な長さにしましょう。",
                    nativeExample = "「きて（来て）」と「きって（切手）」を聞いてください。"
                )
            }
            mora.intensity > 0.3f -> {
                // っ should be nearly silent
                return ProblematicSound(
                    soundType = JapaneseSound.DOUBLE_CONSONANT,
                    mora = mora.text,
                    position = position,
                    issue = PronunciationIssue.WRONG_SOUND,
                    severity = IssueSeverity.MEDIUM,
                    suggestion = "「っ」は音を出さないでください。完全な無音ではありませんが、非常に小さいです。",
                    nativeExample = "「さか（坂）」と「さっか（作家）」の違いを聞いてください。"
                )
            }
        }

        return null
    }

    /**
     * Detect ん sound issues
     */
    private fun detectNSoundIssue(
        mora: Mora,
        position: Int,
        audioData: FloatArray,
        sampleRate: Int
    ): ProblematicSound? {
        // ん should be nasal with specific resonance
        // Check duration - should not be too short
        if (mora.duration < 60) {
            return ProblematicSound(
                soundType = JapaneseSound.N_SOUND,
                mora = mora.text,
                position = position,
                issue = PronunciationIssue.WRONG_LENGTH,
                severity = IssueSeverity.MEDIUM,
                suggestion = "「ん」は独立した音節です。短すぎないように。",
                nativeExample = "「さん（3）」と「さ（差）」を聞き比べてください。"
            )
        }

        // Check if too oral (not nasal enough)
        // This is simplified - real implementation would need spectral analysis
        if (mora.intensity > 0.7f) {
            return ProblematicSound(
                soundType = JapaneseSound.N_SOUND,
                mora = mora.text,
                position = position,
                issue = PronunciationIssue.UNCLEAR_ARTICULATION,
                severity = IssueSeverity.LOW,
                suggestion = "「ん」は鼻音です。鼻から抜ける音にしてください。",
                nativeExample = "「せんせい」の「ん」を注意して聞いてください。"
            )
        }

        return null
    }

    /**
     * Detect し vs ち issues
     */
    private fun detectShiChiIssue(mora: Mora, position: Int): ProblematicSound? {
        // し is more fricative, ち is more affricate
        // Check if consonant part is clear
        val isShi = mora.text.matches(Regex("[しシ]"))

        // This is simplified heuristic
        if (isShi && mora.duration < 80) {
            return ProblematicSound(
                soundType = JapaneseSound.SHI_CHI,
                mora = mora.text,
                position = position,
                issue = PronunciationIssue.WRONG_LENGTH,
                severity = IssueSeverity.LOW,
                suggestion = "「し」の摩擦音をしっかり出してください。",
                nativeExample = "「しお（塩）」と「ちず（地図）」を聞き比べてください。"
            )
        }

        return null
    }

    /**
     * Detect つ vs す issues
     */
    private fun detectTsuSuIssue(mora: Mora, position: Int): ProblematicSound? {
        val isTs = mora.text.matches(Regex("[つツ]"))

        // つ should have an explosive onset, す is purely fricative
        if (isTs && mora.duration > 130) {
            return ProblematicSound(
                soundType = JapaneseSound.TSU_SU,
                mora = mora.text,
                position = position,
                issue = PronunciationIssue.WRONG_SOUND,
                severity = IssueSeverity.MEDIUM,
                suggestion = "「つ」は破裂音です。「す」のような摩擦音ではありません。",
                nativeExample = "「つき（月）」と「すき（好き）」を聞き比べてください。"
            )
        }

        return null
    }

    /**
     * Detect ふ sound issues
     */
    private fun detectFuHuIssue(mora: Mora, position: Int): ProblematicSound? {
        // ふ is bilabial fricative (両唇摩擦音)
        // Not labiodental F, not glottal H

        // If intensity is too strong, might be wrong articulation
        if (mora.intensity > 0.8f) {
            return ProblematicSound(
                soundType = JapaneseSound.FU_HU,
                mora = mora.text,
                position = position,
                issue = PronunciationIssue.WRONG_SOUND,
                severity = IssueSeverity.LOW,
                suggestion = "「ふ」は唇の間から優しく息を出します。英語のFやHではありません。",
                nativeExample = "「ふうせん（風船）」を聞いて練習してください。"
            )
        }

        return null
    }

    /**
     * Generate practice exercises for detected problems
     */
    fun generatePracticeExercises(problems: List<ProblematicSound>): List<SoundPracticeExercise> {
        val exercises = mutableListOf<SoundPracticeExercise>()

        // Group by sound type
        val problemsByType = problems.groupBy { it.soundType }

        problemsByType.forEach { (soundType, soundProblems) ->
            val exercise = when (soundType) {
                JapaneseSound.RA_GYOU -> createRaGyouExercise()
                JapaneseSound.TSU_CHU -> createTsuChuExercise()
                JapaneseSound.LONG_VOWEL -> createLongVowelExercise()
                JapaneseSound.DOUBLE_CONSONANT -> createDoubleConsonantExercise()
                JapaneseSound.N_SOUND -> createNSoundExercise()
                JapaneseSound.SHI_CHI -> createShiChiExercise()
                JapaneseSound.TSU_SU -> createTsuSuExercise()
                JapaneseSound.FU_HU -> createFuHuExercise()
                else -> null
            }

            exercise?.let { exercises.add(it) }
        }

        return exercises
    }

    private fun createRaGyouExercise() = SoundPracticeExercise(
        soundType = JapaneseSound.RA_GYOU,
        targetMora = "ら",
        exampleWords = listOf("らっこ", "りんご", "るーる", "れい", "ろーま"),
        minimalPairs = MinimalPairs.RA_LA,
        tips = listOf(
            "舌先を軽く歯茎に触れて弾きます",
            "英語のRやLとは違います",
            "短く発音します"
        )
    )

    private fun createTsuChuExercise() = SoundPracticeExercise(
        soundType = JapaneseSound.TSU_CHU,
        targetMora = "つ",
        exampleWords = listOf("つくえ", "つづく", "つき", "つめたい"),
        minimalPairs = MinimalPairs.TSU_CHU,
        tips = listOf(
            "「つ」は短く破裂させます",
            "「ちゅ」は摩擦音が長く続きます"
        )
    )

    private fun createLongVowelExercise() = SoundPracticeExercise(
        soundType = JapaneseSound.LONG_VOWEL,
        targetMora = "あー",
        exampleWords = listOf("おばあさん", "こうこう", "しゅうじん", "せんせい"),
        minimalPairs = MinimalPairs.LONG_SHORT,
        tips = listOf(
            "長音は通常の母音の約2倍の長さです",
            "意味が変わることがあるので注意"
        )
    )

    private fun createDoubleConsonantExercise() = SoundPracticeExercise(
        soundType = JapaneseSound.DOUBLE_CONSONANT,
        targetMora = "っ",
        exampleWords = listOf("がっこう", "きって", "さっか", "ずっと"),
        minimalPairs = MinimalPairs.DOUBLE_CONSONANT,
        tips = listOf(
            "明確なポーズを作ります",
            "完全な無音ではありませんが、非常に小さい"
        )
    )

    private fun createNSoundExercise() = SoundPracticeExercise(
        soundType = JapaneseSound.N_SOUND,
        targetMora = "ん",
        exampleWords = listOf("さん", "せんせい", "ほん", "てんぷら"),
        minimalPairs = listOf("さ" to "さん", "せ" to "せん"),
        tips = listOf(
            "鼻から抜ける音です",
            "独立した音節として発音"
        )
    )

    private fun createShiChiExercise() = SoundPracticeExercise(
        soundType = JapaneseSound.SHI_CHI,
        targetMora = "し",
        exampleWords = listOf("しお", "しち", "しんぶん"),
        minimalPairs = listOf("しお" to "ちず", "しち" to "ちち"),
        tips = listOf(
            "「し」は摩擦音",
            "「ち」は破擦音"
        )
    )

    private fun createTsuSuExercise() = SoundPracticeExercise(
        soundType = JapaneseSound.TSU_SU,
        targetMora = "つ",
        exampleWords = listOf("つき", "つる", "つづく"),
        minimalPairs = listOf("つき" to "すき", "つる" to "する"),
        tips = listOf(
            "「つ」は破裂音から始まります",
            "「す」は純粋な摩擦音です"
        )
    )

    private fun createFuHuExercise() = SoundPracticeExercise(
        soundType = JapaneseSound.FU_HU,
        targetMora = "ふ",
        exampleWords = listOf("ふうせん", "ふじさん", "ふゆ"),
        minimalPairs = listOf("ふ" to "は", "ふね" to "はね"),
        tips = listOf(
            "両唇から優しく息を出します",
            "英語のFやHではありません"
        )
    )
}
