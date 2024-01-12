package ai.flox.state

import ai.flox.AppState
import ai.flox.arch.CompositeReducer
import ai.flox.arch.PullbackReducer
import ai.flox.arch.Reducer
import ai.flox.arch.Store
import ai.flox.arch.createStore
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatState
import ai.flox.conversation.model.ConversationAction
import ai.flox.conversation.model.ConversationState
import javax.inject.Inject

class ReducerManager @Inject constructor(
    reducers: Map<@JvmSuppressWildcards String, Reducer<State, Action>>,
    featureStates: Map<@JvmSuppressWildcards String, State>,
) {
    val store: Store<AppState, Action>
    val initialState = AppState(featureStates)

    init {
        store = createStore(
            initialState = initialState,
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
                                    .apply { put(ConversationState.stateKey, conversationState) })
                        },
                    )
                )
            )
        )
    }


}