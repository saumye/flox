package ai.flox.state

sealed interface StateObject<T> {
    data object Loading : StateObject<Any>

    data class Success<T>(
        val chats: T
    ) : StateObject<T>

    data class Error<T>(
        val message: String
    ) : StateObject<Nothing>
}