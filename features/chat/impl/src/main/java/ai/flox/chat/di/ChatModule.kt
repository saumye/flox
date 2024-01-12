package ai.flox.chat.di

import ai.flox.Navigable
import ai.flox.arch.Reducer
import ai.flox.arch.Store
import ai.flox.chat.ChatListViewModel
import ai.flox.chat.ChatRoutes
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
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ChatModule {

    @Singleton
    @Provides
    fun provideChatRepository(
        openAIService: OpenAIService,
        chatDao: ChatDAO
    ): ChatRepository {
        return ChatRepository(openAIService, chatDao)
    }

    @Singleton
    @Provides
    @IntoMap
    @StringKey(ChatState.stateKey)
    fun provideReducer(chatRepository: ChatRepository): Reducer<ChatState, ChatAction> {
        return ChatListViewModel(chatRepository)
    }

    @Singleton
    @Provides
    @IntoMap
    @StringKey(ChatState.stateKey)
    fun provideState(): State {
        return ChatState()
    }

    @Provides
    @IntoSet
    fun provideChatRoutes(store: Store<State, Action>): Navigable = ChatRoutes(store)

}