package ai.flox.advanced

import ai.flox.Navigable
import ai.flox.advanced.model.AdvancedModeState
import ai.flox.advanced.ui.AdvancedModeScreen
import ai.flox.arch.Store
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
class AdvancedModeNavigator(
    private val store: Store<State, Action>,
    private val state: StateFlow<AdvancedModeState>
) : Navigable {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController,
        modifier: Modifier
    ) {
        navGraphBuilder.composable(AdvancedModeRoutes.conversation) {
            it.arguments?.getString("conversationId")?.let {
                AdvancedModeScreen(state, store, it)
            }
        }

    }
}