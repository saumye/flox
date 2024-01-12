package ai.flox.conversation.model

import ai.flox.state.Action
import ai.flox.state.Resource

sealed interface ConversationAction : Action {
    data object RecentConversationRendered : Action.UI.RenderEvent, ConversationAction {
        override val componentIdentifier = ChatIds.RecentConversations
    }
    data class ConversationClicked(val conversation: String) : Action.UI.ClickedEvent,
        ConversationAction {
        override val componentIdentifier = ChatIds.ConversationTitle
    }

    //Chat
    data class CreateOrUpdateConversations(override val resource: Resource<Conversation>) : Action.Data.LoadData<Conversation>,
        ConversationAction
    data class LoadConversations(override val resource: Resource<Conversation>) : Action.Data.LoadData<Conversation>,
        ConversationAction
    data class UpdateConversations(override val resource: Resource<Conversation>) : Action.Data.UpdateData<Conversation>,
        ConversationAction
    data class DeleteConversations(override val resource: Resource<Conversation>) : Action.Data.LoadData<Conversation>,
        ConversationAction
}

object ChatIds {
    const val RecentConversations = "RecentConversations"
    const val ConversationTitle = "ConversationTitle"
}