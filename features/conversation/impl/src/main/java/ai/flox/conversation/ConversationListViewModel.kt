package ai.flox.conversation

import ai.flox.arch.Pure
import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.arch.withFlowEffect
import ai.flox.conversation.data.ConversationRepository
import ai.flox.conversation.model.Conversation
import ai.flox.conversation.model.ConversationAction
import ai.flox.conversation.model.ConversationState
import ai.flox.state.Action
import ai.flox.state.Resource
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val chatRepository: ConversationRepository
) : ViewModel(), Reducer<ConversationState, Action> {

    @Pure
    @Synchronized
    override fun reduce(
        state: ConversationState,
        action: Action
    ): ReduceResult<ConversationState, Action> {
        if(action !is ConversationAction) {
            return state.noEffect()
        }
        return when (action) {
            is ConversationAction.ConversationClicked -> {
                state.noEffect()//withFlowEffect(flow {emit(ChatAc)})
            }
            is ConversationAction.RecentConversationRendered -> {
                state.withFlowEffect(chatRepository.getConversations())
            }
            is ConversationAction.LoadConversations -> {
                if (action.resource is Resource.Success<Conversation>) {
                    val res = action.resource as Resource.Success<Conversation>
                    res.data?.let {
                        state.copy(recentConversationList = it.associateBy { msg -> msg.id }).noEffect()
                    } ?: state.noEffect()
                } else {
                    state.noEffect()
                }
            }
            is ConversationAction.CreateOrUpdateConversations -> {
                if (action.resource is Resource.Success<Conversation>) {
                    val res = action.resource as Resource.Success<Conversation>
                    res.data?.let { messages ->
                        state.recentConversationList.let {
                            val map = it.toMutableMap()
                            for (message in messages) {
                                map[message.id] = message
                            }
                            state.copy(recentConversationList = map.toMap()).noEffect()
                        }
                    } ?: state.noEffect()
                } else {
                    state.noEffect()
                }
            }
            else -> state.noEffect()
        }
    }
}