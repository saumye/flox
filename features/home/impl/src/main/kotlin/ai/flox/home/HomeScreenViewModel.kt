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
import ai.flox.tts.TTS
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    @ApplicationContext context: Context
) : ViewModel(), Reducer<HomeState, Action> {

    private val tts: TTS = TTS(context)

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
            
            is HomeAction.SelectCategory -> {
                val newState = state.copy(selectedCategory = action.category)
                newState.withFlowEffect(
                    merge(
                        newsRepository.refreshNewsByCategory(action.category),
                        newsRepository.getNewsByCategory(action.category)
                    )
                )
            }

            is HomeAction.StartSpeak -> {
                CoroutineScope(Dispatchers.IO).launch {
                    tts.stop()
                    tts.generate(action.text)
                    tts.generatedAudio
                        .collect { audioChunk ->
                            speakText(audioChunk)//AdvancedModeAction.UpdateVisualisation(audioChunk, USER_ID_AI))
                        }
                }
                state.noEffect()
            }
            
            is HomeAction.LoadTopStories -> {
                state.withFlowEffect(newsRepository.getTopStories())
            }

            is HomeAction.LoadArticles -> {
                if (action.resource is Resource.Success) {
                    val res = action.resource as Resource.Success<List<NewsItem>>
                    state.copy(recentNewsList = res.data.associateBy { msg -> msg.id }).noEffect()
                } else {
                    state.noEffect()
                }
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
            
            is HomeAction.LoadTopStoriesArticles -> {
                if (action.resource is Resource.Success) {
                    val res = action.resource as Resource.Success<List<NewsItem>>
                    state.copy(topStories = res.data.associateBy { it.id }).noEffect()
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

    private fun speakText(audioData: FloatArray) {
        // Create and configure AudioTrack
        val sampleRate = 24000 // Use the actual sample rate of your audio
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(audioFormat)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        // Start playback and visualization
        audioTrack.play()

        // Write data to AudioTrack
        audioTrack.write(audioData, 0, audioData.size, AudioTrack.WRITE_BLOCKING)

        // Set a marker at the end of the playback
        audioTrack.setNotificationMarkerPosition(audioData.size)
    }
}