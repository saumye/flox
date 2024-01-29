package ai.flox.chat

import ai.flox.arch.Pure
import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.arch.withFlowEffect
import ai.flox.chat.data.ChatRepository
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatIds
import ai.flox.chat.model.ChatMessage
import ai.flox.chat.model.ChatState
import ai.flox.conversation.model.Conversation
import ai.flox.conversation.model.ConversationAction
import ai.flox.state.Action
import ai.flox.state.ComponentIdentifier
import ai.flox.state.Resource
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel(), Reducer<ChatState, Action> {

    @Pure
    @Synchronized
    override fun reduce(state: ChatState, action: Action): ReduceResult<ChatState, Action> {
        return when (action) {
            is ChatAction.SendMessage -> {
                state.copy(composeState = ChatState.ComposeState.Loading(state.composeState.userInput))
                    .withFlowEffect(
                        merge(
                            chatRepository.sendMessage(
                                ChatMessage(
                                    message = action.message,
                                    userId = "self",
                                    conversation = action.conversation,
                                    timestamp = Date(System.currentTimeMillis())
                                )
                            ), flowOf(
                                ConversationAction.CreateOrUpdateConversation(
                                    Resource.Success(
                                        action.conversation.copy(
                                            title = action.message,
                                            lastMessageTime = Date(System.currentTimeMillis())
                                        )
                                    )
                                )
                            )
                        )
                    )
            }

            is ConversationAction.CreateOrUpdateConversation -> {
                if (action.resource is Resource.Success) {
                    val res = action.resource as Resource.Success<Conversation>
                    state.copy(conversation = res.data).noEffect()
                } else {
                    state.noEffect()
                }
            }

            is ChatAction.RenderChatList -> {
                state.withFlowEffect(
                    merge(
                        chatRepository.getChatMessages(action.conversation), flowOf(
                            Action.Navigate(route = ChatRoutes.chat)
                        )
                    )
                )
            }

            is ChatAction.LoadMessages -> {
                if (action.resource is Resource.Success) {
                    val res = action.resource as Resource.Success<List<ChatMessage>>    //TODO
                    state.copy(recentChatList = res.data.associateBy { msg -> msg.id }).noEffect()
                } else {
                    state.noEffect()
                }
            }

            is ChatAction.CreateOrUpdateMessages -> {
                when (action.resource) {
                    is Resource.Pending<ChatMessage> -> {
                        val res = action.resource as Resource.Pending<ChatMessage>
                        state.withFlowEffect(chatRepository.sendMessage(res.data))
                    }

                    is Resource.Success<ChatMessage> -> {
                        val res = action.resource as Resource.Success<ChatMessage>
                        state.copy(recentChatList = state.recentChatList.toMutableMap().apply {
                            put(res.data.id, res.data)
                        }.toMap()).noEffect()
                    }

                    else -> state.noEffect()
                }
            }

            else -> state.noEffect()
        }
    }
}