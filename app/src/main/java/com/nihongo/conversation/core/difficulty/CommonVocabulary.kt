package com.nihongo.conversation.core.difficulty

/**
 * Common Japanese vocabulary for quick complexity analysis
 * Lightweight alternative to full JLPT word lists
 * Contains 100-200 most frequently used words per level
 */
object CommonVocabulary {

    /**
     * N5 level - Most basic words (100 words)
     * Beginner learners should recognize all of these
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

        // Common phrases
        "ありがとう", "すみません", "ごめんなさい", "おはよう", "こんにちは",
        "こんばんは", "さようなら", "お願いします", "いただきます", "ごちそうさま"
    )

    /**
     * N4 level - Basic conversation words (100 words)
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
        "趣味", "旅行", "運動", "音楽", "映画", "写真", "料理", "買い物",

        // Expressions
        "多分", "きっと", "たぶん", "もちろん", "実は", "やっぱり",
        "ちょっと", "もう", "まだ", "また", "すぐ", "ゆっくり"
    )

    /**
     * N3 level - Intermediate words (100 words)
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
        "発展", "進歩", "改善", "変化", "成長", "減少", "増加",

        // Expressions
        "によって", "に対して", "について", "に関して", "のために",
        "ようだ", "そうだ", "らしい", "はずだ", "べきだ", "わけだ"
    )

    /**
     * Check if text primarily uses vocabulary from a specific level
     * Returns the percentage of words that match the given level (0.0 - 1.0)
     */
    fun analyzeVocabularyLevel(text: String): VocabularyLevel {
        // Simple tokenization by common separators
        val words = text.split(Regex("[\\s、。！？　]+"))
            .filter { it.isNotEmpty() }

        if (words.isEmpty()) return VocabularyLevel.UNKNOWN

        val n5Matches = words.count { word -> N5_COMMON.any { word.contains(it) } }
        val n4Matches = words.count { word -> N4_COMMON.any { word.contains(it) } }
        val n3Matches = words.count { word -> N3_COMMON.any { word.contains(it) } }

        val n5Ratio = n5Matches.toFloat() / words.size
        val n4Ratio = n4Matches.toFloat() / words.size
        val n3Ratio = n3Matches.toFloat() / words.size

        return when {
            n5Ratio > 0.6 -> VocabularyLevel.N5
            n4Ratio > 0.4 -> VocabularyLevel.N4
            n3Ratio > 0.3 -> VocabularyLevel.N3
            n5Ratio + n4Ratio > 0.5 -> VocabularyLevel.N4_N5
            else -> VocabularyLevel.N2_N1
        }
    }

    /**
     * Get vocabulary coverage ratio for a text at a given level
     * Returns 0.0 - 1.0 indicating how much of the text uses known vocabulary
     */
    fun getCoverageRatio(text: String, level: DifficultyLevel): Float {
        val words = text.split(Regex("[\\s、。！？　]+"))
            .filter { it.isNotEmpty() }

        if (words.isEmpty()) return 0f

        val knownWords = when (level) {
            DifficultyLevel.BEGINNER -> N5_COMMON
            DifficultyLevel.INTERMEDIATE -> N5_COMMON + N4_COMMON + N3_COMMON
            DifficultyLevel.ADVANCED -> N5_COMMON + N4_COMMON + N3_COMMON
        }

        val matchCount = words.count { word ->
            knownWords.any { known -> word.contains(known) }
        }

        return matchCount.toFloat() / words.size
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
