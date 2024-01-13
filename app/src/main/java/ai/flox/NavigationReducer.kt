package ai.flox

import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.arch.withFlowEffect
import ai.flox.model.AppAction
import ai.flox.state.Action
import ai.flox.state.State
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NavigationReducer @Inject constructor(
    private val navController: NavHostController
) : Reducer<AppState, AppAction> {
    override fun reduce(state: AppState, action: AppAction): ReduceResult<AppState, AppAction> {
        return when (action) {
            is AppAction.BottomBarClicked -> {
                val selectedTab = state.bottomBarState.bottomTabs[action.id]
                state.copy(
                    bottomBarState = state.bottomBarState.copy(
                        selectedBottomTab = selectedTab
                    )
                ).withFlowEffect(
                    flow {
                        emit(AppAction.Navigate(selectedTab.route))
                    }
                )
            }

            is AppAction.Navigate -> {
                navController.navigate(action.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                state.noEffect()
            }
        }
    }
}