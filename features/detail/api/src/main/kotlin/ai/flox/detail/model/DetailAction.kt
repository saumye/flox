package ai.flox.detail.model

import ai.flox.home.model.NewsItem
import ai.flox.state.Action

sealed class DetailAction : Action {
    data class LoadArticle(val newsItem: NewsItem) : DetailAction()
    data object BookmarkArticle : DetailAction()
    data object ShareArticle : DetailAction()
    data object BackToHome : DetailAction()
} 