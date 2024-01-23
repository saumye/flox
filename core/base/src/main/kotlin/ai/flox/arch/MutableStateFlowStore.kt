package ai.flox.arch

import ai.flox.state.Action
import ai.flox.state.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class MutableStateFlowStore<S : State, A : Action> private constructor(
    override val state: StateFlow<S>,
    private val sendFn: (List<A>) -> Unit,
) : Store<S, A> {

//    override fun <ViewState: S, ViewAction : A> view(
//        mapToLocalState: (State) -> ViewState,
//        mapToGlobalAction: (ViewAction) -> A?,
//    ): Store<ViewState, ViewAction> = MutableStateFlowStore(
//        state = state.map { mapToLocalState(it) }.distinctUntilChanged(),
//        sendFn = { actions ->
//            val globalActions = actions.mapNotNull(mapToGlobalAction)
//            sendFn(globalActions)
//        },
//    )

    companion object {
        fun <S : State, A : Action> create(
            initialState: S,
            reducer: Reducer<S, A>
        ): Store<S, A> {
            val state = MutableStateFlow(initialState)
            val noEffect = NoEffect

            lateinit var send: (List<A>) -> Unit
            send = { actions ->
                CoroutineScope(Dispatchers.Main).launch(context = Dispatchers.Main) {
                    val result: ReduceResult<S, A> =
                        actions.fold(ReduceResult(state.value, noEffect)) { accResult, action ->
                            val (nextState, nextEffect) = reducer.reduce(
                                accResult.state,
                                action
                            )
                            return@fold ReduceResult(
                                nextState,
                                accResult.effect mergeWith nextEffect
                            )
                        }

                    state.value = result.state

                    result.effect.run()
                        .onEach { action -> send(listOf(action)) }
                        .launchIn(CoroutineScope(Dispatchers.Main))
                }
            }
            return MutableStateFlowStore(state, send)
        }
    }

    override fun dispatch(vararg actions: A) {
        sendFn(actions.asList())
    }
}