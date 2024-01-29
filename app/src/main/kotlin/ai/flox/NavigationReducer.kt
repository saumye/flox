package ai.flox

import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.state.Action
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

class NavigationReducer: Reducer<AppState, Action> {

    lateinit var appNavController: NavController
    override fun reduce(state: AppState, action: Action): ReduceResult<AppState, Action> {
        return when (action) {
            is Action.Navigate -> {
                if(!::appNavController.isInitialized) {
                    Log.d("NavigationReducer", "NavController is not initialised")
                    return state.noEffect()
                }
                appNavController.navigate(action.route) {
                    popUpTo(appNavController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                state.noEffect()
            }
            else -> state.noEffect()
        }
    }
}