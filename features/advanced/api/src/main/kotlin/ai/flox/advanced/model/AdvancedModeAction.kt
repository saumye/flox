package ai.flox.advanced.model

import ai.flox.advanced.model.AdvancedModeAction.AdvancedModeIds.AdvancedConversationView
import ai.flox.advanced.model.AdvancedModeAction.AdvancedModeIds.Close
import ai.flox.advanced.model.AdvancedModeAction.AdvancedModeIds.Record
import ai.flox.advanced.model.AdvancedModeAction.AdvancedModeIds.Speak
import ai.flox.chat.model.ChatMessage
import ai.flox.conversation.model.Conversation
import ai.flox.state.Action
import ai.flox.state.ComponentIdentifier

sealed interface AdvancedModeAction : Action {

    data class RenderAdvancedMode(
        val conversation: Conversation,
        val voiceInput: AdvancedModeState.VoiceState? = null,
        val assistantOutput: AdvancedModeState.VoiceState? = null
    ) : Action.UI.RenderEvent,
        AdvancedModeAction {
        override val componentIdentifier: ComponentIdentifier = AdvancedConversationView
    }

    data class RecordAction(val conv: Conversation) : Action.UI.RenderEvent,
        AdvancedModeAction {
        override val componentIdentifier: ComponentIdentifier = Record
    }

    data class SpeakAction(val audio: FloatArray) : Action.UI.RenderEvent,
        AdvancedModeAction {
        override val componentIdentifier: ComponentIdentifier = Speak
    }

    data class CloseAction(val conversation: Conversation) : Action.UI.RenderEvent,
        AdvancedModeAction {
        override val componentIdentifier: ComponentIdentifier = Close
    }

    object AdvancedModeIds {
        const val AdvancedConversationView = "AdvancedConversationView"
        const val Close = "Close"
        const val Record = "Record"
        const val Speak = "Speak"
    }
}