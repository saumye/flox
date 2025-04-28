package ai.flox.advanced.model

import ai.flox.chat.model.ChatMessage
import ai.flox.conversation.model.Conversation
import ai.flox.state.State

data class AdvancedModeState(
    val conversation: Conversation? = null,
    val recentChatList: Map<String, ChatMessage> = mapOf(),
    val voiceInputState: VoiceState = VoiceState(),
    val assistantOutputState: VoiceState = VoiceState()
) : State {

    class VoiceState(val text: String? = null, val byteArray: FloatArray? = null)

    companion object {
        const val stateKey = "advancedModeState"
    }
}