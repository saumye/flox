package ai.flox

import ai.flox.chat.ChatRoutes
import ai.flox.conversation.ConversationRoutes
import ai.flox.state.State
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle

/**
 * App level state objects living on application scope.
 */
data class AppState(
    val featureStates: Map<String, State>,
    val applifecycle: Lifecycle.State = Lifecycle.State.INITIALIZED,
    val bottomBarState: BottomBarState = BottomBarState(),
    val userState: UserState = UserState.NoUser,
    val networkState: NetworkState = NetworkState.Offline
) : State {

    companion object {
        const val stateKey = "appState"
    }
}

enum class BottomTab(
    val title: String,
    @DrawableRes
    val icon: Int,
    val route: String
) {
    CHATS("Conversations", R.drawable.ic_launcher_foreground, ChatRoutes.chat),
    CONVERSATIONS("Chats", R.drawable.ic_launcher_background, ConversationRoutes.conversations)
}

sealed interface UserState {
    object NoUser: UserState
}

sealed interface NetworkState {
    object Offline: NetworkState
}

data class BottomBarState(
    val selectedBottomTab: BottomTab = BottomTab.CHATS,
    val bottomTabs: List<BottomTab> = BottomTab.entries.toList()
)