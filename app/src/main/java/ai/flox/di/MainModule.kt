package ai.flox.di

import ai.flox.AppState
import ai.flox.GlobalReducer
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
    fun provideReducer(): Reducer<*, *> {
        return GlobalReducer()
    }

    @Singleton
    @Provides
    fun provideStore(
        reducers: Map<String, @JvmSuppressWildcards Reducer<*, *>>,
        featureStates: Map<String, @JvmSuppressWildcards State>,
    ): Store<State, Action> {

//        val appReducer : Reducer<AppState, Action> = reducers[AppState.stateKey] as Reducer<AppState, Action>
//        val chatReducer: Reducer<ChatState, ChatAction> = reducers[ChatState.stateKey] as Reducer<ChatState, ChatAction>
//        val conversationReducer: Reducer<ConversationState, ConversationAction> =
//            reducers[ConversationState.stateKey] as Reducer<ConversationState, ConversationAction>
//        val reducersMap = PullbackReducer(
//                ForEachReducer(
//                    childReducer = chatReducer,
//                    parentReducer = appReducer,
//                    mapToChildAction = { action -> if (action is ChatAction) ChatState.stateKey to action else null },
//                    mapToParentAction = { chatAction, _ -> chatAction },
//                    mapToChildState = { appState, _ ->
//                        appState.featureStates[ChatState.stateKey]?.let {
//                            if (it is ChatState) it else ChatState()
//                        } ?: ChatState()
//                    },
//                    mapToParentState = { appState, chatState, _ ->
//                        appState.copy(
//                            featureStates = appState.featureStates.toMutableMap()
//                                .apply { this[ChatState.stateKey] = chatState })
//                    }
//                ),
//                ForEachReducer(
//                    childReducer = conversationReducer,
//                    parentReducer = appReducer,
//                    mapToChildAction = { action -> if (action is ConversationAction) ConversationState.stateKey to action else null },
//                    mapToParentAction = { conversationAction, _ -> conversationAction },
//                    mapToChildState = { appState, _ ->
//                        appState.featureStates[ConversationState.stateKey]?.let {
//                            if (it is ConversationState) it else ConversationState()
//                        } ?: ConversationState()
//                    },
//                    mapToParentState = { appState, conversationState, _ ->
//                        appState.copy(
//                            featureStates = appState.featureStates.toMutableMap()
//                                .apply { this[ConversationState.stateKey] = conversationState })
//                    }
//                )
//        )
//
//        val store = createStore(
//            initialState = AppState(featureStates),
//            reducer = reducersMap
//        )


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
