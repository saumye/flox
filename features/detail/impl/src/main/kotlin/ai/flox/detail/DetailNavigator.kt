package ai.flox.detail

import ai.flox.Navigable
import ai.flox.arch.Store
import ai.flox.detail.model.DetailState
import ai.flox.detail.ui.NewsDetailView
import ai.flox.state.Action
import ai.flox.state.State
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.StateFlow

class DetailNavigator(
    private val store: Store<State, Action>,
    private val state: StateFlow<DetailState>
) : Navigable {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController,
        modifier: Modifier
    ) {
        navGraphBuilder.composable(DetailRoutes.detail) {
            it.arguments?.getString("newsId")?.let {
                NewsDetailView(state, store)
            }
        }
    }
}