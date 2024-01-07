package ai.flox.chat.di

import ai.flox.chat.data.ChatRepository
import ai.flox.chat.model.ChatState
import ai.flox.network.openai.OpenAIService
import ai.flox.storage.chat.ChatDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    fun provideChatState() : ChatState {
        return ChatState()
    }

}