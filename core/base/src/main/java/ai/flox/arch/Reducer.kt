package ai.flox.arch

import ai.flox.state.Action
import ai.flox.state.State

/**
 * Types responsible for handling actions and mutating the state.
 */
fun interface Reducer<S: State, A: Action> {
    /**
     * The reduce function is responsible for transforming actions into state changes.
     * Referential transparency is of utmost importance when it comes to the reduce
     * method, so absolutely no side effects or long-running operations should happen here.
     * The reduce method should instead rely on returning effects which can then signal
     * their completion via actions which will be scheduled for processing.
     * @return A ReduceResult containing the new state and a list of effects that should
     * be executed immediately after reduce method finishes its job
     */
    @Pure
    fun reduce(state: S, action: A): ReduceResult<S, A>
}

/**
 * Annotation that indicates the function is Pure, i.e. no side effects via asynchronous work
 * or updates to any state outside S: State
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Pure