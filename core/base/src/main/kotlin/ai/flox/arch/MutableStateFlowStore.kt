package ai.flox.arch

import ai.flox.state.Action
import ai.flox.state.State
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MutableStateFlowStore<S : State, A : Action> private constructor(
    override val state: StateFlow<S>,
    private val sendFn: (List<A>) -> Unit,
) : Store<S, A> {

    companion object {
        private const val TAG = "MutableStateFlowStore"

        fun <S : State, A : Action> create(
            initialState: S,
            reducer: Reducer<S, A>
        ): Store<S, A> {
            val mutableState = MutableStateFlow(initialState)
            val noEffect = NoEffect

            lateinit var send: (List<A>) -> Unit
            send = { actions ->
                CoroutineScope(Dispatchers.Main.immediate).launch {

                    val result: ReduceResult<S, A> =
                        actions.fold(ReduceResult(mutableState.value, noEffect)) { accResult, action ->
                            val oldState = accResult.state
                            val reduceResult = reducer.reduce(oldState, action)
                            val newState = reduceResult.state

                            ReduceResult(
                                newState,
                                accResult.effect mergeWith reduceResult.effect
                            )
                        }
                    mutableState.value = result.state
                    result.effect.run()
                        .onEach { action ->
                            send(listOf(action))
                        }
                        .launchIn(CoroutineScope(Dispatchers.Main.immediate))
                }
            }
            return MutableStateFlowStore(mutableState, send)
        }
    }

    override fun dispatch(vararg actions: A) {
        sendFn(actions.asList())
    }
}