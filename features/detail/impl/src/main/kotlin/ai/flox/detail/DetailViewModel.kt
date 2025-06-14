package ai.flox.detail

import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.arch.withFlowEffect
import ai.flox.detail.model.DetailAction
import ai.flox.detail.model.DetailState
import ai.flox.state.Action
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

class DetailViewModel(
) : Reducer<DetailState, Action> {
    
    override fun reduce(currentState: DetailState, action: Action): ReduceResult<DetailState, Action> {
        if (currentState !is DetailState) {
            return currentState.noEffect()
        }
        
        return when (action) {
            is DetailAction.LoadArticle -> {
                currentState.copy(
                    currentArticle = action.newsItem,
                    isLoading = false
                ).withFlowEffect(flowOf(Action.Navigate(route = DetailRoutes.detail)))
            }
            else -> currentState.noEffect()
        }
    }
} 