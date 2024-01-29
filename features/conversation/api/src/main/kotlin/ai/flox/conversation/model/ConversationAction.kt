package ai.flox.conversation.model

import ai.flox.state.Action
import ai.flox.state.Resource

sealed interface ConversationAction : Action {
    data object RenderConversationList : Action.UI.RenderEvent,
        ConversationAction {
        override val componentIdentifier = ChatIds.RecentConversations
    }

    //Conversation
    data class CreateOrUpdateConversation(override val resource: Resource<Conversation>) :
        Action.Data.LoadData<Conversation>,
        ConversationAction

    data class LoadConversations(override val resource: Resource<List<Conversation>>) :
        Action.Data.LoadData<List<Conversation>>,
        ConversationAction

    data class DeleteConversation(override val resource: Resource<Conversation>) :
        Action.Data.LoadData<Conversation>,
        ConversationAction
}

object ChatIds {
    const val RecentConversations = "RecentConversations"
    const val ConversationTitle = "ConversationTitle"
}