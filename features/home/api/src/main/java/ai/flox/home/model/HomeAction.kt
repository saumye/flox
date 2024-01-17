package ai.flox.home.model

import ai.flox.state.Action
import ai.flox.state.Resource

sealed interface HomeAction : Action {
    data object RecentNewsRendered : Action.UI.RenderEvent, HomeAction {
        override val componentIdentifier = HomeIds.RecentNews
    }

    //Chat
    data class CreateOrUpdateArticles(override val resource: Resource<NewsItem>) :
        Action.Data.LoadData<NewsItem>, HomeAction

    data class LoadArticles(override val resource: Resource<NewsItem>) :
        Action.Data.LoadData<NewsItem>, HomeAction

    data class UpdateArticles(override val resource: Resource<NewsItem>) :
        Action.Data.UpdateData<NewsItem>, HomeAction

    data class DeleteArticles(override val resource: Resource<NewsItem>) :
        Action.Data.LoadData<NewsItem>, HomeAction
}

object HomeIds {
    const val RecentNews = "RecentNews"
}