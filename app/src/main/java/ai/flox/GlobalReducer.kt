package ai.flox

import ai.flox.arch.Pure
import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.arch.withFlowEffect
import ai.flox.model.AppAction
import ai.flox.state.Action
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.flow.flow

class GlobalReducer : Reducer<AppState, Action> {
    @Pure
    override fun reduce(state: AppState, action: Action): ReduceResult<AppState, Action> {
        if(action !is AppAction) {
            return state.noEffect()
        }
        return when (action) {
            is AppAction.BottomBarClicked -> {
                val selectedTab = action.id
                state.copy(
                    bottomBarState = state.bottomBarState.copy(
                        selectedBottomTab = action.id
                    )
                ).withFlowEffect(
                    flow {
                        emit(AppAction.Navigate(selectedTab.route, action.navController))
                    }
                )
            }

            is AppAction.Navigate -> {
                action.navController.navigate(action.route) {
                    popUpTo(action.navController.graph.findStartDestination().id) {
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