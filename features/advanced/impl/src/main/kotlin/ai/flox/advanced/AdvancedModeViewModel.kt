package ai.flox.advanced

import ai.flox.advanced.model.AdvancedModeAction
import ai.flox.advanced.model.AdvancedModeState
import ai.flox.arch.Pure
import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.arch.withFlowEffect
import ai.flox.asr.Whisper
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatMessage
import ai.flox.conversation.model.Conversation
import ai.flox.state.Action
import ai.flox.state.Resource
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

@HiltViewModel
class AdvancedModeViewModel @Inject constructor(
    @ApplicationContext private val application: Context
) : ViewModel(), Reducer<AdvancedModeState, Action> {

    val TAG = "AdvancedModeViewModel"

    private val s2t: S2T = S2T(application)
    private val tts: TTS = TTS(application)

    @Pure
    @Synchronized
    override fun reduce(
        state: AdvancedModeState,
        action: Action
    ): ReduceResult<AdvancedModeState, Action> {
        Log.d(TAG, action.toString())
        return when (action) {
            is AdvancedModeAction.RenderAdvancedMode -> {
                state.copy(
                    conversation = action.conversation,
                    recentChatList = mapOf(),
                    voiceInputState = action.voiceInput ?: AdvancedModeState.VoiceState(),
                    assistantOutputState = action.assistantOutput ?:AdvancedModeState.VoiceState()
                ).withFlowEffect(merge(recordMessage(action.conversation),
                    flowOf(Action.Navigate(route = AdvancedModeRoutes.conversation))))
            }

            is AdvancedModeAction.RecordAction -> {
                state.withFlowEffect(recordMessage(action.conv))
            }

            is ChatAction.CreateOrUpdateMessages -> {
                when (action.resource) {
                    is Resource.Success<ChatMessage> -> {
                        val res = action.resource as Resource.Success<ChatMessage>
                        val message = res.data
                        if(!message.isSelf() && message.conversation.id.equals(state.conversation?.id)) {
                            return state.withFlowEffect(speakMessage(res.data))
                        } else {
                            return state.noEffect()
                        }
                    }
                    else -> state.noEffect()
                }
            }

            is AdvancedModeAction.SpeakAction -> {
                state.copy(
                    assistantOutputState = AdvancedModeState.VoiceState(
                        byteArray = action.audio,
                        text = state.assistantOutputState.text
                    )
                ).noEffect()
            }

            else -> state.noEffect()
        }
    }

    private fun recordMessage(convId: Conversation): Flow<Action> {
        return callbackFlow {
            s2t.startStreaming(object : Whisper.WhisperListener {
                override fun onUpdateReceived(message: String) {
                    Log.d(TAG, "Update is received, Message: $message")
                }

                override fun onResultReceived(result: String) {
                    Log.d(TAG, "Result: $result")
                    if(s2t.s2tRecorder?.speechDetected == false) {
                        trySendBlocking(
                            ChatAction.SendMessage(
                                message = result,
                                conversation = convId
                            )
                        )
                    }
                }
            })
            awaitClose {
                s2t.startStreaming(null)
            }
        }
    }

    fun speakMessage(message: ChatMessage): Flow<AdvancedModeAction> {
        return callbackFlow {
            if(s2t.s2tRecorder?.should_stop == true) {
                for(message in message.message.split(".", ",", "!")) {
                    tts.generate(message)
                }
                tts.generatedAudio.collect {
                    trySendBlocking(AdvancedModeAction.SpeakAction(it))
                }
            }
            awaitClose {
                tts.stop()
            }
        }
    }


    override fun onCleared() {
        s2t.startStreaming(null)
    }
}