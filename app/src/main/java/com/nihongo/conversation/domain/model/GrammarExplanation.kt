package com.nihongo.conversation.domain.model

/**
 * Grammar component with explanation
 */
data class GrammarComponent(
    val text: String,           // The Japanese text (e.g., "を", "食べます")
    val type: GrammarType,      // Type of grammar pattern
    val explanation: String,    // Korean explanation
    val startIndex: Int,        // Start position in original text
    val endIndex: Int          // End position in original text
)

/**
 * Types of grammar patterns
 */
enum class GrammarType(val colorCode: String, val label: String) {
    PARTICLE("0xFF2196F3", "조사"),           // Blue
    VERB("0xFF4CAF50", "동사"),              // Green
    ADJECTIVE("0xFFFF9800", "형용사"),       // Orange
    NOUN("0xFF9C27B0", "명사"),              // Purple
    AUXILIARY("0xFFE91E63", "보조동사"),      // Pink
    CONJUNCTION("0xFF00BCD4", "접속사"),     // Cyan
    ADVERB("0xFFFFEB3B", "부사"),            // Yellow
    EXPRESSION("0xFF795548", "표현")         // Brown
}

/**
 * Complete grammar explanation for a message
 */
data class GrammarExplanation(
    val originalText: String,
    val components: List<GrammarComponent>,
    val overallExplanation: String,  // High-level explanation in Korean
    val detailedExplanation: String, // Detailed breakdown in Korean
    val examples: List<String>,      // Example sentences from conversation
    val relatedPatterns: List<String> // Related grammar patterns to learn
)
