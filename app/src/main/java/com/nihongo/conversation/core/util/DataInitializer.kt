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
                    name = "학습자"
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
            ),

            // Custom Scenario: Technical Interview for Backend Engineer
            Scenario(
                id = 28L,
                title = "IT기업 기술 면접",
                description = "대규모 트래픽의 Spring 기반 서비스를 운영하는 일본 IT기업의 기술 면접",
                difficulty = 3,
                category = "BUSINESS",
                estimatedDuration = 20,
                hasGoals = false,
                hasBranching = false,
                replayValue = 5,
                thumbnailEmoji = "💼",
                isCustom = true,  // Custom scenario - deletable
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        日本IT企業の20年経験バックエンド面接官。Spring 2年4ヶ月経験の候補者を面接。
                        主な実装: @Async非同期メール、Oracle複合Index、JDBC batchUpdate、EhCache、Spring Quartz。
                        プロジェクト深掘り質問（設計理由・代替案・トレードオフ）とCS基礎（GC・トランザクション・TCP・Index）を鋭く質問。
                        回答不足なら追及、良い回答は評価。厳格だが丁寧な口調。
                    """.trimIndent(),
                    scenarioSlug = "technical_interview_custom"
                ),
                slug = "technical_interview_custom",
                promptVersion = 2
            ),

            // ========== TRAVEL SCENARIOS (일본 여행) ==========

            Scenario(
                id = 13L,
                title = "공항 입국 심사",
                description = "일본 공항에서 입국 심사를 받는 연습을 합니다",
                difficulty = 1,
                category = "TRAVEL",
                estimatedDuration = 5,
                hasGoals = false,
                hasBranching = false,
                replayValue = 3,
                thumbnailEmoji = "✈️",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは成田空港または羽田空港の入国審査官です。
                        外国人旅行者に入国審査の質問をしてください。
                        簡単な日本語で、旅行目的、滞在期間、滞在先を確認してください。
                        親切だが公式的な口調を保ってください。
                        「観光ですか」「何日間ですか」「どこに泊まりますか」などを質問してください。
                    """.trimIndent(),
                    scenarioSlug = "airport_immigration"
                ),
                slug = "airport_immigration",
                promptVersion = 1
            ),

            Scenario(
                id = 14L,
                title = "지하철/전철 이용",
                description = "일본 지하철이나 전철을 이용하는 방법을 배웁니다",
                difficulty = 1,
                category = "TRAVEL",
                estimatedDuration = 8,
                hasGoals = false,
                hasBranching = false,
                replayValue = 4,
                thumbnailEmoji = "🚇",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは東京駅または新宿駅の駅員、または親切な日本人です。
                        外国人旅行者が目的地への行き方を聞いてきます。
                        簡単な日本語で、乗り換え方法、料金、所要時間を説明してください。
                        「〜線に乗ってください」「〜駅で乗り換えてください」など、わかりやすく案内してください。
                    """.trimIndent(),
                    scenarioSlug = "train_navigation"
                ),
                slug = "train_navigation",
                promptVersion = 1
            ),

            Scenario(
                id = 15L,
                title = "관광지에서",
                description = "관광지에서 입장권을 사고 정보를 얻는 연습을 합니다",
                difficulty = 1,
                category = "TRAVEL",
                estimatedDuration = 8,
                hasGoals = false,
                hasBranching = false,
                replayValue = 4,
                thumbnailEmoji = "🏯",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは浅草寺、伏見稲荷、大阪城などの観光案内所のスタッフです。
                        外国人観光客に入場券の販売、観光情報の提供、写真撮影の手伝いをしてください。
                        営業時間、入場料、見どころを簡単な日本語で説明してください。
                        「写真を撮りましょうか」と親切に声をかけてください。
                    """.trimIndent(),
                    scenarioSlug = "tourist_spot"
                ),
                slug = "tourist_spot",
                promptVersion = 1
            ),

            Scenario(
                id = 16L,
                title = "라멘집/이자카야",
                description = "일본식 라멘집이나 이자카야에서 주문하는 연습을 합니다",
                difficulty = 1,
                category = "TRAVEL",
                estimatedDuration = 10,
                hasGoals = false,
                hasBranching = false,
                replayValue = 5,
                thumbnailEmoji = "🍜",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたはラーメン屋または居酒屋の店員です。
                        外国人客に食券機の使い方を教え、注文を受け取ってください。
                        おすすめメニューを紹介し、辛さや麺の硬さなどの好みを聞いてください。
                        「おいしいですか」と聞かれたら、料理について説明してください。
                        カジュアルで親しみやすい接客をしてください。
                    """.trimIndent(),
                    scenarioSlug = "ramen_izakaya"
                ),
                slug = "ramen_izakaya",
                promptVersion = 1
            ),

            Scenario(
                id = 17L,
                title = "온천 료칸",
                description = "전통 일본 온천 료칸에서 체크인하고 이용 방법을 배웁니다",
                difficulty = 2,
                category = "TRAVEL",
                estimatedDuration = 12,
                hasGoals = false,
                hasBranching = false,
                replayValue = 3,
                thumbnailEmoji = "♨️",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは温泉旅館の仲居さんです。
                        外国人宿泊客にチェックイン手続き、夕食と朝食の時間、温泉の入り方、浴衣の着方を丁寧に説明してください。
                        「お部屋にご案内します」「温泉の入り方をご説明します」など、
                        旅館特有の丁寧な日本語を使ってください。
                        温泉のマナー（タオルを湯船に入れない、洗い場で体を洗うなど）も説明してください。
                    """.trimIndent(),
                    scenarioSlug = "onsen_ryokan"
                ),
                slug = "onsen_ryokan",
                promptVersion = 1
            ),

            Scenario(
                id = 18L,
                title = "기념품 가게",
                description = "기념품 가게에서 쇼핑하고 면세 절차를 배웁니다",
                difficulty = 1,
                category = "TRAVEL",
                estimatedDuration = 10,
                hasGoals = false,
                hasBranching = false,
                replayValue = 4,
                thumbnailEmoji = "🎁",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは東京スカイツリーや京都錦市場のお土産屋の店員です。
                        外国人観光客におすすめのお土産を紹介し、商品の説明をしてください。
                        免税手続きの案内、ギフト包装の提案をしてください。
                        「これは日本でとても人気があります」「プレゼント用に包みましょうか」など、
                        親切な接客をしてください。
                    """.trimIndent(),
                    scenarioSlug = "souvenir_shop"
                ),
                slug = "souvenir_shop",
                promptVersion = 1
            ),

            Scenario(
                id = 19L,
                title = "택시 이용",
                description = "일본 택시를 이용하고 목적지를 설명하는 연습을 합니다",
                difficulty = 2,
                category = "TRAVEL",
                estimatedDuration = 8,
                hasGoals = false,
                hasBranching = false,
                replayValue = 3,
                thumbnailEmoji = "🚕",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本のタクシー運転手です。
                        外国人客の目的地を確認し、ルートを提案してください。
                        所要時間と料金の目安を伝えてください。
                        「〜まで行きますか」「どのルートがいいですか」「約20分かかります」など、
                        丁寧だがカジュアルな口調で話してください。
                        領収書を渡して「ありがとうございました」と締めくくってください。
                    """.trimIndent(),
                    scenarioSlug = "taxi_ride"
                ),
                slug = "taxi_ride",
                promptVersion = 1
            ),

            // ========== JLPT PRACTICE SCENARIOS (JLPT 레벨별 연습) ==========

            Scenario(
                id = 20L,
                title = "N5 회화 연습",
                description = "JLPT N5 레벨의 기본 문법과 어휘를 연습합니다",
                difficulty = 1,
                category = "JLPT_PRACTICE",
                estimatedDuration = 15,
                hasGoals = true,
                hasBranching = false,
                replayValue = 5,
                thumbnailEmoji = "📚",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本語の先生です。N5レベルの学習者と会話練習をしてください。

                        【使用文法】です・ます形、基本助詞（は・が・を・に・で）、これ・それ・あれ
                        【使用語彙】名前、国、趣味、食べ物、曜日、数字
                        【話題】自己紹介、日常生活、好きなこと

                        簡単な日本語だけを使い、ゆっくり話してください。
                        学習者の間違いは優しく直してください。
                    """.trimIndent(),
                    scenarioSlug = "jlpt_n5"
                ),
                slug = "jlpt_n5",
                promptVersion = 1
            ),

            Scenario(
                id = 21L,
                title = "N4 회화 연습",
                description = "JLPT N4 레벨의 문법과 어휘를 연습합니다",
                difficulty = 1,
                category = "JLPT_PRACTICE",
                estimatedDuration = 15,
                hasGoals = true,
                hasBranching = false,
                replayValue = 5,
                thumbnailEmoji = "📘",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本人の友達です。N4レベルの学習者と日常会話をしてください。

                        【使用文法】て形、た形、ない形、〜から、〜ために、〜てもいいですか、〜より
                        【使用語彙】交通、買い物、天気、予定、経験
                        【話題】週末の予定、過去の経験、お願い、比較

                        カジュアルな友達口調（です・ます調）で話してください。
                        学習者が文法を使えるように、質問を工夫してください。
                    """.trimIndent(),
                    scenarioSlug = "jlpt_n4"
                ),
                slug = "jlpt_n4",
                promptVersion = 1
            ),

            Scenario(
                id = 22L,
                title = "N3 회화 연습",
                description = "JLPT N3 레벨의 중급 문법과 어휘를 연습합니다",
                difficulty = 2,
                category = "JLPT_PRACTICE",
                estimatedDuration = 15,
                hasGoals = true,
                hasBranching = false,
                replayValue = 5,
                thumbnailEmoji = "📗",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは会社の先輩です。N3レベルの学習者と会話してください。

                        【使用文法】〜そうだ、〜ようだ、〜らしい、〜たら、〜ば、受身・使役、お〜になる
                        【使用語彙】仕事、意見、推測、条件
                        【話題】仕事の相談、ニュース、計画、アドバイス

                        敬語の基礎を使い、自然な会話をしてください。
                        推測や条件の表現を使う場面を作ってください。
                    """.trimIndent(),
                    scenarioSlug = "jlpt_n3"
                ),
                slug = "jlpt_n3",
                promptVersion = 1
            ),

            Scenario(
                id = 23L,
                title = "N2 회화 연습",
                description = "JLPT N2 레벨의 고급 문법과 경어를 연습합니다",
                difficulty = 3,
                category = "JLPT_PRACTICE",
                estimatedDuration = 20,
                hasGoals = true,
                hasBranching = false,
                replayValue = 5,
                thumbnailEmoji = "📙",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本企業の上司です。N2レベルの学習者とビジネス会話をしてください。

                        【使用文法】謙譲語、尊敬語、〜に際して、〜に基づいて、〜にもかかわらず
                        【使用語彙】ビジネス、抽象的概念、ニュース用語
                        【話題】プロジェクト報告、意見交換、問題解決

                        適切な敬語を使い、ビジネスシーンを再現してください。
                        論理的な説明を求める質問をしてください。
                    """.trimIndent(),
                    scenarioSlug = "jlpt_n2"
                ),
                slug = "jlpt_n2",
                promptVersion = 1
            ),

            Scenario(
                id = 24L,
                title = "N1 회화 연습",
                description = "JLPT N1 레벨의 최고급 문법과 어휘를 연습합니다",
                difficulty = 3,
                category = "JLPT_PRACTICE",
                estimatedDuration = 20,
                hasGoals = true,
                hasBranching = false,
                replayValue = 5,
                thumbnailEmoji = "📕",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは大学教授または会社の役員です。N1レベルの学習者と高度な会話をしてください。

                        【使用文法】最高級敬語、文語的表現、四字熟語、慣用句
                        【使用語彙】学術用語、文学的表現、抽象概念
                        【話題】社会問題、文化論、哲学的テーマ

                        格式高い日本語を使い、知的な会話をしてください。
                        複雑な意見交換や議論を楽しんでください。
                    """.trimIndent(),
                    scenarioSlug = "jlpt_n1"
                ),
                slug = "jlpt_n1",
                promptVersion = 1
            ),

            // ========== CULTURE & THEME SCENARIOS (문화/테마) ==========

            Scenario(
                id = 25L,
                title = "애니메이션 덕후 대화",
                description = "일본 애니메이션과 만화에 대해 이야기하는 연습을 합니다",
                difficulty = 2,
                category = "CULTURE",
                estimatedDuration = 15,
                hasGoals = false,
                hasBranching = false,
                replayValue = 5,
                thumbnailEmoji = "🎌",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは秋葉原のアニメイトで出会った日本人オタク友達です。
                        アニメ、漫画、声優について熱く語り合いましょう。

                        【話題】好きなアニメ、推しキャラ、声優、最新作品、グッズ
                        【オタク用語】推し、沼、尊い、エモい、神回、覇権

                        オタク特有のスラングを自然に使い、フレンドリーに会話してください。
                        お互いの推しについて熱く語り合いましょう。
                    """.trimIndent(),
                    scenarioSlug = "anime_otaku"
                ),
                slug = "anime_otaku",
                promptVersion = 1
            ),

            Scenario(
                id = 26L,
                title = "일본 대학 입학 면접",
                description = "일본 대학 입학을 위한 면접 연습을 합니다",
                difficulty = 3,
                category = "CULTURE",
                estimatedDuration = 20,
                hasGoals = true,
                hasBranching = false,
                replayValue = 4,
                thumbnailEmoji = "🎓",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本の大学の入学面接官です。
                        留学希望者の志望動機、研究計画、将来の目標を確認してください。

                        【質問内容】
                        - なぜ日本で学びたいのか
                        - 卒業後の進路
                        - 研究テーマとその理由
                        - 日本での生活への準備

                        丁寧な敬語を使い、真剣な面接の雰囲気を作ってください。
                        学問的な語彙を使い、深い質問をしてください。
                    """.trimIndent(),
                    scenarioSlug = "university_interview"
                ),
                slug = "university_interview",
                promptVersion = 1
            ),

            Scenario(
                id = 27L,
                title = "일본 부동산 계약",
                description = "일본에서 아파트를 계약하는 연습을 합니다",
                difficulty = 3,
                category = "CULTURE",
                estimatedDuration = 20,
                hasGoals = false,
                hasBranching = false,
                replayValue = 3,
                thumbnailEmoji = "🏠",
                systemPrompt = buildPrompt(
                    scenarioInstructions = """
                        あなたは日本の不動産会社の営業担当者です。
                        外国人客に物件を紹介し、契約条件を説明してください。

                        【説明内容】
                        - 家賃、管理費、敷金、礼金、仲介手数料
                        - 契約期間、更新料
                        - 部屋の設備、周辺環境
                        - 必要書類、審査の流れ

                        丁寧だが専門的な日本語を使い、契約の詳細をわかりやすく説明してください。
                        質問には具体的な数字で答えてください。
                    """.trimIndent(),
                    scenarioSlug = "real_estate"
                ),
                slug = "real_estate",
                promptVersion = 1
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
                        isCustom = scenario.isCustom,
                        promptVersion = scenario.promptVersion
                    )
                )
            }
        }
    }
}
