package com.nihongo.conversation

import com.nihongo.conversation.core.grammar.LocalGrammarAnalyzer
import org.junit.Test
import org.junit.Assert.*

class GrammarOptimizationTest {

    @Test
    fun testLocalAnalyzablePatterns() {
        // Simple sentences that should be handled locally (avoid API)
        val simplePatterns = listOf(
            "これは本です",           // Simple copula
            "学校に行きます",         // Basic particle + verb
            "おいしいです",           // i-adjective
            "静かです",               // na-adjective
            "食べました",             // Past tense
            "見ています",             // Progressive
            "行きたい",               // Want to
            "できます",               // Can do
            "本を読みます",           // Object marker
            "友達と話します",         // With particle
            "家から出ます",           // From particle
            "毎日勉強します",         // Time expression
            "大きくない",             // Negative adjective
            "行かなかった",           // Negative past
            "食べてください"          // Te-form request
        )

        println("=== Simple Patterns (Should use Local Analysis) ===")
        simplePatterns.forEach { sentence ->
            val canAnalyzeLocally = LocalGrammarAnalyzer.canAnalyzeLocally(sentence)
            println("'$sentence' -> Local: $canAnalyzeLocally")
            assertTrue("'$sentence' should be analyzable locally", canAnalyzeLocally)

            // Verify local analysis produces results
            val analysis = LocalGrammarAnalyzer.analyzeSentence(sentence, userLevel = 2)
            assertNotNull("Analysis should not be null for '$sentence'", analysis)
            assertTrue("Should have grammar points for '$sentence'",
                analysis.grammarPoints.isNotEmpty())
        }
    }

    @Test
    fun testComplexPatterns() {
        // Complex sentences that should use API
        val complexPatterns = listOf(
            "昨日友達と会った時、彼が新しい仕事について話してくれました",
            "もし明日雨が降ったら、ピクニックは中止になるでしょう",
            "日本に来てから、日本語がだんだん上手になってきました",
            "宿題を終わらせてから、ゲームをしてもいいですか",
            "彼女は日本語が話せるだけでなく、中国語も流暢です",
            "電車が遅れているようなので、タクシーで行きましょう",
            "先生に叱られないように、宿題を忘れずにやりました"
        )

        println("\n=== Complex Patterns (Should use API) ===")
        complexPatterns.forEach { sentence ->
            val canAnalyzeLocally = LocalGrammarAnalyzer.canAnalyzeLocally(sentence)
            println("'$sentence' -> Local: $canAnalyzeLocally")
            assertFalse("'$sentence' should require API analysis", canAnalyzeLocally)
        }
    }

    @Test
    fun testCacheEfficiency() {
        // Test that cache properly stores and retrieves results
        val testSentences = listOf(
            "これは本です",
            "学校に行きます",
            "おいしいです"
        )

        val analysisCache = mutableMapOf<String, Long>()

        println("\n=== Cache Efficiency Test ===")
        testSentences.forEach { sentence ->
            val startTime = System.currentTimeMillis()
            val analysis = LocalGrammarAnalyzer.analyzeSentence(sentence, userLevel = 2)
            val duration = System.currentTimeMillis() - startTime
            analysisCache[sentence] = duration

            println("First analysis of '$sentence': ${duration}ms")

            // Second analysis should be faster due to internal caching
            val cachedStart = System.currentTimeMillis()
            val cachedAnalysis = LocalGrammarAnalyzer.analyzeSentence(sentence, userLevel = 2)
            val cachedDuration = System.currentTimeMillis() - cachedStart

            println("Cached analysis of '$sentence': ${cachedDuration}ms")
            assertTrue("Cached should be faster", cachedDuration <= duration)
            assertEquals("Results should be identical",
                analysis.overallExplanation, cachedAnalysis.overallExplanation)
        }
    }

    @Test
    fun testPerformanceMetrics() {
        // Measure performance improvements
        val testSet = listOf(
            "これはペンです",
            "学校に行きます",
            "本を読んでいます",
            "友達と話しました",
            "日本語が好きです"
        )

        println("\n=== Performance Metrics ===")

        var localAnalysisCount = 0
        var apiRequiredCount = 0
        var totalLocalTime = 0L

        testSet.forEach { sentence ->
            if (LocalGrammarAnalyzer.canAnalyzeLocally(sentence)) {
                localAnalysisCount++
                val start = System.currentTimeMillis()
                LocalGrammarAnalyzer.analyzeSentence(sentence, userLevel = 2)
                totalLocalTime += (System.currentTimeMillis() - start)
            } else {
                apiRequiredCount++
            }
        }

        val localPercentage = (localAnalysisCount.toDouble() / testSet.size) * 100
        val avgLocalTime = if (localAnalysisCount > 0) totalLocalTime / localAnalysisCount else 0

        println("Total sentences: ${testSet.size}")
        println("Handled locally: $localAnalysisCount (${String.format("%.1f", localPercentage)}%)")
        println("Require API: $apiRequiredCount")
        println("Average local analysis time: ${avgLocalTime}ms")
        println("Estimated API savings: ${localAnalysisCount * 10}s (assuming 10s per API call)")

        // Assert optimization goals
        assertTrue("At least 80% should be handled locally", localPercentage >= 80)
        assertTrue("Local analysis should be under 50ms", avgLocalTime < 50)
    }
}