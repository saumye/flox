package ai.flox.home.model

import ai.flox.state.Action
import ai.flox.state.ComponentIdentifier
import ai.flox.state.Resource

sealed interface HomeAction : Action {

    data class SelectCategory(val category: NewsCategory) : Action.UI.RenderEvent, HomeAction {
        override val componentIdentifier = HomeIds.RecentNews
    }

    // Articles/News
    data class CreateOrUpdateArticles(override val resource: Resource<List<NewsItem>>) :
        Action.Data.LoadData<List<NewsItem>>, HomeAction

    data class LoadCategoryArticles(val category: NewsCategory, override val resource: Resource<List<NewsItem>>) :
        Action.Data.LoadData<List<NewsItem>>, HomeAction

    data class DeleteArticles(override val resource: Resource<NewsItem>) :
        Action.Data.LoadData<NewsItem>, HomeAction

    object StartBackgroundAudio : HomeAction
    object StopBackgroundAudio : HomeAction
    data class PlayStoryAudio(val newsId: String) : HomeAction
    data class StopStoryAudio(val newsId: String? = null) : HomeAction

}

object HomeIds {
    const val RecentNews = "RecentNews"
    const val TopStories = "TopStories"
}