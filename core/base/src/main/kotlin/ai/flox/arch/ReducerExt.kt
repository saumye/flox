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
/**
 * Embeds a child reducer within a parent domain, allowing it to operate on childs of a collection
 * within the parent's state.
 *
 * For example, if a parent feature manages an array of child states, you can utilize the [forEach]
 * operator to execute both the parent and child's logic:
 *
 * The `forEach` function ensures a specific order of operations, first running the child reducer and
 * then the parent reducer. Reversing this order could lead to subtle bugs, as the parent feature
 * might remove the child state from the array before the child can react to the action.
 */
class ForEachReducer<ChildState : State, ParentState : State, ChildAction : Action, ParentAction : Action, ID>(
    private val parentReducer: Reducer<ParentState, ParentAction>,
    private val childReducer: Reducer<ChildState, ChildAction>,
    private val mapToChildAction: (ParentAction) -> Pair<ID, ChildAction>?,
    private val mapToChildState: (ParentState, ID) -> ChildState,
    private val mapToParentAction: (ChildAction, ID) -> ParentAction,
    private val mapToParentState: (ParentState, ChildState, ID) -> ParentState
) : Reducer<ParentState, ParentAction> {
    override fun reduce(
        state: ParentState,
        action: ParentAction,
    ): ReduceResult<ParentState, ParentAction> {
        val (id: ID, childAction: ChildAction) = mapToChildAction(action) ?: return parentReducer.reduce(state, action)
        val mapToChildState: (ParentState) -> ChildState = { parentState: ParentState -> mapToChildState(parentState, id) }
        val mapToState: (ParentState, ChildState) -> ParentState = { parentState: ParentState, childState: ChildState -> mapToParentState(parentState, childState, id) }

        val (childState, childEffect) = childReducer.reduce(mapToChildState(state), childAction)
        val (parentState, parentEffect) = parentReducer.reduce(mapToState(state, childState), action)

        return ReduceResult(
            state = parentState,
            effect = childEffect.map { mapToParentAction(it, id) } mergeWith parentEffect,
        )
    }
}

/**
 * A specialized version of [ForEachReducer] for handling a parent state with a [Map] of childs,
 * each associated with a unique key.
 */
fun <ChildState : State, ParentState : State, ChildAction : Action, ParentAction : Action, ID>
        Reducer<ParentState, ParentAction>.forEachMap(
    childReducer: Reducer<ChildState, ChildAction>,
    mapToChildAction: (ParentAction) -> Pair<ID, ChildAction>?,
    mapToChildMap: (ParentState) -> Map<ID, ChildState>,
    mapToParentAction: (ChildAction, ID) -> ParentAction,
    mapToParentState: (ParentState, Map<ID, ChildState>) -> ParentState
): Reducer<ParentState, ParentAction> =
    ForEachReducer(
        parentReducer = this,
        childReducer = childReducer,
        mapToChildAction = mapToChildAction,
        mapToChildState = { state, key -> mapToChildMap(state)[key] ?: throw NoSuchElementException("Element with key=$key not found") },
        mapToParentAction = mapToParentAction,
        mapToParentState = { state, childState, key ->
            val newChildMap = mapToChildMap(state).toMutableMap().apply {
                this[key] = childState
            }
            mapToParentState(state, newChildMap)
        },
    )

