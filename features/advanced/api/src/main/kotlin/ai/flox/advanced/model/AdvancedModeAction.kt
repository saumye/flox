package ai.flox.advanced.model

import ai.flox.advanced.model.AdvancedModeAction.AdvancedModeIds.AdvancedConversationView
import ai.flox.advanced.model.AdvancedModeAction.AdvancedModeIds.Close
import ai.flox.advanced.model.AdvancedModeAction.AdvancedModeIds.InputText
import ai.flox.advanced.model.AdvancedModeAction.AdvancedModeIds.Visualisation
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

    data class StartRecord(val conv: Conversation) : Action.UI.InputEvent,
        AdvancedModeAction {
        override val componentIdentifier: ComponentIdentifier = Visualisation
    }

    data class UserInput(val text: String) : Action.UI.RenderEvent,
        AdvancedModeAction {
        override val componentIdentifier: ComponentIdentifier = Visualisation
    }

    data class AppendUserText(val textSegment: String): Action.UI.RenderEvent, AdvancedModeAction {
        override val componentIdentifier: ComponentIdentifier = InputText
    }

    object TtsStarted : AdvancedModeAction

    object TtsFinished : AdvancedModeAction

    data class UpdateVisualisation(val audio: FloatArray, val source: String) : Action.UI.RenderEvent,
        AdvancedModeAction {
        override val componentIdentifier: ComponentIdentifier = Visualisation
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as UpdateVisualisation

            if (!audio.contentEquals(other.audio)) return false
            if (source != other.source) return false
            if (componentIdentifier != other.componentIdentifier) return false

            return true
        }

        override fun hashCode(): Int {
            var result = audio.contentHashCode()
            result = 31 * result + source.hashCode()
            result = 31 * result + componentIdentifier.hashCode()
            return result
        }
    }

    data class StartSpeak(val text: String) : Action.UI.InputEvent,
        AdvancedModeAction {
        override val componentIdentifier: ComponentIdentifier = Visualisation
    }

    data class AssistantOutput(val text: String) : Action.UI.RenderEvent,
        AdvancedModeAction {
        override val componentIdentifier: ComponentIdentifier = Visualisation
    }

    data class CloseAction(val conversation: Conversation) : Action.UI.RenderEvent,
        AdvancedModeAction {
        override val componentIdentifier: ComponentIdentifier = Close
    }

    object AdvancedModeIds {
        const val AdvancedConversationView = "AdvancedConversationView"
        const val Close = "Close"
        const val Visualisation = "Visualisation"
        const val InputText = "InputText"
    }
}