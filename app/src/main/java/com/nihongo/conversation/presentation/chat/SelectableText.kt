package com.nihongo.conversation.presentation.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Selectable text component that allows word selection and dictionary lookup
 */
@Composable
fun SelectableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    textAlign: TextAlign = TextAlign.Start,
    onWordSelected: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    SelectionContainer {
        Text(
            text = text,
            modifier = modifier.pointerInput(text) {
                detectTapGestures(
                    onLongPress = { offset ->
                        textLayoutResult?.let { layoutResult ->
                            val position = layoutResult.getOffsetForPosition(offset)
                            val selectedWord = extractWordAtPosition(text, position)
                            if (selectedWord.isNotEmpty()) {
                                onWordSelected?.invoke(selectedWord)
                                    ?: openJishoDictionary(context, selectedWord)
                            }
                        }
                    }
                )
            },
            style = style,
            textAlign = textAlign,
            onTextLayout = { textLayoutResult = it }
        )
    }
}

/**
 * Extract a word at the given position in the text
 * Supports Japanese characters (Hiragana, Katakana, Kanji)
 */
private fun extractWordAtPosition(text: String, position: Int): String {
    if (position !in text.indices) return ""

    // Japanese word boundary characters
    val wordBoundaries = setOf(
        ' ', '\n', '\t', '、', '。', '！', '？', '（', '）', '「', '」',
        '『', '』', '【', '】', '・', '…', ',', '.', '!', '?', '(', ')'
    )

    // Find start of word
    var start = position
    while (start > 0 && text[start - 1] !in wordBoundaries) {
        start--
    }

    // Find end of word
    var end = position
    while (end < text.length && text[end] !in wordBoundaries) {
        end++
    }

    return text.substring(start, end).trim()
}

/**
 * Open Jisho.org dictionary with the selected word
 */
fun openJishoDictionary(context: Context, word: String) {
    val url = "https://jisho.org/search/${Uri.encode(word)}"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    // Check if browser is available
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}
