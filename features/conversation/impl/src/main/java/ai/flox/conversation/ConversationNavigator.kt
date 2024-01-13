package ai.flox.conversation

import ai.flox.Navigable
import ai.flox.arch.Store
import ai.flox.conversation.model.ConversationState
import ai.flox.conversation.ui.ConversationListScreen
import ai.flox.state.Action
import ai.flox.state.State
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.StateFlow

/**
 * Configures all navigation routes to Composables in conversation
 */
class ConversationNavigator(
    private val state: StateFlow<ConversationState>
) : Navigable {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController,
        modifier: Modifier
    ) {
        navGraphBuilder.composable(ConversationRoutes.conversations) {
            ConversationListScreen(state)
        }
    }
}