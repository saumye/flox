package ai.flox.home.model

import ai.flox.state.State

data class HomeState(
    val recentNewsList: Map<String, NewsItem>? = null,
    val categorizedNews: Map<NewsCategory, Map<String, NewsItem>> = emptyMap(),
    val topStories: Map<String, NewsItem>? = null,
    val categories: List<NewsCategory> = NewsCategory.entries,
    val selectedCategory: NewsCategory = NewsCategory.TOP,
) : State {

    companion object {
        const val stateKey = "homeState"
    }
}