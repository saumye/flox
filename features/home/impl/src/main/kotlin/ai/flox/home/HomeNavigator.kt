package ai.flox.home

import ai.flox.Navigable
import ai.flox.arch.Store
import ai.flox.home.model.HomeState
import ai.flox.home.ui.HomeScreen
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
class HomeNavigator(
    private val store: Store<State, Action>,
    private val state: StateFlow<HomeState>
) : Navigable {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController,
        modifier: Modifier
    ) {
        navGraphBuilder.composable(HomeRoutes.home) {
            HomeScreen(state, store)
        }
    }
}