package ai.flox.chat.model

import ai.flox.conversation.model.Conversation
import ai.flox.state.State

data class ChatState(
    val conversation: Conversation? = null,
    val recentChatList: Map<String, ChatMessage> = mapOf(),
    val composeState: ComposeState = ComposeState.LoadComplete(),
    val voiceInputState: VoiceState = VoiceState(),
    val assistantOutputState: VoiceState = VoiceState()
) : State {

    sealed interface ComposeState {
        val userInput: String
        data class Loading(override val userInput: String = "") : ComposeState
        data class LoadComplete(override val userInput: String = "") : ComposeState
    }

    class VoiceState(val voiceInputStr: String? = null)

    companion object {
        const val stateKey = "chatState"
    }
}