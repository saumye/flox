package ai.flox.chat.model

import ai.flox.state.Action
import ai.flox.state.Resource

sealed interface ChatAction : Action {
    data object RecentChatsRendered : Action.UI.RenderEvent, ChatAction {
        override val componentIdentifier = ChatIds.RecentChats
    }

    data class SendButtonClicked(val message: String) : Action.UI.ClickedEvent, ChatAction {
        override val componentIdentifier = ChatIds.BtnSend
    }

    //Chat
    data class CreateOrUpdateMessages(override val resource: Resource<ChatMessage>) :
        Action.Data.LoadData<ChatMessage>,
        ChatAction

    data class LoadMessages(override val resource: Resource<ChatMessage>) :
        Action.Data.LoadData<ChatMessage>,
        ChatAction

    data class UpdateMessages(override val resource: Resource<ChatMessage>) :
        Action.Data.UpdateData<ChatMessage>,
        ChatAction

    data class DeleteMessages(override val resource: Resource<ChatMessage>) :
        Action.Data.LoadData<ChatMessage>,
        ChatAction
}

object ChatIds {
    const val RecentChats = "RecentChats"
    const val BtnSend = "BtnSend"
}