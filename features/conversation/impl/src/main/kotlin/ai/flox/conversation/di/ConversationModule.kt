package ai.flox.conversation.di

import ai.flox.arch.Reducer
import ai.flox.conversation.ConversationListViewModel
import ai.flox.conversation.data.ConversationRepository
import ai.flox.conversation.model.ConversationState
import ai.flox.state.State
import ai.flox.storage.conversation.ConversationDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConversationModule {

    @Singleton
    @Provides
    fun provideConversationRepository(
        conversationDao: ConversationDAO
    ): ConversationRepository {
        return ConversationRepository(conversationDao)
    }

    @Provides
    @IntoMap
    @StringKey(ConversationState.stateKey)
    fun provideReducer(conversationRepo: ConversationRepository): Reducer<*, *> {
        return ConversationListViewModel(conversationRepo)
    }

    @Provides
    @IntoMap
    @StringKey(ConversationState.stateKey)
    fun provideState(): State {
        return ConversationState()
    }

}