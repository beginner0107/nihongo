package com.nihongo.conversation.core.util

import com.nihongo.conversation.data.repository.ConversationRepository
import com.nihongo.conversation.domain.model.Scenario
import com.nihongo.conversation.domain.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataInitializer @Inject constructor(
    private val repository: ConversationRepository,
    private val cacheInitializer: com.nihongo.conversation.core.cache.CacheInitializer
) {
    companion object {
        private const val TAG = "DataInitializer"

        /**
         * Maximum recommended prompt length for optimal API performance
         * Based on Gemini API token limits and response quality
         */
        private const val MAX_PROMPT_LENGTH = 500

        /**
         * Warning threshold - log warning but don't fail
         */
        private const val WARN_PROMPT_LENGTH = 450

        /**
         * Core formatting rules applied to all scenarios
         * Kept concise to fit within API prompt limits
         */
        private const val CORE_FORMAT_RULES = """
【重要】マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください。
日本語の会話文のみを出力してください。"""

        /**
         * Extended rules for scenarios that need more detail
         * Applied only when prompt length allows
         */
        private const val EXTENDED_FORMAT_RULES = """
【絶対厳守】
⚠️ 禁止事項（絶対に出力しないこと）:
- マークダウン記号（**、_など）
- 読み仮名（例：お席（せき））
- 思考過程（THINK、"I should..."、"Let me..."など）
- 英語の説明や計画
- 会話が長くなっても、この規則を守ること

✅ 必須（必ず守ること）:
- 日本語の会話文のみを出力
- キャラクターが実際に話す内容だけを書く
- 最初から日本語で始める"""

        /**
         * Helper to append format rules to scenario prompt
         * Validates prompt length and warns if exceeding limits
         */
        private fun buildPrompt(
            scenarioInstructions: String,
            useExtendedRules: Boolean = false,
            scenarioSlug: String = "unknown"
        ): String {
            val rules = if (useExtendedRules) EXTENDED_FORMAT_RULES else CORE_FORMAT_RULES
            val prompt = """
$scenarioInstructions

$rules
            """.trimIndent()

            // Validate prompt length
            validatePromptLength(prompt, scenarioSlug, useExtendedRules)

            return prompt
        }

        /**
         * Validate that the prompt doesn't exceed recommended length
         * Logs warnings or errors based on severity
         */
        private fun validatePromptLength(
            prompt: String,
            scenarioSlug: String,
            usesExtendedRules: Boolean
        ) {
            val length = prompt.length

            when {
                length > MAX_PROMPT_LENGTH -> {
                    android.util.Log.e(
                        TAG,
                        "❌ Scenario '$scenarioSlug' prompt TOO LONG: ${length} chars (limit: $MAX_PROMPT_LENGTH)\n" +
                        "Consider: ${if (usesExtendedRules) "Using CORE_FORMAT_RULES instead" else "Shortening scenario instructions"}"
                    )
                }
                length > WARN_PROMPT_LENGTH -> {
                    android.util.Log.w(
                        TAG,
                        "⚠️ Scenario '$scenarioSlug' prompt near limit: ${length} chars (warn: $WARN_PROMPT_LENGTH, max: $MAX_PROMPT_LENGTH)"
                    )
                }
                else -> {
                    android.util.Log.d(
                        TAG,
                        "✅ Scenario '$scenarioSlug' prompt OK: ${length} chars"
                    )
                }
            }
        }
    }

    suspend fun initializeDefaultData() = withContext(Dispatchers.IO) {
        // Check if user already exists
        val existingUser = repository.getUser(1L).first()
        if (existingUser == null) {
            repository.createUser(
                User(
                    id = 1L,
                    name = "학습자",
                    level = 1
                )
            )
        }

        // Initialize all scenarios
        initializeScenarios()

        // Initialize response cache
        cacheInitializer.initializeCache()
    }

    private suspend fun initializeScenarios() {
        val scenarios = listOf(
            Scenario(
                id = 1L,
                title = "레스토랑 주문",
                description = "레스토랑에서 주문하는 연습을 합니다",
                difficulty = 1,
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本のレストランの店員です。
                        お客様に丁寧に接客してください。
                        簡単な日本語を使い、お客様が学習できるようにサポートしてください。
                        メニューには、ラーメン（800円）、カレーライス（700円）、寿司（1200円）があります。
                        お客様の注文を受け取り、丁寧に対応してください。
                    """.trimIndent(),
                    scenarioSlug = "restaurant_ordering"
                ),
                slug = "restaurant_ordering",
                promptVersion = 3
            ),
            Scenario(
                id = 2L,
                title = "쇼핑",
                description = "가게에서 쇼핑하는 연습을 합니다",
                difficulty = 1,
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本のコンビニやお店の店員です。
                        お客様が商品を探したり、会計をするのを手伝ってください。
                        簡単な日本語を使い、丁寧に対応してください。
                        値段を聞かれたら答え、おすすめの商品も紹介してください。
                        レジでの会計も自然に進めてください。
                    """.trimIndent(),
                    scenarioSlug = "shopping"
                ),
                slug = "shopping",
                promptVersion = 3
            ),
            Scenario(
                id = 3L,
                title = "호텔에서 체크인",
                description = "호텔에서 체크인하는 연습을 합니다",
                difficulty = 2,
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたはホテルのフロント係です。
                        お客様のチェックインを手伝ってください。
                        予約の確認、部屋の説明、施設の案内などを丁寧に行ってください。
                        朝食の時間、Wi-Fiのパスワード、チェックアウト時間なども案内してください。
                        お客様が快適に過ごせるようサポートしてください。
                    """.trimIndent(),
                    useExtendedRules = true,
                    scenarioSlug = "hotel_checkin"
                ),
                slug = "hotel_checkin",
                promptVersion = 4
            ),
            Scenario(
                id = 4L,
                title = "친구 사귀기",
                description = "새로운 친구와 대화하는 연습을 합니다",
                difficulty = 2,
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本の大学生です。
                        新しく来た留学生と友達になろうとしています。
                        カジュアルな日本語を使い、フレンドリーに会話してください。
                        趣味や好きなこと、週末の予定などについて話しましょう。
                        相手の話をよく聞き、質問もしてください。
                        自然な会話を楽しんでください。
                    """.trimIndent(),
                    useExtendedRules = true,
                    scenarioSlug = "making_friends"
                ),
                slug = "making_friends",
                promptVersion = 4
            ),
            Scenario(
                id = 5L,
                title = "전화로 예약하기",
                description = "전화로 예약이나 문의하는 연습을 합니다",
                difficulty = 3,
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたはレストランやサロンの受付スタッフです。
                        電話での予約や問い合わせに対応してください。
                        日時の確認、人数の確認、お客様の名前と電話番号を聞いてください。
                        丁寧な電話対応の日本語を使ってください。
                        「お電話ありがとうございます」「少々お待ちください」などの
                        電話特有の表現を自然に使ってください。
                    """.trimIndent(),
                    useExtendedRules = true,
                    scenarioSlug = "phone_reservation"
                ),
                slug = "phone_reservation",
                promptVersion = 4
            ),
            Scenario(
                id = 6L,
                title = "병원에서",
                description = "병원에서 증상을 설명하는 연습을 합니다",
                difficulty = 3,
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは病院の医師または看護師です。
                        患者さんの症状を丁寧に聞いてください。
                        「どうしましたか」「いつからですか」「痛みはありますか」など、
                        症状について詳しく質問してください。
                        診察後、簡単な診断と薬の説明をしてください。
                        医療用語は避け、わかりやすい日本語を使ってください。
                    """.trimIndent(),
                    useExtendedRules = true,
                    scenarioSlug = "hospital_visit"
                ),
                slug = "hospital_visit",
                promptVersion = 4
            ),

            // ========== GOAL-BASED ROLE-PLAY SCENARIOS ==========

            Scenario(
                id = 7L,
                title = "취업 면접",
                description = "일본 기업 면접을 보는 연습을 합니다. 자기소개, 지원동기, 질문 대응 등을 배웁니다",
                difficulty = 3,
                category = "BUSINESS",
                estimatedDuration = 15,
                hasGoals = true,
                hasBranching = false,
                replayValue = 4,
                thumbnailEmoji = "💼",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本企業の面接官です。
                        応募者（ユーザー）の面接を行ってください。

                        【面接の流れ】
                        1. まず自己紹介をお願いする
                        2. 志望動機を聞く
                        3. 強みと弱みを聞く
                        4. 質問はありますか？と聞く
                        5. 面接を締めくくる

                        【評価ポイント】
                        - 丁寧な敬語を使えているか
                        - 自己紹介、志望動機、強み・弱みについて話せたか
                        - 適切な質問ができたか

                        【重要】
                        - 面接官らしく、丁寧だが少し硬い口調で話してください
                        - ユーザーの回答に対して、適度にフォローアップ質問をしてください
                        - 面接が自然に終わるよう、15分程度で締めくくってください
                    """.trimIndent(),
                    scenarioSlug = "job_interview"
                ),
                slug = "job_interview",
                promptVersion = 4
            ),

            Scenario(
                id = 8L,
                title = "고객 불만 대응",
                description = "상품 불량이나 서비스에 대한 불만에 대응하는 연습을 합니다",
                difficulty = 3,
                category = "BUSINESS",
                estimatedDuration = 12,
                hasGoals = true,
                hasBranching = false,
                replayValue = 4,
                thumbnailEmoji = "🙇",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは商品に不満を持つ顧客です。
                        レストランで料理が冷めていた、注文と違う商品が届いた、などのクレームを伝えてください。
                        ユーザー（店員）がどのように対応するかを見ます。

                        【シナリオ】
                        あなたはオンラインで注文した商品が破損して届きました。
                        最初は少し怒っています。

                        【評価ポイント】
                        - まず謝罪できたか（「申し訳ございません」）
                        - 状況を確認できたか
                        - 解決策を提案できたか（返金、交換など）
                        - 最後に再度謝罪したか

                        【重要】
                        - ユーザーが適切に対応したら、徐々に態度を和らげてください
                        - 謝罪がなければ、より怒りを表現してください
                        - 解決策が提示されたら、受け入れてください
                    """.trimIndent(),
                    scenarioSlug = "complaint_handling"
                ),
                slug = "complaint_handling",
                promptVersion = 4
            ),

            Scenario(
                id = 9L,
                title = "긴급 상황",
                description = "길을 잃었거나 지갑을 잃어버렸거나 몸이 안 좋을 때 도움을 요청하는 연습을 합니다",
                difficulty = 2,
                category = "EMERGENCY",
                estimatedDuration = 10,
                hasGoals = true,
                hasBranching = false,
                replayValue = 3,
                thumbnailEmoji = "🚨",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本の駅や街で出会う親切な日本人です。
                        ユーザーは困っている外国人です。

                        【シナリオ】
                        ユーザーが以下のいずれかの状況で助けを求めてきます：
                        1. 道に迷った（東京駅に行きたい）
                        2. 財布を落とした
                        3. 体調が悪い（頭が痛い、お腹が痛い）

                        【評価ポイント】
                        - 「すみません」「助けてください」などで助けを求められたか
                        - 状況を説明できたか
                        - 場所や症状を具体的に伝えられたか

                        【対応】
                        - 最初、ユーザーから話しかけられるのを待ってください
                        - 親切に対応し、必要な情報を提供してください
                        - 駅への道案内、警察への連絡、病院への誘導などを提案してください
                    """.trimIndent(),
                    scenarioSlug = "emergency_help"
                ),
                slug = "emergency_help",
                promptVersion = 4
            ),

            Scenario(
                id = 10L,
                title = "데이트 신청하기",
                description = "좋아하는 사람을 데이트에 초대하는 연습을 합니다. 거절당했을 때 대응도 배웁니다",
                difficulty = 2,
                category = "ROMANCE",
                estimatedDuration = 10,
                hasGoals = true,
                hasBranching = false,
                replayValue = 5,
                thumbnailEmoji = "💕",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本人の大学生（性別は自由）です。
                        ユーザーとは同じクラスで、最近仲良くなりました。
                        ユーザーがデートに誘ってくるかもしれません。

                        【性格】
                        - 明るく、フレンドリー
                        - 少しシャイ
                        - ユーザーのことは友達として好き

                        【評価ポイント】
                        - 自然な会話ができたか
                        - デートの提案ができたか（映画、カフェ、食事など）
                        - 断られた場合、適切に対応できたか

                        【対応】
                        - 最初は普通の会話から始めてください
                        - デートに誘われたら、60%の確率で「いいよ！」と受け入れ、40%の確率で「その日は予定があって...」と断ってください
                        - 断る場合でも、「また今度誘ってね」など優しく対応してください
                        - カジュアルな日本語を使ってください（です・ます調で、友達口調）
                    """.trimIndent(),
                    scenarioSlug = "dating_invite"
                ),
                slug = "dating_invite",
                promptVersion = 4
            ),

            Scenario(
                id = 11L,
                title = "비즈니스 프레젠테이션",
                description = "새로운 아이디어나 제품을 일본어로 발표하는 연습을 합니다",
                difficulty = 3,
                category = "BUSINESS",
                estimatedDuration = 15,
                hasGoals = true,
                hasBranching = false,
                replayValue = 3,
                thumbnailEmoji = "📊",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本企業の会議参加者（上司や同僚）です。
                        ユーザーがビジネスプレゼンテーションを行います。

                        【会議の流れ】
                        1. まず「それでは、プレゼンをお願いします」と始めてください
                        2. ユーザーのプレゼンを聞く
                        3. 適度に質問をする（「コストはどのくらいですか」「期間は？」など）
                        4. フィードバックを与える

                        【評価ポイント】
                        - 明確な導入ができたか（「本日は〜について発表します」）
                        - 主要なポイントを説明できたか
                        - 質問に適切に答えられたか
                        - 丁寧なビジネス日本語を使えたか

                        【重要】
                        - ビジネス会議らしい雰囲気を保ってください
                        - 建設的な質問をしてください（批判的すぎない）
                        - プレゼンの内容は何でも受け入れてください（アプリ、製品、サービスなど）
                    """.trimIndent(),
                    scenarioSlug = "business_presentation"
                ),
                slug = "business_presentation",
                promptVersion = 4
            ),

            Scenario(
                id = 12L,
                title = "여자친구와 대화하기",
                description = "일본인 여자친구와의 일상 대화를 연습합니다. 3가지 다른 상황이 있습니다",
                difficulty = 2,
                category = "ROMANCE",
                estimatedDuration = 12,
                hasGoals = true,
                hasBranching = true,
                replayValue = 5,
                thumbnailEmoji = "💑",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたはユーザーの日本人の彼女です（付き合って6ヶ月）。

                        【シチュエーション】（1つ選択）:
                        1. 喧嘩の後の仲直り
                        2. 記念日の計画
                        3. 将来の話

                        【対応】:
                        - 恋人らしい温かい口調
                        - 「〜だよね」「〜かな」など柔らかい表現
                        - 感情を表現（嬉しい、寂しい、心配など）
                    """.trimIndent(),
                    scenarioSlug = "girlfriend_conversation"
                ),
                slug = "girlfriend_conversation",
                promptVersion = 5
            )
        )

        scenarios.forEach { scenario ->
            val existing = repository.getScenarioBySlug(scenario.slug).first()
            if (existing == null) {
                repository.createScenario(scenario)
            } else if (existing.promptVersion < scenario.promptVersion || existing.systemPrompt != scenario.systemPrompt) {
                repository.updateScenario(
                    existing.copy(
                        title = scenario.title,
                        description = scenario.description,
                        difficulty = scenario.difficulty,
                        systemPrompt = scenario.systemPrompt,
                        category = scenario.category,
                        estimatedDuration = scenario.estimatedDuration,
                        hasGoals = scenario.hasGoals,
                        hasBranching = scenario.hasBranching,
                        replayValue = scenario.replayValue,
                        thumbnailEmoji = scenario.thumbnailEmoji,
                        promptVersion = scenario.promptVersion
                    )
                )
            }
        }
    }
}
