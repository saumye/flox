package ai.flox.chat

import ai.flox.arch.Pure
import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.arch.withFlowEffect
import ai.flox.chat.data.ChatRepository
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatMessage
import ai.flox.chat.model.ChatState
import ai.flox.state.Action
import ai.flox.state.Resource
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel(), Reducer<ChatState, Action> {

    @Pure
    @Synchronized
    override fun reduce(state: ChatState, action: Action): ReduceResult<ChatState, Action> {
        if(action !is ChatAction){
            return state.noEffect()
        }
        return when (action) {
            is ChatAction.SendButtonClicked -> {
                state.copy(composeState = ChatState.ComposeState.Loading(state.composeState.userInput))
                    .withFlowEffect(
                        chatRepository.sendMessage(
                            ChatMessage(
                                message = action.message,
                                isUser = true,
                                timestamp = Date(System.currentTimeMillis())
                            )
                        )
                    )
            }

            is ChatAction.RecentChatsRendered -> {
                state.withFlowEffect(chatRepository.getChatMessages())
            }

            is ChatAction.LoadMessages -> {
                if (action.resource is Resource.Success<ChatMessage>) {
                    val res = action.resource as Resource.Success<ChatMessage>
                    res.data?.let {
                        state.copy(recentChatList = it.associateBy { msg -> msg.id }).noEffect()
                    } ?: state.noEffect()
                } else {
                    state.noEffect()
                }
            }

            is ChatAction.CreateOrUpdateMessages -> {
                if (action.resource is Resource.Success<ChatMessage>) {
                    val res = action.resource as Resource.Success<ChatMessage>
                    res.data?.let { messages ->
                        state.recentChatList?.let {
                            val map = it.toMutableMap()
                            for (message in messages) {
                                map[message.id] = message
                            }
                            state.copy(recentChatList = map.toMap()).noEffect()
                        } ?: state.noEffect()
                    } ?: state.noEffect()
                } else {
                    state.noEffect()
                }
            }

            else -> state.noEffect()
        }
    }
}