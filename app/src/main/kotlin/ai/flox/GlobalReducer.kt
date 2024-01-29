package ai.flox

import ai.flox.arch.Pure
import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.arch.withFlowEffect
import ai.flox.model.AppAction
import ai.flox.model.AppIds
import ai.flox.state.Action
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GlobalReducer @Inject constructor(
    private val navigationReducer: NavigationReducer
) : Reducer<AppState, Action> {

    @Pure
    override fun reduce(state: AppState, action: Action): ReduceResult<AppState, Action> {
        return when (action) {
            is Action.Navigate -> return navigationReducer.reduce(state, action)
            is AppAction.BottomBarClicked -> {
                val selectedTab = action.id
                state.copy(
                    bottomBarState = state.bottomBarState.copy(
                        selectedBottomTab = action.id
                    )
                ).withFlowEffect(
                    flow {
                        emit(Action.Navigate(selectedTab.route))
                    }
                )
            }
            else -> state.noEffect()
        }
    }
}