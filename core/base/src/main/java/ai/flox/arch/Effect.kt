package ai.flox.arch

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

/**
 * Effects are returned by reducers when they wish to produce a side effect.
 * This can be anything from cpu/io bound operations to changes that simply affect the UI.
 * @see Reducer.reduce
 */
fun interface Effect<out Action> {

    /**
     * Executes the effect. This operation can produce side effects, and it's the
     * responsibility of the class implementing this interface to change threads
     * to prevent blocking the UI when needed.
     * @return An action that will be sent again for further processing
     * @see Store.dispatch
     */
    fun run(): Flow<Action>

    companion object {

        fun <Action> fromFlow(flow: Flow<Action>): Effect<Action> =
            Effect { flow }
    }
}

object NoEffect : Effect<Nothing> {
    override fun run(): Flow<Nothing> = emptyFlow()
}


/**
 * @param effect The Effect instance to be merged with the current Effect.
 * @return A new Effect instance combining the emissions from both the current and provided Effect.
 */
infix fun <Action> Effect<Action>.mergeWith(effect: Effect<Action>): Effect<Action> =
    Effect {
        kotlinx.coroutines.flow.merge(run(), effect.run())
    }

/**
 * Transforms and effect by mapping the resulting action into a different type.
 *
 * @param mapFn The function to transform the returned action.
 * @return An effect whose return action type has been mapped.
 */
inline fun <T, R> Effect<T>.map(crossinline transform: suspend (T) -> R): Effect<R> =
    if (this is NoEffect) {
        NoEffect
    } else {
        Effect { this.run().map { transform(it) } }
    }

/**
 * @return ReduceResult containing the current state and 'NoEffect'.
 */
fun <State, Action> State.noEffect(): ReduceResult<State, Action> =
    ReduceResult(this)

/**
 * Returns a ReduceResult with the current state and a flow-based effect.
 *
 * @param flow The flow of actions to be executed as an effect.
 * @return ReduceResult containing the current state and the defined effect.
 */
fun <State, Action> State.withFlowEffect(flow: Flow<Action>): ReduceResult<State, Action> =
    ReduceResult(this, Effect.fromFlow(flow))
