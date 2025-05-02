package ai.flox.advanced.model

import ai.flox.chat.model.ChatMessage
import ai.flox.conversation.model.Conversation
import ai.flox.state.State

data class AdvancedModeState(
    var conversation: Conversation? = null,
    var recentChatList: Map<String, ChatMessage> = mapOf(),
    var voiceInputState: VoiceState = VoiceState(),
    var assistantOutputState: VoiceState = VoiceState(),
    var forceUpdate: Long = 0  // Add this field
) : State {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdvancedModeState

        if (conversation != other.conversation) return false
        if (recentChatList != other.recentChatList) return false

        // Rest of the equality checks
        if (voiceInputState.text != other.voiceInputState.text) return false
        if (assistantOutputState.text != other.assistantOutputState.text) return false

        if(forceUpdate != other.forceUpdate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = conversation?.hashCode() ?: 0
        result = 31 * result + recentChatList.hashCode()
        result = 31 * result + voiceInputState.hashCode()
        result = 31 * result + assistantOutputState.hashCode()
        result = 31 * result + forceUpdate.hashCode()
        result = 31 * result + System.identityHashCode(this) // Add uniqueness
        return result
    }

    data class VoiceState(val text: String = "", val voiceSamples: FloatArray? = null) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as VoiceState

            if (text != other.text) return false
            if (voiceSamples != null) {
                if (other.voiceSamples == null) return false
                if (!voiceSamples.contentEquals(other.voiceSamples)) return false
            } else if (other.voiceSamples != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + (voiceSamples?.contentHashCode() ?: 0)
            return result
        }
    }

    companion object {
        const val stateKey = "advancedModeState"
    }
}