package com.nihongo.conversation.core.translation

import android.util.Log

/**
 * Local translation dictionary for common Japanese phrases
 * Used as fallback when API fails or for instant translation of common phrases
 */
object LocalTranslationDictionary {

    private const val TAG = "LocalTranslation"

    /**
     * Common phrase translations (Japanese -> Korean)
     * Covers ~200 most common phrases in conversation scenarios
     */
    private val dictionary = mapOf(
        // Greetings & Basic
        "こんにちは" to "안녕하세요",
        "おはようございます" to "좋은 아침입니다",
        "こんばんは" to "안녕하세요 (저녁 인사)",
        "おやすみなさい" to "안녕히 주무세요",
        "さようなら" to "안녕히 가세요",
        "またね" to "또 봐",
        "じゃあね" to "그럼 잘 가",
        "ただいま" to "다녀왔습니다",
        "おかえりなさい" to "어서 오세요",
        "いってきます" to "다녀오겠습니다",
        "いってらっしゃい" to "다녀오세요",

        // Gratitude
        "ありがとうございます" to "감사합니다",
        "ありがとう" to "고마워",
        "どういたしまして" to "천만에요",
        "すみません" to "죄송합니다 / 실례합니다",
        "ごめんなさい" to "미안합니다",
        "ごめん" to "미안해",
        "お願いします" to "부탁합니다",
        "お疲れ様でした" to "수고하셨습니다",
        "よろしくお願いします" to "잘 부탁드립니다",

        // Restaurant
        "いらっしゃいませ" to "어서오세요",
        "メニューをください" to "메뉴 주세요",
        "これをください" to "이것 주세요",
        "お水をください" to "물 주세요",
        "お会計お願いします" to "계산 부탁합니다",
        "いくらですか" to "얼마예요?",
        "美味しいです" to "맛있어요",
        "ご馳走様でした" to "잘 먹었습니다",
        "予約したいです" to "예약하고 싶어요",
        "おすすめは何ですか" to "추천 메뉴는 무엇인가요?",
        "辛いですか" to "매워요?",
        "少々お待ちください" to "잠시만 기다려주세요",

        // Shopping
        "いくらですか" to "얼마예요?",
        "高いです" to "비싸요",
        "安いです" to "싸요",
        "これはいくらですか" to "이것은 얼마예요?",
        "見せてください" to "보여주세요",
        "試着してもいいですか" to "입어봐도 되나요?",
        "もっと安いのはありますか" to "더 싼 게 있나요?",
        "買います" to "살게요",
        "やめます" to "그만둘게요",
        "袋をください" to "봉투 주세요",

        // Hotel
        "チェックインお願いします" to "체크인 부탁합니다",
        "チェックアウトお願いします" to "체크아웃 부탁합니다",
        "予約しました" to "예약했습니다",
        "部屋を変えてください" to "방을 바꿔주세요",
        "Wi-Fiはありますか" to "와이파이 있나요?",
        "朝食は何時からですか" to "아침 식사는 몇 시부터예요?",
        "タオルをください" to "수건 주세요",
        "鍵をなくしました" to "열쇠를 잃어버렸어요",

        // Questions
        "何ですか" to "뭐예요?",
        "どこですか" to "어디예요?",
        "いつですか" to "언제예요?",
        "誰ですか" to "누구예요?",
        "なぜですか" to "왜요?",
        "どうですか" to "어때요?",
        "どうしますか" to "어떻게 할까요?",
        "どうやって行きますか" to "어떻게 가나요?",
        "これは何ですか" to "이것은 뭐예요?",
        "どれですか" to "어느 것이에요?",
        "いくつですか" to "몇 개예요?",

        // Responses
        "はい" to "네",
        "いいえ" to "아니요",
        "そうです" to "그래요",
        "違います" to "아니에요",
        "わかりました" to "알겠습니다",
        "わかりません" to "모르겠어요",
        "知りません" to "몰라요",
        "大丈夫です" to "괜찮아요",
        "いいです" to "좋아요",
        "ダメです" to "안 돼요",
        "結構です" to "괜찮습니다 (사양)",

        // Requests
        "教えてください" to "가르쳐 주세요",
        "手伝ってください" to "도와주세요",
        "待ってください" to "기다려 주세요",
        "もう一度お願いします" to "다시 한번 부탁합니다",
        "ゆっくり話してください" to "천천히 말씀해 주세요",
        "書いてください" to "써주세요",
        "見せてください" to "보여주세요",
        "聞いてください" to "들어주세요",

        // Emergency
        "助けてください" to "도와주세요",
        "危ない" to "위험해요",
        "病院に行きたいです" to "병원에 가고 싶어요",
        "警察を呼んでください" to "경찰을 불러주세요",
        "救急車を呼んでください" to "구급차를 불러주세요",
        "痛いです" to "아파요",
        "頭が痛いです" to "머리가 아파요",
        "お腹が痛いです" to "배가 아파요",
        "熱があります" to "열이 있어요",
        "迷いました" to "길을 잃었어요",

        // Directions
        "どこですか" to "어디예요?",
        "ここはどこですか" to "여기는 어디예요?",
        "駅はどこですか" to "역은 어디예요?",
        "トイレはどこですか" to "화장실은 어디예요?",
        "まっすぐ行ってください" to "똑바로 가세요",
        "右に曲がってください" to "오른쪽으로 도세요",
        "左に曲がってください" to "왼쪽으로 도세요",
        "近いです" to "가까워요",
        "遠いです" to "멀어요",

        // Time
        "今何時ですか" to "지금 몇 시예요?",
        "今日" to "오늘",
        "明日" to "내일",
        "昨日" to "어제",
        "今" to "지금",
        "後で" to "나중에",
        "早い" to "빠르다",
        "遅い" to "늦다",

        // Common verbs (polite form)
        "行きます" to "가요",
        "来ます" to "와요",
        "帰ります" to "돌아가요",
        "食べます" to "먹어요",
        "飲みます" to "마셔요",
        "見ます" to "봐요",
        "聞きます" to "들어요",
        "話します" to "이야기해요",
        "読みます" to "읽어요",
        "書きます" to "써요",
        "買います" to "사요",
        "売ります" to "팔아요",
        "します" to "해요",
        "できます" to "할 수 있어요",
        "わかります" to "알아요",
        "知っています" to "알고 있어요",
        "思います" to "생각해요",
        "欲しいです" to "갖고 싶어요",
        "好きです" to "좋아해요",
        "嫌いです" to "싫어해요",

        // Common adjectives
        "良いです" to "좋아요",
        "悪いです" to "나빠요",
        "大きいです" to "커요",
        "小さいです" to "작아요",
        "新しいです" to "새로워요",
        "古いです" to "오래됐어요",
        "高いです" to "비싸요 / 높아요",
        "安いです" to "싸요 / 낮아요",
        "暑いです" to "더워요",
        "寒いです" to "추워요",
        "暖かいです" to "따뜻해요",
        "涼しいです" to "시원해요",
        "美味しいです" to "맛있어요",
        "まずいです" to "맛없어요",
        "難しいです" to "어려워요",
        "簡単です" to "쉬워요",
        "忙しいです" to "바빠요",
        "暇です" to "한가해요",

        // Numbers
        "一つ" to "하나",
        "二つ" to "둘",
        "三つ" to "셋",
        "四つ" to "넷",
        "五つ" to "다섯",
        "一人" to "한 명",
        "二人" to "두 명",
        "三人" to "세 명",

        // Common sentence endings
        "〜ですか" to "~인가요?",
        "〜ください" to "~주세요",
        "〜ましょう" to "~합시다",
        "〜たいです" to "~하고 싶어요",
        "〜ませんか" to "~하지 않을래요?",
        "〜てもいいですか" to "~해도 되나요?",
        "〜ても構いません" to "~해도 괜찮아요"
    )

    /**
     * Partial matching dictionary for common patterns
     * Used when exact match fails
     */
    private val partialMatches = mapOf(
        "ください" to "주세요",
        "お願いします" to "부탁합니다",
        "ありがとう" to "감사합니다",
        "すみません" to "죄송합니다",
        "ごめん" to "미안합니다",
        "わかりました" to "알겠습니다",
        "ですか" to "인가요?",
        "ですね" to "이네요",
        "でしょう" to "이겠죠",
        "たいです" to "하고 싶어요",
        "ましょう" to "합시다",
        "ませんか" to "하지 않을래요?",
        "ですから" to "이니까",
        "けど" to "지만",
        "から" to "부터 / 때문에"
    )

    /**
     * Try to translate using local dictionary
     * Returns translation if found, null otherwise
     */
    fun translate(japaneseText: String): String? {
        val normalized = japaneseText.trim()

        // Exact match
        dictionary[normalized]?.let { translation ->
            Log.d(TAG, "Exact match found: '$normalized' -> '$translation'")
            return translation
        }

        // Try with various normalizations
        val variations = listOf(
            normalized,
            normalized.replace("。", ""),
            normalized.replace("！", ""),
            normalized.replace("？", ""),
            normalized.replace(" ", ""),
            normalized.replace("　", "") // Full-width space
        )

        for (variation in variations) {
            dictionary[variation]?.let { translation ->
                Log.d(TAG, "Variation match found: '$variation' -> '$translation'")
                return translation
            }
        }

        // Try partial matching for longer sentences
        if (normalized.length > 5) {
            for ((pattern, translation) in partialMatches) {
                if (normalized.contains(pattern)) {
                    Log.d(TAG, "Partial match found: contains '$pattern'")
                    // This is a hint that we found a pattern, but don't return partial translation
                    // Return null to let API handle full translation
                }
            }
        }

        Log.d(TAG, "No match found for: '$normalized'")
        return null
    }

    /**
     * Check if text is likely translatable by local dictionary
     * Used to optimize API usage
     */
    fun canTranslateLocally(japaneseText: String): Boolean {
        val normalized = japaneseText.trim()
        return dictionary.containsKey(normalized) ||
               dictionary.containsKey(normalized.replace(Regex("[。！？\\s　]"), ""))
    }

    /**
     * Get dictionary size for statistics
     */
    fun getDictionarySize(): Int = dictionary.size

    /**
     * Get all available phrases (for testing/debugging)
     */
    fun getAllPhrases(): List<Pair<String, String>> = dictionary.toList()

    /**
     * Get suggestions for similar phrases
     */
    fun getSimilarPhrases(japaneseText: String, limit: Int = 5): List<Pair<String, String>> {
        val normalized = japaneseText.trim().lowercase()

        return dictionary.entries
            .filter { (key, _) ->
                key.lowercase().contains(normalized) ||
                normalized.contains(key.lowercase())
            }
            .take(limit)
            .map { it.key to it.value }
    }
}
