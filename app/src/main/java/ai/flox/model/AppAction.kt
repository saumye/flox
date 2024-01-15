package ai.flox.model

import ai.flox.BottomTab
import ai.flox.state.Action
import androidx.navigation.NavController

sealed interface AppAction : Action {
    data class Navigate(val navController: NavController, val route: String) : Action.UI.NavigateEvent, AppAction {
        override val componentIdentifier = AppIds.BottomBarIcon
    }

    data class BottomBarClicked(val id: BottomTab, val navController: NavController) : Action.UI.RenderEvent, AppAction {
        override val componentIdentifier = AppIds.BottomBarIcon
    }
}

object AppIds {
    const val BottomBarIcon = "BottomBarIcon"
}