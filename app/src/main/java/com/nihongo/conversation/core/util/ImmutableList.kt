package com.nihongo.conversation.core.util

import androidx.compose.runtime.Immutable

/**
 * Immutable wrapper for List to prevent unnecessary recompositions in Compose
 * Compose treats this class as stable and won't recompose if the reference doesn't change
 */
@Immutable
@JvmInline
value class ImmutableList<T>(val items: List<T>) : List<T> by items {
    companion object {
        fun <T> of(vararg items: T): ImmutableList<T> = ImmutableList(items.toList())
        fun <T> empty(): ImmutableList<T> = ImmutableList(emptyList())
    }
}

/**
 * Convert List to ImmutableList with defensive copy
 * Creates a new list to prevent external mutations
 */
fun <T> List<T>.toImmutableList(): ImmutableList<T> = ImmutableList(this.toList())

/**
 * Immutable wrapper for Map to prevent unnecessary recompositions
 */
@Immutable
@JvmInline
value class ImmutableMap<K, V>(val items: Map<K, V>) : Map<K, V> by items {
    companion object {
        fun <K, V> empty(): ImmutableMap<K, V> = ImmutableMap(emptyMap())
    }
}

/**
 * Convert Map to ImmutableMap with defensive copy
 * Creates a new map to prevent external mutations
 */
fun <K, V> Map<K, V>.toImmutableMap(): ImmutableMap<K, V> = ImmutableMap(this.toMap())

/**
 * Immutable wrapper for Set to prevent unnecessary recompositions
 */
@Immutable
@JvmInline
value class ImmutableSet<T>(val items: Set<T>) : Set<T> by items {
    companion object {
        fun <T> empty(): ImmutableSet<T> = ImmutableSet(emptySet())
    }
}

/**
 * Convert Set to ImmutableSet with defensive copy
 * Creates a new set to prevent external mutations
 */
fun <T> Set<T>.toImmutableSet(): ImmutableSet<T> = ImmutableSet(this.toSet())
