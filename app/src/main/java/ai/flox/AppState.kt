package ai.flox

import ai.flox.chat.model.ChatMessage
import ai.flox.chat.model.ChatState
import ai.flox.chat.model.Conversation
import ai.flox.state.State
import androidx.lifecycle.Lifecycle

data class AppState(
    val applifecycle: Lifecycle.State = Lifecycle.State.INITIALIZED,
    val chatState: ChatState = ChatState(),
    val conversations: Map<String, Conversation> = emptyMap(),
    val messages: Map<String, ChatMessage> = emptyMap(),
    val userState: ai.flox.UserState = ai.flox.UserState.NoUser,
    val networkState: ai.flox.NetworkState = ai.flox.NetworkState.Offline
) : State


sealed interface UserState {
    object NoUser: ai.flox.UserState
    object LoggedIn: ai.flox.UserState
    object SignInError: ai.flox.UserState
}

sealed interface NetworkState {
    object Connected: ai.flox.NetworkState
    object Offline: ai.flox.NetworkState
}