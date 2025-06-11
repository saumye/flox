package ai.flox.detail.model

import ai.flox.home.model.NewsItem
import ai.flox.state.State

data class DetailState(
    val currentArticle: NewsItem? = null,
    val isBookmarked: Boolean = false,
    val relatedArticles: List<NewsItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) : State {
    companion object {
        const val stateKey = "detail"
    }
} 