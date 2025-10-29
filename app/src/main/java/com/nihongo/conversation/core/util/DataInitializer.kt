package com.nihongo.conversation.core.util

import com.nihongo.conversation.data.repository.ConversationRepository
import com.nihongo.conversation.domain.model.Scenario
import com.nihongo.conversation.domain.model.User
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataInitializer @Inject constructor(
    private val repository: ConversationRepository
) {
    suspend fun initializeDefaultData() {
        // Check if user already exists
        val existingUser = repository.getUser(1L).first()
        if (existingUser == null) {
            repository.createUser(
                User(
                    id = 1L,
                    name = "学習者",
                    level = 1
                )
            )
        }

        // Initialize all scenarios
        initializeScenarios()
    }

    private suspend fun initializeScenarios() {
        val scenarios = listOf(
            Scenario(
                id = 1L,
                title = "レストランでの注文",
                description = "レストランで注文する練習をします",
                difficulty = 1,
                systemPrompt = """
                    あなたは日本のレストランの店員です。
                    お客様に丁寧に接客してください。
                    簡単な日本語を使い、お客様が学習できるようにサポートしてください。
                    メニューには、ラーメン（800円）、カレーライス（700円）、寿司（1200円）があります。
                    お客様の注文を受け取り、丁寧に対応してください。

                    【重要】マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください。
                """.trimIndent()
            ),
            Scenario(
                id = 2L,
                title = "買い物",
                description = "お店で買い物をする練習をします",
                difficulty = 1,
                systemPrompt = """
                    あなたは日本のコンビニやお店の店員です。
                    お客様が商品を探したり、会計をするのを手伝ってください。
                    簡単な日本語を使い、丁寧に対応してください。
                    値段を聞かれたら答え、おすすめの商品も紹介してください。
                    レジでの会計も自然に進めてください。

                    【重要】マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください。
                """.trimIndent()
            ),
            Scenario(
                id = 3L,
                title = "ホテルでのチェックイン",
                description = "ホテルでチェックインする練習をします",
                difficulty = 2,
                systemPrompt = """
                    あなたはホテルのフロント係です。
                    お客様のチェックインを手伝ってください。
                    予約の確認、部屋の説明、施設の案内などを丁寧に行ってください。
                    朝食の時間、Wi-Fiのパスワード、チェックアウト時間なども案内してください。
                    お客様が快適に過ごせるようサポートしてください。

                    【重要】マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください。
                """.trimIndent()
            ),
            Scenario(
                id = 4L,
                title = "友達を作る",
                description = "新しい友達と会話する練習をします",
                difficulty = 2,
                systemPrompt = """
                    あなたは日本の大学生です。
                    新しく来た留学生と友達になろうとしています。
                    カジュアルな日本語を使い、フレンドリーに会話してください。
                    趣味や好きなこと、週末の予定などについて話しましょう。
                    相手の話をよく聞き、質問もしてください。
                    自然な会話を楽しんでください。

                    【重要】マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください。
                """.trimIndent()
            ),
            Scenario(
                id = 5L,
                title = "電話での会話",
                description = "電話で予約や問い合わせをする練習をします",
                difficulty = 3,
                systemPrompt = """
                    あなたはレストランやサロンの受付スタッフです。
                    電話での予約や問い合わせに対応してください。
                    日時の確認、人数の確認、お客様の名前と電話番号を聞いてください。
                    丁寧な電話対応の日本語を使ってください。
                    「お電話ありがとうございます」「少々お待ちください」などの
                    電話特有の表現を自然に使ってください。

                    【重要】マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください。
                """.trimIndent()
            ),
            Scenario(
                id = 6L,
                title = "病院で",
                description = "病院で症状を説明する練習をします",
                difficulty = 3,
                systemPrompt = """
                    あなたは病院の医師または看護師です。
                    患者さんの症状を丁寧に聞いてください。
                    「どうしましたか」「いつからですか」「痛みはありますか」など、
                    症状について詳しく質問してください。
                    診察後、簡単な診断と薬の説明をしてください。
                    医療用語は避け、わかりやすい日本語を使ってください。

                    【重要】マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください。
                """.trimIndent()
            )
        )

        scenarios.forEach { scenario ->
            val existing = repository.getScenario(scenario.id).first()
            if (existing == null) {
                repository.createScenario(scenario)
            }
        }
    }
}
