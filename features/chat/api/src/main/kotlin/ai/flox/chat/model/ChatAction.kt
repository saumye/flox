package ai.flox.chat.model

import ai.flox.conversation.model.Conversation
import ai.flox.state.Action
import ai.flox.state.Resource

sealed interface ChatAction : Action {
    data class RenderChatList(val conversation: Conversation) : Action.UI.RenderEvent, ChatAction {
        override val componentIdentifier = ChatIds.RecentChats
    }

    data class SendMessage(val message: String, val conversation: Conversation) : Action.UI.ClickedEvent, ChatAction {
        override val componentIdentifier = ChatIds.BtnSend
    }

    //Chat
    data class CreateOrUpdateMessages(override val resource: Resource<ChatMessage>) :
        Action.Data.LoadData<ChatMessage>,
        ChatAction

    data class LoadMessages(override val resource: Resource<List<ChatMessage>>) :
        Action.Data.LoadData<List<ChatMessage>>,
        ChatAction

    data class UpdateMessages(override val resource: Resource<List<ChatMessage>>) :
        Action.Data.UpdateData<List<ChatMessage>>,
        ChatAction

    data class DeleteMessages(override val resource: Resource<ChatMessage>) :
        Action.Data.LoadData<ChatMessage>,
        ChatAction
}

object ChatIds {
    const val RecentChats = "RecentChats"
    const val BtnSend = "BtnSend"
}