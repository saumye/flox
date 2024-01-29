package ai.flox.home

import ai.flox.arch.Pure
import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.arch.withFlowEffect
import ai.flox.home.data.NewsRepository
import ai.flox.home.model.HomeAction
import ai.flox.home.model.HomeState
import ai.flox.home.model.NewsItem
import ai.flox.state.Action
import ai.flox.state.Resource
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel(), Reducer<HomeState, Action> {

    @Pure
    @Synchronized
    override fun reduce(state: HomeState, action: Action): ReduceResult<HomeState, Action> {
        if (action !is HomeAction) {
            return state.noEffect()
        }
        return when (action) {

            is HomeAction.RecentNewsRendered -> {
                state.withFlowEffect(merge(newsRepository.refreshNews(), newsRepository.getNews()))
            }

            is HomeAction.LoadArticles -> {
                if (action.resource is Resource.Success) {
                    val res = action.resource as Resource.Success<List<NewsItem>>
                    state.copy(recentNewsList = res.data.associateBy { msg -> msg.id }).noEffect()
                } else {
                    state.noEffect()
                }
            }

            is HomeAction.CreateOrUpdateArticles -> {
                if (action.resource is Resource.Success) {
                    val res = action.resource as Resource.Success<List<NewsItem>>
                    state.recentNewsList?.let {
                        val map = it.toMutableMap()
                        for (message in res.data) {
                            map[message.id] = message
                        }
                        state.copy(recentNewsList = map.toMap()).noEffect()
                    } ?: state.noEffect()
                } else {
                    state.noEffect()
                }
            }

            else -> state.noEffect()
        }
    }
}