package ai.flox.arch

import ai.flox.state.Action
import ai.flox.state.State

/**
 * Allows multiple reducers of the same type to be composed into a single reducer.
 * This is usually used in combination with [PullbackReducer] to create the main reducer which is
 * then used to create a store.
 * @see createStore
 */
class CompositeReducer<S: State, A: Action>(private val reducers: List<Reducer<S, A>>) :
    Reducer<S, A> {
    override fun reduce(state: S, action: A): ReduceResult<S, A> =
        reducers.fold(ReduceResult(state, NoEffect)) { accResult, reducer ->
            val result = reducer.reduce(accResult.state, action)
            ReduceResult(result.state, accResult.effect mergeWith result.effect)
        }
}

/**
 * Wraps a reducer to change its action and state type. This allows combining multiple child
 * reducers into a single parent reducer.
 * see also [CompositeReducer]
 */
class PullbackReducer<ChildState : State, ParentState : State, ChildAction : Action, ParentAction : Action>(
    private val innerReducer: Reducer<ChildState, ChildAction>,
    private val mapToChildAction: (ParentAction) -> ChildAction?,
    private val mapToChildState: (ParentState) -> ChildState,
    private val mapToParentAction: (ChildAction) -> ParentAction,
    private val mapToParentState: (ParentState, ChildState) -> ParentState
) : Reducer<ParentState, ParentAction> {
    override fun reduce(
        state: ParentState,
        action: ParentAction
    ): ReduceResult<ParentState, ParentAction> {
        val childAction = mapToChildAction(action)
            ?: return ReduceResult(state, NoEffect)

        val childResult = innerReducer.reduce(mapToChildState(state), childAction)

        return ReduceResult(
            mapToParentState(state, childResult.state),
            childResult.effect.map(mapToParentAction),
        )
    }
}