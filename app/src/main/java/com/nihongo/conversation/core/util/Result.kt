package com.nihongo.conversation.core.util

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

/**
 * Transform the data if this is a Success, otherwise return the original Error/Loading
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
        is Result.Loading -> this
    }
}

/**
 * Get the data if Success, otherwise return null
 */
fun <T> Result<T>.getOrNull(): T? {
    return when (this) {
        is Result.Success -> data
        is Result.Error -> null
        is Result.Loading -> null
    }
}

/**
 * Get the exception if Error, otherwise return null
 */
fun <T> Result<T>.exceptionOrNull(): Exception? {
    return when (this) {
        is Result.Success -> null
        is Result.Error -> exception
        is Result.Loading -> null
    }
}

/**
 * Fold the result into a single value
 * @param onSuccess Called with data if Success
 * @param onError Called with exception if Error
 * @param onLoading Called if Loading
 */
inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onError: (Exception) -> R,
    onLoading: () -> R
): R {
    return when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onError(exception)
        is Result.Loading -> onLoading()
    }
}
