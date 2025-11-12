package com.nihongo.conversation.core.difficulty

import com.nihongo.conversation.core.cache.JapaneseTextNormalizer

/**
 * Common Japanese vocabulary for quick complexity analysis
 * Phase 4 Improvements:
 * - Uses JapaneseTextNormalizer for consistent text processing
 * - Character-based coverage instead of word-based (better for Japanese)
 * - Particle filtering to avoid inflating coverage
 * - Grammar patterns removed (moved to GrammarPatterns)
 * - Calibrated thresholds with target ranges
 */
object CommonVocabulary {

    private val normalizer = JapaneseTextNormalizer.INSTANCE

    /**
     * Common Japanese particles (excluded from coverage analysis)
     * These are structural elements, not content vocabulary
     */
    private val PARTICLES = setOf(
        "は", "が", "を", "に", "で", "と", "の", "へ", "も",
        "から", "まで", "や", "より", "か", "ね", "よ", "な",
        "わ", "さ", "ぞ", "ぜ"
    )

    /**
     * N5 level - Most basic words (100+ words)
     * Pure vocabulary - no grammar patterns
     */
    val N5_COMMON = setOf(
        // Verbs
        "です", "ます", "ある", "いる", "する", "くる", "行く", "来る",
        "見る", "食べる", "飲む", "買う", "読む", "書く", "聞く", "話す",
        "分かる", "知る", "会う", "待つ", "帰る", "出る", "入る", "立つ",
        "座る", "寝る", "起きる", "開く", "閉める", "始まる", "終わる",
        "使う", "持つ", "取る", "売る", "作る", "働く", "遊ぶ", "歌う",
        "泳ぐ", "走る", "歩く", "乗る", "降りる", "渡る", "曲がる",

        // Adjectives
        "いい", "悪い", "大きい", "小さい", "新しい", "古い", "高い", "安い",
        "多い", "少ない", "長い", "短い", "早い", "遅い", "暑い", "寒い",
        "暖かい", "涼しい", "明るい", "暗い", "重い", "軽い", "強い", "弱い",
        "きれい", "静か", "賑やか", "便利", "元気", "好き", "嫌い",

        // Nouns
        "人", "私", "あなた", "彼", "彼女", "先生", "学生", "友達",
        "家", "学校", "会社", "店", "駅", "病院", "図書館", "公園",
        "時間", "今", "明日", "昨日", "朝", "昼", "夜", "午前", "午後",
        "月", "火", "水", "木", "金", "土", "日", "春", "夏", "秋", "冬",
        "水", "お茶", "コーヒー", "食べ物", "魚", "肉", "野菜", "果物",

        // Common greetings (lexical items)
        "ありがとう", "すみません", "ごめんなさい", "おはよう", "こんにちは",
        "こんばんは", "さようなら", "お願いします", "いただきます", "ごちそうさま"
    )

    /**
     * N4 level - Basic conversation words (100+ words)
     * Grammar patterns moved to GrammarPatterns
     */
    val N4_COMMON = setOf(
        // Verbs
        "できる", "思う", "言う", "教える", "習う", "覚える", "忘れる",
        "探す", "見つける", "貸す", "借りる", "あげる", "もらう", "くれる",
        "送る", "届ける", "変える", "直す", "壊れる", "落ちる", "上がる",
        "下がる", "増える", "減る", "集める", "選ぶ", "決める", "比べる",
        "続く", "続ける", "残る", "残す", "困る", "心配する", "安心する",

        // Adjectives
        "難しい", "易しい", "若い", "優しい", "厳しい", "恥ずかしい",
        "嬉しい", "悲しい", "楽しい", "つまらない", "面白い", "怖い",
        "危ない", "痛い", "美味しい", "不味い", "甘い", "辛い", "苦い",
        "大切", "必要", "不要", "簡単", "複雑", "特別", "普通", "有名",

        // Nouns
        "生活", "経験", "習慣", "機会", "予定", "計画", "約束", "準備",
        "意見", "気持ち", "考え", "理由", "原因", "結果", "問題", "答え",
        "練習", "試験", "宿題", "質問", "説明", "相談", "連絡", "返事",
        "天気", "季節", "自然", "世界", "社会", "文化", "歴史", "科学",
        "趣味", "旅行", "運動", "音楽", "映画", "写真", "料理", "買い物"
    )

    /**
     * N3 level - Intermediate words (100+ words)
     * Grammar patterns moved to GrammarPatterns
     */
    val N3_COMMON = setOf(
        // Verbs
        "受ける", "失敗する", "成功する", "努力する", "頑張る", "諦める",
        "我慢する", "感じる", "感動する", "驚く", "喜ぶ", "怒る", "泣く",
        "笑う", "謝る", "許す", "信じる", "疑う", "期待する", "想像する",
        "確認する", "注意する", "気をつける", "慣れる", "付き合う",
        "参加する", "協力する", "相談する", "説明する", "伝える",

        // Adjectives & Na-adjectives
        "珍しい", "懐かしい", "羨ましい", "残念", "幸せ", "不幸",
        "正直", "素直", "真面目", "熱心", "積極的", "消極的",
        "具体的", "抽象的", "現実的", "理想的", "可能", "不可能",

        // Nouns
        "目的", "効果", "影響", "関係", "場合", "状況", "条件", "基準",
        "能力", "才能", "性格", "態度", "雰囲気", "印象", "評価", "批判",
        "知識", "情報", "技術", "方法", "手段", "過程", "段階", "順序",
        "現象", "事実", "真実", "嘘", "秘密", "噂", "誤解", "理解",
        "発展", "進歩", "改善", "変化", "成長", "減少", "増加"
    )

    /**
     * All vocabulary combined (for advanced learners)
     */
    val ALL_VOCABULARY = N5_COMMON + N4_COMMON + N3_COMMON

    /**
     * Coverage target ranges per difficulty level
     * Based on typical learner comprehension rates
     */
    data class CoverageTarget(
        val low: Float,      // Below this = too hard
        val target: Float,   // Ideal range
        val high: Float      // Above this = too easy
    )

    val COVERAGE_TARGETS = mapOf(
        DifficultyLevel.BEGINNER to CoverageTarget(0.5f, 0.7f, 0.85f),
        DifficultyLevel.INTERMEDIATE to CoverageTarget(0.4f, 0.6f, 0.75f),
        DifficultyLevel.ADVANCED to CoverageTarget(0.3f, 0.5f, 0.65f)
    )

    /**
     * Analyze vocabulary level using character-based coverage
     * Phase 4: More accurate than word-based for Japanese
     */
    fun analyzeVocabularyLevel(text: String): VocabularyLevel {
        // Normalize text
        val normalized = normalizer.normalize(text)
        if (normalized.isEmpty()) return VocabularyLevel.UNKNOWN

        // Remove particles for analysis
        val contentText = removeParticles(normalized)
        if (contentText.isEmpty()) return VocabularyLevel.UNKNOWN

        // Calculate character-based coverage
        val n5Coverage = calculateCharacterCoverage(contentText, N5_COMMON)
        val n4Coverage = calculateCharacterCoverage(contentText, N4_COMMON)
        val n3Coverage = calculateCharacterCoverage(contentText, N3_COMMON)

        return when {
            n5Coverage > 0.7 -> VocabularyLevel.N5
            n5Coverage > 0.5 && n4Coverage > 0.3 -> VocabularyLevel.N4_N5
            n4Coverage > 0.5 -> VocabularyLevel.N4
            n3Coverage > 0.4 -> VocabularyLevel.N3
            n3Coverage > 0.2 -> VocabularyLevel.N2_N1
            else -> VocabularyLevel.N2_N1
        }
    }

    /**
     * Get character-based coverage ratio
     * Phase 4: Better than word-based for Japanese (no word boundaries)
     */
    fun getCoverageRatio(text: String, level: DifficultyLevel): Float {
        val normalized = normalizer.normalize(text)
        if (normalized.isEmpty()) return 0f

        val contentText = removeParticles(normalized)
        if (contentText.isEmpty()) return 0f

        val knownWords = when (level) {
            DifficultyLevel.VERY_BEGINNER -> N5_COMMON.take(300).toSet()  // 300-500어
            DifficultyLevel.BEGINNER -> N5_COMMON  // 500-1000어
            DifficultyLevel.INTERMEDIATE -> N5_COMMON + N4_COMMON  // 1000-3000어
            DifficultyLevel.ADVANCED -> N5_COMMON + N4_COMMON + N3_COMMON  // 3000-6000어
            DifficultyLevel.VERY_ADVANCED -> ALL_VOCABULARY  // 6000어 이상
        }

        return calculateCharacterCoverage(contentText, knownWords)
    }

    /**
     * Calculate what percentage of characters are covered by known vocabulary
     * More accurate than word-based counting for Japanese
     */
    private fun calculateCharacterCoverage(text: String, knownWords: Set<String>): Float {
        if (text.isEmpty()) return 0f

        // Normalize known words
        val normalizedKnown = knownWords.map { normalizer.normalize(it) }.toSet()

        // Track which characters are covered
        val coveredChars = BooleanArray(text.length) { false }

        // For each known word, mark covered characters
        for (word in normalizedKnown) {
            if (word.isEmpty()) continue

            var startIndex = 0
            while (startIndex < text.length) {
                val index = text.indexOf(word, startIndex)
                if (index == -1) break

                // Mark characters as covered
                for (i in index until (index + word.length).coerceAtMost(text.length)) {
                    coveredChars[i] = true
                }

                startIndex = index + 1
            }
        }

        // Calculate coverage ratio
        val coveredCount = coveredChars.count { it }
        return coveredCount.toFloat() / text.length
    }

    /**
     * Remove particles from text for cleaner analysis
     */
    private fun removeParticles(text: String): String {
        var result = text
        for (particle in PARTICLES) {
            result = result.replace(particle, "")
        }
        return result
    }

    /**
     * Get coverage assessment for a text
     */
    fun assessCoverage(text: String, level: DifficultyLevel): CoverageAssessment {
        val coverage = getCoverageRatio(text, level)
        val target = COVERAGE_TARGETS[level] ?: COVERAGE_TARGETS[DifficultyLevel.BEGINNER]!!

        return when {
            coverage < target.low -> CoverageAssessment.TOO_HARD
            coverage > target.high -> CoverageAssessment.TOO_EASY
            coverage < target.target -> CoverageAssessment.SLIGHTLY_HARD
            coverage > target.target -> CoverageAssessment.SLIGHTLY_EASY
            else -> CoverageAssessment.OPTIMAL
        }
    }

    /**
     * Generate adaptive nudge based on coverage
     */
    fun getAdaptiveNudge(text: String, level: DifficultyLevel): String? {
        val assessment = assessCoverage(text, level)

        return when (assessment) {
            CoverageAssessment.TOO_HARD -> when (level) {
                DifficultyLevel.VERY_BEGINNER -> "もっと簡単な言葉で話してください。"
                DifficultyLevel.BEGINNER -> "もっと簡単な言葉で、短い文で話してください。"
                DifficultyLevel.INTERMEDIATE -> "少し簡単な表現を使ってください。"
                DifficultyLevel.ADVANCED -> "もう少し分かりやすく説明してください。"
                DifficultyLevel.VERY_ADVANCED -> "基本的な表現も加えてください。"
            }
            CoverageAssessment.TOO_EASY -> when (level) {
                DifficultyLevel.VERY_BEGINNER -> null  // Easy is fine for very beginners
                DifficultyLevel.BEGINNER -> null  // Easy is fine for beginners
                DifficultyLevel.INTERMEDIATE -> "もう少し自然な表現を使ってもいいですよ。"
                DifficultyLevel.ADVANCED -> "より高度な語彙や表現を使ってください。"
                DifficultyLevel.VERY_ADVANCED -> "専門的な表現を使ってください。"
            }
            else -> null  // Optimal or close enough
        }
    }
}

/**
 * Vocabulary level classification
 */
enum class VocabularyLevel {
    N5,       // Beginner
    N4_N5,    // Beginner-Elementary
    N4,       // Elementary
    N3,       // Intermediate
    N2_N1,    // Advanced
    UNKNOWN   // Cannot determine
}

/**
 * Coverage assessment result
 */
enum class CoverageAssessment {
    TOO_HARD,      // Coverage below low threshold
    SLIGHTLY_HARD, // Coverage below target
    OPTIMAL,       // Coverage at target
    SLIGHTLY_EASY, // Coverage above target
    TOO_EASY       // Coverage above high threshold
}
