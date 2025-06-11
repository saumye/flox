package ai.flox.home.model

import ai.flox.state.Action
import ai.flox.state.ComponentIdentifier
import ai.flox.state.Resource

sealed interface HomeAction : Action {
    data object RecentNewsRendered : Action.UI.RenderEvent, HomeAction {
        override val componentIdentifier = HomeIds.RecentNews
    }

    data class SelectCategory(val category: NewsCategory) : Action.UI.RenderEvent, HomeAction {
        override val componentIdentifier = HomeIds.RecentNews
    }

    data object LoadTopStories : Action.UI.RenderEvent, HomeAction {
        override val componentIdentifier = HomeIds.TopStories
    }

    data class StartSpeak(val text: String) : Action.UI.InputEvent,
        HomeAction {
        override val componentIdentifier: ComponentIdentifier = HomeIds.TopStories
    }

    // Articles/News
    data class CreateOrUpdateArticles(override val resource: Resource<List<NewsItem>>) :
        Action.Data.LoadData<List<NewsItem>>, HomeAction

    data class LoadArticles(override val resource: Resource<List<NewsItem>>) :
        Action.Data.LoadData<List<NewsItem>>, HomeAction

    data class LoadCategoryArticles(val category: NewsCategory, override val resource: Resource<List<NewsItem>>) :
        Action.Data.LoadData<List<NewsItem>>, HomeAction

    data class LoadTopStoriesArticles(override val resource: Resource<List<NewsItem>>) :
        Action.Data.LoadData<List<NewsItem>>, HomeAction

    data class UpdateArticles(override val resource: Resource<NewsItem>) :
        Action.Data.UpdateData<NewsItem>, HomeAction

    data class DeleteArticles(override val resource: Resource<NewsItem>) :
        Action.Data.LoadData<NewsItem>, HomeAction
}

object HomeIds {
    const val RecentNews = "RecentNews"
    const val TopStories = "TopStories"
}