package ai.flox.arch

import ai.flox.state.Action
import ai.flox.state.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class MutableStateFlowStore<S: State, A: Action> private constructor(
    override val state: Flow<S>,
    private val sendFn: (List<A>) -> Unit,
) : Store<S, A> {

    override fun <ViewState: S, ViewAction : A> view(
        mapToLocalState: (State) -> ViewState,
        mapToGlobalAction: (ViewAction) -> A?,
    ): Store<ViewState, ViewAction> = MutableStateFlowStore(
        state = state.map { mapToLocalState(it) }.distinctUntilChanged(),
        sendFn = { actions ->
            val globalActions = actions.mapNotNull(mapToGlobalAction)
            sendFn(globalActions)
        },
    )

    companion object {
        fun <S: State, A: Action> create(
            initialState: S,
            reducer: Reducer<S, A>
        ): Store<S, A> {
            val state = MutableStateFlow(initialState)
            val noEffect = NoEffect

            lateinit var send: (List<A>) -> Unit
            send = { actions ->
                GlobalScope.launch(context = Dispatchers.Main) {
                    val result: ReduceResult<S, A> = actions.fold(ReduceResult(state.value, noEffect)) { accResult, action ->
                        try {
                            val (nextState, nextEffect) = reducer.reduce(accResult.state, action)
                            return@fold ReduceResult(nextState, accResult.effect mergeWith nextEffect)
                        } catch (e: Throwable) {
                            ReduceResult(accResult.state, noEffect)//TODO exceptionHandler.handleReduceException(e))
                        }
                    }

                    state.value = result.state

                    try {
                        result.effect.run()
                            .onEach { action -> send(listOf(action)) }
                            .launchIn(GlobalScope)
                    } catch (e: Throwable) {
                        // exceptionHandler.handleEffectException(e)//TODO
                    }
                }
            }
            return MutableStateFlowStore(state, send)
        }
    }

    override fun dispatch(actions: List<A>) {
        if (actions.isEmpty()) return

        sendFn(actions)
    }
}