package com.wallora.app.data.remote.api

/** Sealed result wrapper for network calls, mirroring [kotlin.Result] semantics. */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val code: Int, val message: String) : NetworkResult<Nothing>()
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>()

    val isSuccess get() = this is Success
    fun getOrNull(): T? = if (this is Success) data else null
    fun errorMessage(): String = when (this) {
        is Success -> ""
        is Error -> "HTTP $code: $message"
        is Exception -> throwable.localizedMessage ?: throwable.toString()
    }
}

/** Execute a suspend API call and wrap the result safely. */
suspend fun <T> safeApiCall(block: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (e: retrofit2.HttpException) {
        NetworkResult.Error(e.code(), e.message())
    } catch (e: java.io.IOException) {
        NetworkResult.Exception(e)
    } catch (e: kotlinx.serialization.SerializationException) {
        NetworkResult.Exception(e)
    } catch (e: Throwable) {
        NetworkResult.Exception(e)
    }
}
