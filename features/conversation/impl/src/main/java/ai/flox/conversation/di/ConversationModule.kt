package ai.flox.conversation.di

import ai.flox.Navigable
import ai.flox.arch.Reducer
import ai.flox.conversation.ConversationListViewModel
import ai.flox.conversation.ConversationRoutes
import ai.flox.conversation.data.ConversationRepository
import ai.flox.conversation.model.ConversationAction
import ai.flox.conversation.model.ConversationState
import ai.flox.network.openai.OpenAIService
import ai.flox.state.State
import ai.flox.storage.conversation.ConversationDAO
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
class ConversationModule {

    @Singleton
    @Provides
    fun provideConversationRepository(
        openAIService: OpenAIService,
        conversationDao: ConversationDAO
    ): ConversationRepository {
        return ConversationRepository(conversationDao)
    }

    @Singleton
    @Provides
    @IntoMap
    @StringKey(ConversationState.stateKey)
    fun provideReducer(conversationRepo: ConversationRepository): Reducer<ConversationState, ConversationAction> {
        return ConversationListViewModel(conversationRepo)
    }

    @Singleton
    @Provides
    @IntoSet
    fun provideChatRoute(): Navigable = ConversationRoutes

    @Singleton
    @Provides
    @IntoMap
    @StringKey(ConversationState.stateKey)
    fun provideState(): State {
        return ConversationState()
    }

}