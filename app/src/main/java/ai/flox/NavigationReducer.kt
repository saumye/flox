package ai.flox

import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.arch.withFlowEffect
import ai.flox.model.AppAction
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NavigationReducer @Inject constructor(
    val navController: NavController
) : Reducer<AppState, AppAction> {
    override fun reduce(state: AppState, action: AppAction): ReduceResult<AppState, AppAction> {
        return when (action) {
            is AppAction.BottomBarClicked -> {
                val tab = state.bottomTabs[action.id]
                state.copy(selectedBottomTab = tab).withFlowEffect(
                    flow {
                        emit(AppAction.Navigate(tab.route))
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