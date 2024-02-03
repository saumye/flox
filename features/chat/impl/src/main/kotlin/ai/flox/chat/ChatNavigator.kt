package ai.flox.chat

import ai.flox.Navigable
import ai.flox.arch.Store
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatState
import ai.flox.chat.ui.ChatListScreen
import ai.flox.state.Action
import ai.flox.state.State
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.StateFlow

/**
 * Configures all navigation routes to Composables in chats
 */
class ChatNavigator(
    private val store: Store<State, Action>,
    private val state: StateFlow<ChatState>
) : Navigable {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController,
        modifier: Modifier
    ) {
        navGraphBuilder.composable(ChatRoutes.chat) {
            it.arguments?.getString("conversationId")?.let {
                ChatListScreen(state, store, it)
            }
        }

    }
}