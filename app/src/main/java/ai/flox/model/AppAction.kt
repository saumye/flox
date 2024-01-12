package ai.flox.model

import ai.flox.state.Action

sealed interface AppAction : Action {
    data class Navigate(val route: String) : Action.UI.NavigateEvent, AppAction {
        override val componentIdentifier = AppIds.BottomBarIcon
    }

    data class BottomBarClicked(val id: Int) : Action.UI.RenderEvent, AppAction {
        override val componentIdentifier = AppIds.BottomBarIcon
    }
}

object AppIds {
    const val BottomBarIcon = "BottomBarIcon"
}