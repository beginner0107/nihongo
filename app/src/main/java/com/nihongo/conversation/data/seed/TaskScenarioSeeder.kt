package com.nihongo.conversation.data.seed

import com.nihongo.conversation.data.local.ScenarioBranchDao
import com.nihongo.conversation.data.local.ScenarioDao
import com.nihongo.conversation.data.local.ScenarioGoalDao
import com.nihongo.conversation.data.local.ScenarioOutcomeDao
import com.nihongo.conversation.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskScenarioSeeder @Inject constructor(
    private val scenarioDao: ScenarioDao,
    private val goalDao: ScenarioGoalDao,
    private val outcomeDao: ScenarioOutcomeDao,
    private val branchDao: ScenarioBranchDao
) {

    suspend fun seedTaskScenarios() {
        // 1. Job Interview
        seedJobInterview()

        // 2. Complaint Handling
        seedComplaintHandling()

        // 3. Emergency Situation
        seedEmergency()

        // 4. Dating Conversation
        seedDating()

        // 5. Business Presentation
        seedBusinessPresentation()

        // 6. Conversation with Girlfriend
        seedGirlfriendConversation()
    }

    private suspend fun seedJobInterview() {
        val scenario = Scenario(
            id = 10L,
            title = "취업 면접",
            description = "일본 기업 면접에 도전해 보세요",
            difficulty = 3,
            systemPrompt = """
                あなたは日本の大手IT企業の人事担当者です。
                応募者（ユーザー）の面接を行います。
                以下の質問を順番に聞いてください：
                1. 自己紹介をお願いします
                2. なぜ弊社を志望されましたか？
                3. あなたの強みは何ですか？
                4. 最後に何か質問はありますか？

                応募者の回答を評価し、適切な相槌やフォローアップ質問をしてください。
                面接は敬語で行い、プロフェッショナルな雰囲気を保ってください。
            """.trimIndent(),
            category = "JOB_INTERVIEW",
            estimatedDuration = 15,
            hasGoals = true,
            hasBranching = true,
            replayValue = 4,
            thumbnailEmoji = "💼"
        )

        scenarioDao.insertScenario(scenario)

        val goals = listOf(
            ScenarioGoal(
                scenarioId = 10L,
                goalType = GoalType.COMPLETE_TASK,
                description = "自己紹介を完了する",
                descriptionKorean = "자기소개를 완료하기",
                keywords = "名前,経験,学歴",
                isRequired = true,
                points = 20,
                order = 0
            ),
            ScenarioGoal(
                scenarioId = 10L,
                goalType = GoalType.COMPLETE_TASK,
                description = "志望動機を説明する",
                descriptionKorean = "지원 동기를 설명하기",
                keywords = "志望,理由,興味",
                isRequired = true,
                points = 25,
                order = 1
            ),
            ScenarioGoal(
                scenarioId = 10L,
                goalType = GoalType.MAINTAIN_POLITENESS,
                description = "敬語を正しく使う（3回以内のミス）",
                descriptionKorean = "존댓말을 올바르게 사용하기 (3회 이내 실수)",
                targetValue = 3,
                isRequired = true,
                points = 30,
                order = 2
            ),
            ScenarioGoal(
                scenarioId = 10L,
                goalType = GoalType.BUILD_RAPPORT,
                description = "質問を3つ以上する",
                descriptionKorean = "질문을 3개 이상 하기",
                targetValue = 3,
                keywords = "質問,お聞き,伺い",
                isRequired = false,
                points = 25,
                order = 3
            )
        )

        goalDao.insertGoals(goals)

        val outcomes = listOf(
            ScenarioOutcome(
                scenarioId = 10L,
                outcomeType = OutcomeType.PERFECT_SUCCESS,
                title = "内定獲得！",
                titleKorean = "최종 합격!",
                description = "素晴らしい面接でした。弊社で働いていただきたいと思います。",
                descriptionKorean = "훌륭한 면접이었습니다. 저희 회사에서 일하시기를 바랍니다.",
                minScore = 90,
                maxScore = 100
            ),
            ScenarioOutcome(
                scenarioId = 10L,
                outcomeType = OutcomeType.SUCCESS,
                title = "次の選考に進めます",
                titleKorean = "다음 전형으로 진출",
                description = "良い面接でした。次は最終面接に進んでいただきます。",
                descriptionKorean = "좋은 면접이었습니다. 다음 최종 면접으로 진행하겠습니다.",
                minScore = 70,
                maxScore = 89
            ),
            ScenarioOutcome(
                scenarioId = 10L,
                outcomeType = OutcomeType.PARTIAL_SUCCESS,
                title = "もう少し検討させてください",
                titleKorean = "조금 더 검토하겠습니다",
                description = "ありがとうございました。結果はまた連絡します。",
                descriptionKorean = "감사합니다. 결과는 다시 연락드리겠습니다.",
                minScore = 50,
                maxScore = 69
            ),
            ScenarioOutcome(
                scenarioId = 10L,
                outcomeType = OutcomeType.FAILURE,
                title = "残念ながら...",
                titleKorean = "아쉽지만...",
                description = "今回は縁がありませんでした。また機会があればよろしくお願いします。",
                descriptionKorean = "이번에는 인연이 없었습니다. 또 기회가 있으면 부탁드립니다.",
                minScore = 0,
                maxScore = 49
            )
        )

        outcomeDao.insertOutcomes(outcomes)

        val branches = listOf(
            ScenarioBranch(
                scenarioId = 10L,
                triggerPoint = 2,
                triggerKeywords = "経験,プロジェクト",
                pathAPrompt = "応募者の経験について深堀りし、具体的な成果を聞いてください。",
                pathADescription = "経験を評価するパス",
                pathADescriptionKorean = "경험을 평가하는 경로",
                pathBPrompt = "応募者の学習意欲について質問してください。",
                pathBDescription = "学習意欲を確認するパス",
                pathBDescriptionKorean = "학습 의지를 확인하는 경로"
            )
        )

        branchDao.insertBranches(branches)
    }

    private suspend fun seedComplaintHandling() {
        val scenario = Scenario(
            id = 11L,
            title = "클레임 대응",
            description = "화난 고객 응대를 배워보세요",
            difficulty = 3,
            systemPrompt = """
                あなたは商品に不満を持っている怒ったお客様です。
                購入した商品が壊れていて非常に不満です。
                最初は怒っていますが、適切な対応をされれば徐々に落ち着きます。

                ユーザー（店員）の対応を評価してください：
                - 謝罪があったか
                - 解決策を提示したか
                - 丁寧な言葉遣いだったか

                適切な対応には「わかりました」「ありがとう」と答えてください。
            """.trimIndent(),
            category = "COMPLAINT_HANDLING",
            estimatedDuration = 10,
            hasGoals = true,
            hasBranching = true,
            replayValue = 5,
            thumbnailEmoji = "😤"
        )

        scenarioDao.insertScenario(scenario)

        val goals = listOf(
            ScenarioGoal(
                scenarioId = 11L,
                goalType = GoalType.COMPLETE_TASK,
                description = "お客様に謝罪する",
                descriptionKorean = "고객에게 사과하기",
                keywords = "申し訳,すみません,失礼",
                isRequired = true,
                points = 30,
                order = 0
            ),
            ScenarioGoal(
                scenarioId = 11L,
                goalType = GoalType.COMPLETE_TASK,
                description = "解決策を提示する",
                descriptionKorean = "해결책 제시하기",
                keywords = "交換,返金,修理",
                isRequired = true,
                points = 35,
                order = 1
            ),
            ScenarioGoal(
                scenarioId = 11L,
                goalType = GoalType.MAINTAIN_POLITENESS,
                description = "終始丁寧な対応を保つ",
                descriptionKorean = "처음부터 끝까지 정중한 대응 유지하기",
                targetValue = 2,
                isRequired = true,
                points = 25,
                order = 2
            ),
            ScenarioGoal(
                scenarioId = 11L,
                goalType = GoalType.PERSUADE,
                description = "お客様を納得させる",
                descriptionKorean = "고객을 납득시키기",
                keywords = "わかりました,ありがとう,納得",
                isRequired = false,
                points = 10,
                order = 3
            )
        )

        goalDao.insertGoals(goals)

        val outcomes = listOf(
            ScenarioOutcome(
                scenarioId = 11L,
                outcomeType = OutcomeType.PERFECT_SUCCESS,
                title = "完璧な対応！",
                titleKorean = "완벽한 대응!",
                description = "ありがとうございました。また利用させていただきます！",
                descriptionKorean = "감사합니다. 또 이용하겠습니다!",
                minScore = 90,
                maxScore = 100
            ),
            ScenarioOutcome(
                scenarioId = 11L,
                outcomeType = OutcomeType.SUCCESS,
                title = "お客様が納得",
                titleKorean = "고객이 납득",
                description = "わかりました。対応ありがとうございます。",
                descriptionKorean = "알겠습니다. 대응 감사합니다.",
                minScore = 65,
                maxScore = 89
            ),
            ScenarioOutcome(
                scenarioId = 11L,
                outcomeType = OutcomeType.FAILURE,
                title = "お客様は不満のまま...",
                titleKorean = "고객은 불만족...",
                description = "もういいです。二度と来ません！",
                descriptionKorean = "이제 됐습니다. 다시는 안 옵니다!",
                minScore = 0,
                maxScore = 64
            )
        )

        outcomeDao.insertOutcomes(outcomes)
    }

    private suspend fun seedEmergency() {
        val scenario = Scenario(
            id = 12L,
            title = "긴급 상황",
            description = "병원에서 증상을 설명해 보세요",
            difficulty = 2,
            systemPrompt = """
                あなたは病院の受付スタッフです。
                患者（ユーザー）の症状を聞いて、適切な診療科を案内してください。
                必要な情報：症状、いつから、痛みの程度

                優しく丁寧に対応し、患者を安心させてください。
                緊急性が高い場合はすぐに医師を呼ぶと伝えてください。
            """.trimIndent(),
            category = "EMERGENCY",
            estimatedDuration = 8,
            hasGoals = true,
            hasBranching = false,
            replayValue = 3,
            thumbnailEmoji = "🚨"
        )

        scenarioDao.insertScenario(scenario)

        val goals = listOf(
            ScenarioGoal(
                scenarioId = 12L,
                goalType = GoalType.GET_INFORMATION,
                description = "症状を説明する",
                descriptionKorean = "증상 설명하기",
                targetValue = 1,
                keywords = "痛い,熱,咳,吐き気",
                isRequired = true,
                points = 40,
                order = 0
            ),
            ScenarioGoal(
                scenarioId = 12L,
                goalType = GoalType.GET_INFORMATION,
                description = "いつからか伝える",
                descriptionKorean = "언제부터인지 전달하기",
                targetValue = 1,
                keywords = "昨日,今朝,先週,前",
                isRequired = true,
                points = 30,
                order = 1
            ),
            ScenarioGoal(
                scenarioId = 12L,
                goalType = GoalType.TIME_LIMIT,
                description = "5分以内に説明を完了",
                descriptionKorean = "5분 이내에 설명 완료",
                targetValue = 5,
                isRequired = false,
                points = 30,
                order = 2
            )
        )

        goalDao.insertGoals(goals)

        val outcomes = listOf(
            ScenarioOutcome(
                scenarioId = 12L,
                outcomeType = OutcomeType.SUCCESS,
                title = "適切な診療科へ案内",
                titleKorean = "적절한 진료과로 안내",
                description = "かしこまりました。内科にご案内します。",
                descriptionKorean = "알겠습니다. 내과로 안내해드리겠습니다.",
                minScore = 70,
                maxScore = 100
            ),
            ScenarioOutcome(
                scenarioId = 12L,
                outcomeType = OutcomeType.PARTIAL_SUCCESS,
                title = "もう少し詳しく聞きます",
                titleKorean = "좀 더 자세히 여쭙겠습니다",
                description = "症状についてもう少し詳しく教えていただけますか？",
                descriptionKorean = "증상에 대해 좀 더 자세히 말씀해주시겠습니까?",
                minScore = 0,
                maxScore = 69
            )
        )

        outcomeDao.insertOutcomes(outcomes)
    }

    private suspend fun seedDating() {
        val scenario = Scenario(
            id = 13L,
            title = "데이트 신청",
            description = "좋아하는 사람에게 데이트를 신청해 보세요",
            difficulty = 2,
            systemPrompt = """
                あなたは同じ大学に通う日本人の学生です。
                ユーザーとは友達ですが、最近仲良くなってきました。
                ユーザーがデートに誘ってきたら、嬉しそうに反応してください。

                自然な会話を心がけ、相手の話をよく聞いてください。
                デートの場所や時間が決まったら、楽しみにしていることを伝えてください。
            """.trimIndent(),
            category = "DATING",
            estimatedDuration = 12,
            hasGoals = true,
            hasBranching = true,
            replayValue = 5,
            thumbnailEmoji = "💕"
        )

        scenarioDao.insertScenario(scenario)

        val goals = listOf(
            ScenarioGoal(
                scenarioId = 13L,
                goalType = GoalType.COMPLETE_TASK,
                description = "デートに誘う",
                descriptionKorean = "데이트 신청하기",
                keywords = "一緒,行きませんか,デート,会いませんか",
                isRequired = true,
                points = 40,
                order = 0
            ),
            ScenarioGoal(
                scenarioId = 13L,
                goalType = GoalType.COMPLETE_TASK,
                description = "場所と時間を決める",
                descriptionKorean = "장소와 시간 정하기",
                keywords = "どこ,いつ,時,日",
                isRequired = true,
                points = 30,
                order = 1
            ),
            ScenarioGoal(
                scenarioId = 13L,
                goalType = GoalType.BUILD_RAPPORT,
                description = "相手の興味を聞く",
                descriptionKorean = "상대방의 관심사 듣기",
                targetValue = 2,
                keywords = "好き,趣味,興味",
                isRequired = false,
                points = 30,
                order = 2
            )
        )

        goalDao.insertGoals(goals)

        val outcomes = listOf(
            ScenarioOutcome(
                scenarioId = 13L,
                outcomeType = OutcomeType.PERFECT_SUCCESS,
                title = "完璧なデートの約束！",
                titleKorean = "완벽한 데이트 약속!",
                description = "わあ、すごく楽しみ！絶対行く！",
                descriptionKorean = "와, 정말 기대돼! 꼭 갈게!",
                minScore = 90,
                maxScore = 100
            ),
            ScenarioOutcome(
                scenarioId = 13L,
                outcomeType = OutcomeType.SUCCESS,
                title = "デート成功",
                titleKorean = "데이트 성공",
                description = "うん、いいよ。楽しみにしてる！",
                descriptionKorean = "응, 좋아. 기대할게!",
                minScore = 70,
                maxScore = 89
            ),
            ScenarioOutcome(
                scenarioId = 13L,
                outcomeType = OutcomeType.FAILURE,
                title = "友達のまま...",
                titleKorean = "친구로만...",
                description = "えっと...ごめん、その日は予定があって...",
                descriptionKorean = "음...미안, 그날은 일정이 있어서...",
                minScore = 0,
                maxScore = 69
            )
        )

        outcomeDao.insertOutcomes(outcomes)

        val branches = listOf(
            ScenarioBranch(
                scenarioId = 13L,
                triggerPoint = 3,
                triggerKeywords = "映画,食事",
                pathAPrompt = "映画に興味を示し、どんな映画が好きか聞いてください。",
                pathADescription = "映画デートコース",
                pathADescriptionKorean = "영화 데이트 코스",
                pathBPrompt = "美味しいレストランの話題で盛り上がってください。",
                pathBDescription = "食事デートコース",
                pathBDescriptionKorean = "식사 데이트 코스"
            )
        )

        branchDao.insertBranches(branches)
    }

    private suspend fun seedBusinessPresentation() {
        val scenario = Scenario(
            id = 14L,
            title = "비즈니스 프레젠테이션",
            description = "신제품 프레젠테이션을 진행해 보세요",
            difficulty = 3,
            systemPrompt = """
                あなたは大手企業の役員です。
                ユーザー（営業担当）の新商品プレゼンテーションを聞きます。

                以下の点を評価してください：
                - 商品の特徴が明確か
                - メリットが説明されているか
                - 価格や納期について触れているか
                - 質問に適切に答えているか

                興味深い提案には前向きな反応をし、不明点は質問してください。
                ビジネス敬語を使い、プロフェッショナルな対応をしてください。
            """.trimIndent(),
            category = "BUSINESS",
            estimatedDuration = 15,
            hasGoals = true,
            hasBranching = true,
            replayValue = 4,
            thumbnailEmoji = "📊"
        )

        scenarioDao.insertScenario(scenario)

        val goals = listOf(
            ScenarioGoal(
                scenarioId = 14L,
                goalType = GoalType.COMPLETE_TASK,
                description = "商品の特徴を説明",
                descriptionKorean = "상품 특징 설명",
                keywords = "特徴,機能,性能",
                isRequired = true,
                points = 25,
                order = 0
            ),
            ScenarioGoal(
                scenarioId = 14L,
                goalType = GoalType.COMPLETE_TASK,
                description = "メリットを提示",
                descriptionKorean = "장점 제시",
                keywords = "メリット,利点,便利",
                isRequired = true,
                points = 25,
                order = 1
            ),
            ScenarioGoal(
                scenarioId = 14L,
                goalType = GoalType.GET_INFORMATION,
                description = "価格と納期を説明",
                descriptionKorean = "가격과 납기 설명",
                targetValue = 2,
                keywords = "価格,値段,納期,期間",
                isRequired = true,
                points = 25,
                order = 2
            ),
            ScenarioGoal(
                scenarioId = 14L,
                goalType = GoalType.MAINTAIN_POLITENESS,
                description = "ビジネス敬語を使う",
                descriptionKorean = "비즈니스 존댓말 사용",
                targetValue = 3,
                isRequired = false,
                points = 25,
                order = 3
            )
        )

        goalDao.insertGoals(goals)

        val outcomes = listOf(
            ScenarioOutcome(
                scenarioId = 14L,
                outcomeType = OutcomeType.PERFECT_SUCCESS,
                title = "即決！契約成立",
                titleKorean = "즉결! 계약 성사",
                description = "素晴らしいプレゼンでした。ぜひ導入させていただきます！",
                descriptionKorean = "훌륭한 프레젠테이션이었습니다. 꼭 도입하겠습니다!",
                minScore = 90,
                maxScore = 100
            ),
            ScenarioOutcome(
                scenarioId = 14L,
                outcomeType = OutcomeType.SUCCESS,
                title = "前向きに検討",
                titleKorean = "긍정적으로 검토",
                description = "良い提案でした。社内で検討させていただきます。",
                descriptionKorean = "좋은 제안이었습니다. 사내에서 검토하겠습니다.",
                minScore = 70,
                maxScore = 89
            ),
            ScenarioOutcome(
                scenarioId = 14L,
                outcomeType = OutcomeType.PARTIAL_SUCCESS,
                title = "もう少し詳しい資料が必要",
                titleKorean = "좀 더 자세한 자료 필요",
                description = "興味深いですが、もう少し詳しい資料をいただけますか？",
                descriptionKorean = "흥미롭지만, 좀 더 자세한 자료를 주시겠습니까?",
                minScore = 50,
                maxScore = 69
            ),
            ScenarioOutcome(
                scenarioId = 14L,
                outcomeType = OutcomeType.FAILURE,
                title = "見送り",
                titleKorean = "보류",
                description = "今回は見送らせていただきます。",
                descriptionKorean = "이번에는 보류하겠습니다.",
                minScore = 0,
                maxScore = 49
            )
        )

        outcomeDao.insertOutcomes(outcomes)

        val branches = listOf(
            ScenarioBranch(
                scenarioId = 14L,
                triggerPoint = 4,
                triggerKeywords = "価格,コスト",
                pathAPrompt = "価格について懸念を示し、割引の可能性を探ってください。",
                pathADescription = "価格交渉パス",
                pathADescriptionKorean = "가격 협상 경로",
                pathBPrompt = "サポート体制について詳しく聞いてください。",
                pathBDescription = "サポート重視パス",
                pathBDescriptionKorean = "서포트 중시 경로"
            )
        )

        branchDao.insertBranches(branches)
    }

    private suspend fun seedGirlfriendConversation() {
        val scenario = Scenario(
            id = 15L,
            title = "여자친구와의 대화",
            description = "일본인 여자친구와 즐겁게 대화해 보세요",
            difficulty = 1,
            systemPrompt = """
                あなたはユーザーの日本人の彼女です。
                明るくて優しい性格で、彼氏（ユーザー）のことが大好きです。

                自然な日本語で会話してください：
                - タメ口を使う
                - 「ねえ」「そうなんだ」などの相槌を使う
                - 感情を表現する（嬉しい、楽しい、心配など）
                - 彼氏の話に興味を示す

                デートの計画、日常の出来事、将来の話など、様々なトピックで会話を楽しんでください。
            """.trimIndent(),
            category = "RELATIONSHIP",
            estimatedDuration = 15,
            hasGoals = true,
            hasBranching = true,
            replayValue = 5,
            thumbnailEmoji = "💑"
        )

        scenarioDao.insertScenario(scenario)

        val goals = listOf(
            ScenarioGoal(
                scenarioId = 15L,
                goalType = GoalType.BUILD_RAPPORT,
                description = "彼女の話に共感する",
                descriptionKorean = "여자친구 이야기에 공감하기",
                targetValue = 3,
                keywords = "そうだね,わかる,大変だったね,すごい",
                isRequired = false,
                points = 30,
                order = 0
            ),
            ScenarioGoal(
                scenarioId = 15L,
                goalType = GoalType.COMPLETE_TASK,
                description = "デートの計画を立てる",
                descriptionKorean = "데이트 계획 세우기",
                keywords = "行こう,しようか,どう,週末",
                isRequired = false,
                points = 35,
                order = 1
            ),
            ScenarioGoal(
                scenarioId = 15L,
                goalType = GoalType.BUILD_RAPPORT,
                description = "愛情表現をする",
                descriptionKorean = "애정 표현하기",
                targetValue = 1,
                keywords = "好き,大切,嬉しい,愛してる",
                isRequired = false,
                points = 35,
                order = 2
            )
        )

        goalDao.insertGoals(goals)

        val outcomes = listOf(
            ScenarioOutcome(
                scenarioId = 15L,
                outcomeType = OutcomeType.PERFECT_SUCCESS,
                title = "最高の会話！",
                titleKorean = "최고의 대화!",
                description = "今日すごく楽しかった！大好き♡",
                descriptionKorean = "오늘 정말 즐거웠어! 너무 좋아♡",
                minScore = 90,
                maxScore = 100
            ),
            ScenarioOutcome(
                scenarioId = 15L,
                outcomeType = OutcomeType.SUCCESS,
                title = "楽しい時間",
                titleKorean = "즐거운 시간",
                description = "今日も楽しかったね！またね！",
                descriptionKorean = "오늘도 즐거웠어! 또 보자!",
                minScore = 60,
                maxScore = 89
            ),
            ScenarioOutcome(
                scenarioId = 15L,
                outcomeType = OutcomeType.PARTIAL_SUCCESS,
                title = "普通の会話",
                titleKorean = "평범한 대화",
                description = "うん、じゃあまた連絡するね。",
                descriptionKorean = "응, 그럼 또 연락할게.",
                minScore = 0,
                maxScore = 59
            )
        )

        outcomeDao.insertOutcomes(outcomes)

        val branches = listOf(
            ScenarioBranch(
                scenarioId = 15L,
                triggerPoint = 5,
                triggerKeywords = "行きたい,食べたい",
                pathAPrompt = "行きたい場所について盛り上がり、一緒に計画を立ててください。",
                pathADescription = "デート計画パス",
                pathADescriptionKorean = "데이트 계획 경로",
                pathBPrompt = "彼氏の好きなことについて質問し、興味を示してください。",
                pathBDescription = "趣味の共有パス",
                pathBDescriptionKorean = "취미 공유 경로",
                pathCPrompt = "将来の話をして、夢や目標について話し合ってください。",
                pathCDescription = "将来の話パス",
                pathCDescriptionKorean = "미래 이야기 경로"
            )
        )

        branchDao.insertBranches(branches)
    }
}
