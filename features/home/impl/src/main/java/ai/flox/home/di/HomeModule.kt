package ai.flox.home.di

import ai.flox.arch.Reducer
import ai.flox.home.HomeScreenViewModel
import ai.flox.home.data.NewsRepository
import ai.flox.home.model.HomeState
import ai.flox.network.newsapi.NewsAPI
import ai.flox.network.newsapi.NewsService
import ai.flox.network.openai.OpenAIService
import ai.flox.state.State
import ai.flox.storage.chat.ChatDAO
import ai.flox.storage.news.NewsDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeModule {

    @Singleton
    @Provides
    fun provideChatRepository(
        newsAPI: NewsService,
        newsDAO: NewsDAO
    ): NewsRepository = NewsRepository(newsAPI, newsDAO)

    @Provides
    @IntoMap
    @StringKey(HomeState.stateKey)
    fun provideReducer(newsRepository: NewsRepository): Reducer<*, *> {
        return HomeScreenViewModel(newsRepository)
    }

    @Provides
    @IntoMap
    @StringKey(HomeState.stateKey)
    fun provideState(): State {
        return HomeState()
    }
}