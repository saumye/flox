package ai.flox.network

/**
 * Result of [INativeApiNetworkCall].
 */
sealed class NetworkResource<T> {

    /**
     * The network call succeeded.
     */
    data class Success<T>(val data: T?, val errorMessage: String?) : NetworkResource<T>()

    /**
     * The network call failed.
     */
    data class Failure(val error: NetworkException) : NetworkResource<Nothing>()
}

/**
 * An exception raised by NativeApi networking code.
 */
class NetworkException(
    val reason: Reason,
    val rawStatusCode: String?,
    override val cause: Throwable? = null
) : Exception(cause) {

    /**
     * Reason of failure.
     */
    enum class Reason {
        UNKNOWN,
        BLOCKED_BY_RESILIENCY_GATE,
        MAX_RETRY_EXCEEDED,
        NETWORK_UNAVAILABLE,
        SOCKET_TIMEOUT,
        CANCELLED,
        TOKEN_FETCH_FAILURE,
        IO_EXCEPTION,
        USER_SIGNING_OUT_OR_SIGNED_OUT,
        AUTH_ERROR
    }

    companion object {
        fun fromException(e: Exception): NetworkException {
            return NetworkException(Reason.UNKNOWN, e.javaClass.simpleName, e)
        }
    }
}