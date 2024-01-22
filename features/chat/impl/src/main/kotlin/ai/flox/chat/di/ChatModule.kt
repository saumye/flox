package ai.flox.chat.di

import ai.flox.arch.Reducer
import ai.flox.chat.ChatListViewModel
import ai.flox.chat.data.ChatRepository
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatState
import ai.flox.network.openai.OpenAIService
import ai.flox.state.Action
import ai.flox.state.State
import ai.flox.storage.chat.ChatDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Singleton
    @Provides
    fun provideChatRepository(
        openAIService: OpenAIService,
        chatDao: ChatDAO
    ): ChatRepository = ChatRepository(openAIService, chatDao)

    @Provides
    @IntoMap
    @StringKey(ChatState.stateKey)
    fun provideReducer(chatRepository: ChatRepository): Reducer<*, *> {
        return ChatListViewModel(chatRepository)
    }

    @Provides
    @IntoMap
    @StringKey(ChatState.stateKey)
    fun provideState(): State {
        return ChatState()
    }
}