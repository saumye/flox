package ai.flox

import ai.flox.state.State
import ai.flox.state.StateKey
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
    //override val stateKey: StateKey = "appState"

    companion object {
        const val stateKey = "appState"
    }
}

enum class BottomTabs(
    val title: String,
    @DrawableRes
    val icon: Int,
    val route: String
) {
    HOME("Home", R.drawable.ic_launcher_foreground, "home"),
    CHATS("Chats", R.drawable.ic_launcher_background, "conversations")
}

sealed interface UserState {
    object NoUser: UserState
    object LoggedIn: UserState
    object SignInError: UserState
}

sealed interface NetworkState {
    object Connected: NetworkState
    object Offline: NetworkState
}

data class BottomBarState(
    val selectedBottomTab: BottomTabs = BottomTabs.CHATS,
    val bottomTabs: List<BottomTabs> = BottomTabs.values().toList()
)