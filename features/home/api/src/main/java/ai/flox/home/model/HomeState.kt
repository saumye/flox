package ai.flox.home.model

import ai.flox.state.State

data class HomeState(
    val recentNewsList: Map<String, NewsItem>? = null,
) : State {

    companion object {
        const val stateKey = "homeState"
    }
}