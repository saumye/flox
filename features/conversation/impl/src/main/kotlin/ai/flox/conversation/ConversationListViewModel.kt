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
import android.widget.Toast
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
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

            is ConversationAction.RenderConversationList -> {
                state.withFlowEffect(conversationRepository.getConversations())
            }
            is ConversationAction.LoadConversations -> {
                if (action.resource is Resource.Success) {
                    val res = action.resource as Resource.Success<List<Conversation>>
                    state.copy(recentConversationList = res.data.associateBy { msg -> msg.id }).noEffect()
                } else {
                    state.noEffect()
                }
            }
            is ConversationAction.CreateOrUpdateConversation -> {
                when(action.resource) {
                    is Resource.Pending<Conversation> -> {
                        val res = action.resource as Resource.Pending<Conversation>
                        state.withFlowEffect(conversationRepository.addConversation(res.data))
                    }
                    is Resource.Success<Conversation> -> {
                        val res = action.resource as Resource.Success<Conversation>
                        state.recentConversationList.let {
                            state.copy(recentConversationList = it.toMutableMap().apply {
                                put(res.data.id,res.data)
                            }.toMap()).noEffect()
                        }
                    }
                    else -> state.noEffect()
                }
            }
            else -> state.noEffect()
        }
    }
}