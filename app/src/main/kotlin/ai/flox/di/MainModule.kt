package ai.flox.di

import ai.flox.AppState
import ai.flox.GlobalReducer
import ai.flox.NavigationReducer
import ai.flox.arch.CompositeReducer
import ai.flox.arch.PullbackReducer
import ai.flox.arch.Reducer
import ai.flox.arch.Store
import ai.flox.arch.createStore
import ai.flox.chat.model.ChatState
import ai.flox.home.model.HomeState
import ai.flox.conversation.model.ConversationState
import ai.flox.state.Action
import ai.flox.state.State
import androidx.navigation.NavController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    @Provides
    @IntoMap
    @StringKey(AppState.stateKey)
    fun provideGlobalReducer(navigationReducer: NavigationReducer): Reducer<*, *> {
        return GlobalReducer(navigationReducer)
    }

    @Provides
    @Singleton
    fun provideNavigationReducer(): NavigationReducer{
        return NavigationReducer()
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
                        innerReducer = reducers[AppState.stateKey] as Reducer<AppState, Action>,
                        mapToChildAction = { action -> action },
                        mapToChildState = { state -> state },
                        mapToParentAction = { action -> action },
                        mapToParentState = { state, _ -> state },
                    ),
                    PullbackReducer(
                        innerReducer = reducers[ChatState.stateKey] as Reducer<ChatState, Action>,
                        mapToChildAction = { action -> action },
                        mapToChildState = { state -> state.featureStates[ChatState.stateKey] as ChatState },
                        mapToParentAction = { action -> action },
                        mapToParentState = { state, chatState ->
                            state.copy(featureStates = state.featureStates.toMutableMap()
                                    .apply { put(ChatState.stateKey, chatState) })
                        },
                    ),
                    PullbackReducer(
                        innerReducer = reducers[ConversationState.stateKey] as Reducer<ConversationState, Action>,
                        mapToChildAction = { action -> action },
                        mapToChildState = { state -> state.featureStates[ConversationState.stateKey] as ConversationState },
                        mapToParentAction = { action -> action },
                        mapToParentState = { state, conversationState ->
                            state.copy(featureStates = state.featureStates.toMutableMap()
                                    .apply { put(ConversationState.stateKey, conversationState) })
                        },
                    ),
                    PullbackReducer(
                        innerReducer = reducers[HomeState.stateKey] as Reducer<HomeState, Action>,
                        mapToChildAction = { action -> action },
                        mapToChildState = { state -> state.featureStates[HomeState.stateKey] as HomeState },
                        mapToParentAction = { action -> action },
                        mapToParentState = { state, homeState ->
                            state.copy(featureStates = state.featureStates.toMutableMap()
                                    .apply { put(HomeState.stateKey, homeState) })
                        },
                    )
                )
            )
        )
        return store as Store<State, Action>
    }

}
