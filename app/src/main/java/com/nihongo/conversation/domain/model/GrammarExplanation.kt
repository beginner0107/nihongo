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
 *
 * Kuromoji Integration (2025-11-02): Expanded from 8 to 12 types for accurate morphological analysis
 * Color codes optimized for accessibility (darker tones, colorblind-friendly)
 */
enum class GrammarType(val colorCode: String, val label: String) {
    // Core types (improved colors for accessibility)
    PARTICLE("0xFF1976D2", "조사"),           // Darker Blue (was #2196F3)
    VERB("0xFF388E3C", "동사"),              // Darker Green (was #4CAF50)
    ADJECTIVE("0xFFF57C00", "형용사"),       // Darker Orange (was #FF9800)
    NOUN("0xFF7B1FA2", "명사"),              // Darker Purple (was #9C27B0)
    AUXILIARY("0xFFC2185B", "조동사"),        // Darker Pink (was #E91E63)
    ADVERB("0xFFF9A825", "부사"),            // Darker Yellow (was #FFEB3B) - improved readability

    // New types for Kuromoji detailed POS tagging
    PREFIX("0xFF3F51B5", "접두사"),          // Indigo
    INTERJECTION("0xFFFF5722", "감탄사"),    // Deep Orange
    SYMBOL("0xFF616161", "기호"),            // Dark Grey (was #9E9E9E)
    RENTAISHI("0xFF00897B", "연체사"),       // Teal (pre-noun adjectival)
    CONJUNCTION("0xFF0097A7", "접속사"),     // Darker Cyan (was #00BCD4)

    // Fallback
    EXPRESSION("0xFF6D4C41", "표현")         // Darker Brown (was #795548)
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
