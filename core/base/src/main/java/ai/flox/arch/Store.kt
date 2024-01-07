package ai.flox.arch

import ai.flox.state.Action
import ai.flox.state.State
import kotlinx.coroutines.flow.Flow

/**
 * A Store holds a reducer and does all orchestration for ensuring that actions are properly
 * forwarded, state is correctly mutated and effects are properly executed.
 * You can create one using the createStore function.
 */
interface Store<S : State, A : Action> {

    /**
     * A flow that emits whenever the state of the application changes.
     */
    val state: Flow<S>

    /**
     * Sends actions to be processed by the internal reducer.
     * If multiple actions are sent, the state will still only emit
     * once all actions are processed in a batch.
     * @see state
     */
    fun dispatch(action: List<A>)

    /**
     * Sends actions to be processed by the internal reducer.
     * If multiple actions are sent, the state will still only emit
     * once all actions are processed in a batch.
     * @see state
     */
    fun dispatch(action: A) = dispatch(listOf(action))

    /**
     * Creates a view of the store, which will only emit state changes that are relevant to the view.
     * @param mapToLocalState       A function that maps the global state to the local state of the view.
     * @param mapToGlobalAction     A function that maps the local actions of the view to global actions.
     * @return A store that only emits state changes relevant to the view.
     */
    fun <ViewState : S, ViewAction : A> view(
        mapToLocalState: (State) -> ViewState,
        mapToGlobalAction: (ViewAction) -> A?
    ): Store<ViewState, ViewAction>
}

/**
 * Creates a store that can be used for sending actions and listening to state.
 * @param initialState      First state of the store.
 * @param reducer           The global reducer, which should be a combination of all child reducers.
 * @return A default store implementation backed by MutableStateFlow.
 * @see kotlinx.coroutines.flow.MutableStateFlow
 * @see kotlinx.coroutines.GlobalScope
 */
fun <S: State, A: Action> createStore(
    initialState: S,
    reducer: Reducer<S, A>
) = MutableStateFlowStore.create(
    initialState = initialState,
    reducer = reducer
)