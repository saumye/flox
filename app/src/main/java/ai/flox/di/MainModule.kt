package ai.flox.di

import ai.flox.AppState
import ai.flox.NavigationReducer
import ai.flox.arch.CompositeReducer
import ai.flox.arch.PullbackReducer
import ai.flox.arch.Reducer
import ai.flox.arch.Store
import ai.flox.arch.createStore
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatState
import ai.flox.conversation.model.ConversationAction
import ai.flox.conversation.model.ConversationState
import ai.flox.state.Action
import ai.flox.state.State
import android.content.Context
import androidx.navigation.NavHostController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    @Singleton
    @Provides
    fun provideNavController(@ApplicationContext context: Context): NavHostController =
        NavHostController(context)

    @Provides
    @IntoMap
    @StringKey(AppState.stateKey)
    fun provideReducer(navController: NavHostController): Reducer<*, *> {
        return NavigationReducer(navController)
    }

    @Singleton
    @Provides
    fun provideStore(
        reducers: Map<String, @JvmSuppressWildcards Reducer<*, *>>,
        featureStates: Map<String, @JvmSuppressWildcards State>,
    ): Store<State, Action> {
        val store: Store<AppState, Action> = createStore(
            initialState = AppState(featureStates),
            reducer = CompositeReducer(
                listOf(
                    PullbackReducer(
                        innerReducer = reducers[ChatState.stateKey] as Reducer<ChatState, ChatAction>,
                        mapToChildAction = { action -> action as ChatAction },
                        mapToChildState = { state -> state.featureStates[ChatState.stateKey] as ChatState },
                        mapToParentAction = { action -> action },
                        mapToParentState = { state, chatState ->
                            state.copy(
                                featureStates = featureStates.toMutableMap()
                                    .apply { put(ChatState.stateKey, chatState) })
                        },
                    ),
                    PullbackReducer(
                        innerReducer = reducers[ConversationState.stateKey] as Reducer<ConversationState, ConversationAction>,
                        mapToChildAction = { action -> action as ConversationAction },
                        mapToChildState = { state -> state.featureStates[ConversationState.stateKey] as ConversationState },
                        mapToParentAction = { action -> action },
                        mapToParentState = { state, conversationState ->
                            state.copy(
                                featureStates = featureStates.toMutableMap()
                                    .apply {
                                        put(
                                            ConversationState.stateKey,
                                            conversationState
                                        )
                                    })
                        },
                    )
                )
            )
        )
        return store as Store<State, Action>
    }

}
