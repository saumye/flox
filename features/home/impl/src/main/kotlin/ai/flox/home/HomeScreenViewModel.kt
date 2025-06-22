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
import ai.flox.home.service.AudioPlaybackService
import ai.flox.state.Action
import ai.flox.state.Resource
import ai.flox.tts.TTS
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel(), Reducer<HomeState, Action> {

    @Pure
    @Synchronized
    override fun reduce(state: HomeState, action: Action): ReduceResult<HomeState, Action> {
        if (action !is HomeAction) {
            return state.noEffect()
        }
        return when (action) {
            
            is HomeAction.SelectCategory -> {
                val newState = state.copy(selectedCategory = action.category)
                newState.withFlowEffect(newsRepository.getNewsByCategory(action.category))
            }
            
            is HomeAction.LoadCategoryArticles -> {
                if (action.resource is Resource.Success) {
                    val res = action.resource as Resource.Success<List<NewsItem>>
                    val category = action.category
                    val currentCategorizedNews = state.categorizedNews.toMutableMap()
                    currentCategorizedNews[category] = res.data.associateBy { it.id }
                    state.copy(categorizedNews = currentCategorizedNews).noEffect()
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

            is HomeAction.PlayStoryAudio -> {
                val intent = Intent(context, AudioPlaybackService::class.java).apply {
                    this.action = AudioPlaybackService.ACTION_PLAY_STORY
                    putExtra(AudioPlaybackService.EXTRA_NEWS_ID, action.newsId)
                }
                context.startService(intent)
                state.noEffect()
            }

            is HomeAction.StopStoryAudio -> {
                val intent = Intent(context, AudioPlaybackService::class.java).apply {
                    this.action = AudioPlaybackService.ACTION_STOP_STORY
                }
                context.startService(intent)
                state.noEffect()
            }
            
            is HomeAction.StartBackgroundAudio -> {
                val intent = Intent(context, AudioPlaybackService::class.java).apply {
                    this.action = AudioPlaybackService.ACTION_START_BACKGROUND
                }
                context.startService(intent)
                state.noEffect()
            }
            
            is HomeAction.StopBackgroundAudio -> {
                val intent = Intent(context, AudioPlaybackService::class.java).apply {
                    this.action = AudioPlaybackService.ACTION_STOP_BACKGROUND
                }
                context.startService(intent)
                state.noEffect()
            }

            else -> {state.noEffect()}
        }
    }
}