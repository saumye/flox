package ai.flox.advanced

import ai.flox.advanced.model.AdvancedModeAction
import ai.flox.advanced.model.AdvancedModeState
import ai.flox.arch.Pure
import ai.flox.arch.ReduceResult
import ai.flox.arch.Reducer
import ai.flox.arch.noEffect
import ai.flox.arch.withFlowEffect
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatMessage
import ai.flox.chat.model.ChatMessage.Companion.USER_ID_AI
import ai.flox.chat.model.ChatMessage.Companion.USER_ID_SELF
import ai.flox.state.Action
import ai.flox.state.Resource
import ai.liv.s2tlibrary.RecorderState
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvancedModeViewModel @Inject constructor(
    @ApplicationContext private val application: Context
) : ViewModel(), Reducer<AdvancedModeState, Action> {

    val TAG = "AdvancedModeViewModel"

    private val s2t: S2T = S2T(application)
    private val tts: TTS = TTS(application)

    // StateFlow to hold the accumulating user utterance text
    private val _currentUtteranceText = MutableStateFlow("")

    // StateFlow to track TTS activity
    private val _isTtsSpeaking = MutableStateFlow(false)
    // val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow() // Expose if needed

    // Job for collecting VAD state
    private var vadStateJob: Job? = null
    private var listenerAudioJob: Job? = null // Keep track of listener job
    private var transcriptionResultJob: Job? = null // Job for collecting transcription results

    @Pure
    @Synchronized
    override fun reduce(
        state: AdvancedModeState,
        action: Action
    ): ReduceResult<AdvancedModeState, Action> {
        // Log state transitions and actions for debugging
        // Log.d(TAG, "Reduce: State=$state, Action=$action")

        return when (action) {
            is AdvancedModeAction.RenderAdvancedMode -> {
                _currentUtteranceText.value = ""
                _isTtsSpeaking.value = false // Reset TTS state
                state.copy(
                    // ... (rest of copy)
                    conversation = action.conversation,
                    recentChatList = mapOf(),
                    voiceInputState = action.voiceInput ?: AdvancedModeState.VoiceState(),
                    assistantOutputState = action.assistantOutput ?:AdvancedModeState.VoiceState()
                ).withFlowEffect(merge(flowOf(Action.Navigate(route = AdvancedModeRoutes.conversation)), flowOf(AdvancedModeAction.StartRecord(action.conversation))))
            }

            is ChatAction.CreateOrUpdateMessages -> {
                when (action.resource) {
                    is Resource.Success<ChatMessage> -> {
                        val res = action.resource as Resource.Success<ChatMessage>
                        val message = res.data
                        if(!message.isSelf() && message.conversation.id.equals(state.conversation?.id)) {
                            _currentUtteranceText.value = ""
                            // Only trigger StartSpeak if TTS isn't already running (e.g., from a previous message)
                            if (!_isTtsSpeaking.value) {
                                return state.copy(
                                    voiceInputState = state.voiceInputState.copy(text = "")
                                ).withFlowEffect(flowOf(AdvancedModeAction.StartSpeak(res.data.message)))
                            } else {
                                Log.w(TAG, "AI message received but TTS already active, skipping StartSpeak.")
                                return state.copy(
                                    voiceInputState = state.voiceInputState.copy(text = "")
                                ).noEffect() // Still clear user text
                            }
                        } else {
                            return state.noEffect()
                        }
                    }
                    else -> state.noEffect()
                }
            }

            is AdvancedModeAction.UpdateVisualisation -> {
                // ... (Update logic remains the same) ...
                val newState = state.copy()
                if (action.source == USER_ID_AI) {
                    newState.assistantOutputState = AdvancedModeState.VoiceState(
                        text = state.assistantOutputState.text,
                        voiceSamples = action.audio
                    )
                } else {
                    newState.voiceInputState = AdvancedModeState.VoiceState(
                        text = _currentUtteranceText.value,
                        voiceSamples = action.audio
                    )
                }
                newState.forceUpdate = System.nanoTime() // Keep this for visualization updates if needed
                newState.noEffect()
            }

            is AdvancedModeAction.AppendUserText -> {
                _currentUtteranceText.value += action.textSegment
                state.copy(
                    voiceInputState = state.voiceInputState.copy(text = _currentUtteranceText.value)
                ).noEffect()
            }

            is AdvancedModeAction.AssistantOutput -> state.copy(assistantOutputState = state.assistantOutputState.copy(text = action.text)).noEffect()

            is AdvancedModeAction.StartRecord -> {
                _currentUtteranceText.value = ""
                if (!_isTtsSpeaking.value) {
                    Log.i(TAG, "Starting recording...")
                    state.copy(
                        voiceInputState = AdvancedModeState.VoiceState(text = "")
                    ).withFlowEffect(recordMessage())
                } else {
                    Log.w(TAG, "Ignoring StartRecord because TTS is speaking.")
                    state.noEffect() // Ignore if TTS is active
                }
            }
            is AdvancedModeAction.StartSpeak -> {
                // Optional: Clear previous assistant text immediately for responsiveness
                state.copy(
                    assistantOutputState = AdvancedModeState.VoiceState(text = "")
                ).withFlowEffect(merge(flowOf(AdvancedModeAction.AssistantOutput(action.text)), speakMessage(action.text)))
            }
            is AdvancedModeAction.UserInput -> {
                _currentUtteranceText.value = "" // Clear accumulator
                // Send message using the repository with the online/offline state
                state.copy(
                    voiceInputState = state.voiceInputState.copy(text = "") // Clear display
                ).withFlowEffect(
                    flowOf(ChatAction.SendMessage(action.text, state.conversation!!))
                )
            }

            // Handle TTS State Actions
            is AdvancedModeAction.TtsStarted -> {
                Log.d(TAG, "Reducer: TTS Started")
                _isTtsSpeaking.value = true
                state.withFlowEffect(flowOf(AdvancedModeAction.SetIdleState(false, USER_ID_AI)))
            }
            is AdvancedModeAction.TtsFinished -> {
                Log.d(TAG, "Reducer: TTS Finished")
                _isTtsSpeaking.value = false
                state.withFlowEffect(
                    merge(
                        flowOf(AdvancedModeAction.SetIdleState(true, USER_ID_AI)),
                        flowOf(AdvancedModeAction.StartRecord(state.conversation!!))
                    )
                )
            }

            is AdvancedModeAction.ToggleOnlineMode -> {
                state.copy(
                    isOnlineMode = action.isOnline,
                    forceUpdate = System.nanoTime()
                ).noEffect()
            }
            
            is AdvancedModeAction.CloseAction -> {
                // Stop recording, TTS, and navigate back
                s2t.stopStreaming()
                tts.stop()
                
                state.withFlowEffect(flowOf(Action.Navigate.BACK))
            }
            
            is AdvancedModeAction.SetIdleState -> {
                // Update idle state for visualization
                val newState = state.copy()
                if (action.source == USER_ID_AI) {
                    newState.assistantOutputState = state.assistantOutputState.copy(
                        isIdle = action.isIdle
                    )
                } else {
                    newState.voiceInputState = state.voiceInputState.copy(
                        isIdle = action.isIdle
                    )
                }
                newState.forceUpdate = System.nanoTime()
                newState.noEffect()
            }

            else -> state.noEffect()
        }
    }

    private fun recordMessage(): Flow<Action> {
        // Ensure any previous jobs are cancelled before starting new ones
        listenerAudioJob?.cancel()
        vadStateJob?.cancel()
        transcriptionResultJob?.cancel() // Cancel previous transcription collection

        return callbackFlow {
            // First set user not idle when recording starts
            trySend(AdvancedModeAction.SetIdleState(false, USER_ID_SELF))

            // Collect listener audio data
            s2t.listenerAudioData?.let {
                listenerAudioJob = viewModelScope.launch {
                    it.catch { e -> Log.e(TAG, "Error in listenerAudioData flow", e) }
                        .collect { audioSamples ->
                            trySend(AdvancedModeAction.UpdateVisualisation(audioSamples, USER_ID_SELF))
                        }
                }
            } ?: run { Log.w(TAG, "listenerAudioData is null") }

            // Collect VAD State to detect end of speech
            s2t.isSpeechDetectedFlow?.let {
                vadStateJob = viewModelScope.launch {
                    var speechStartTime = -1L // Track when speech started
                    it.onEach { speaking -> if (speaking && speechStartTime < 0) speechStartTime = System.currentTimeMillis() } // Mark speech start
                        .debounce { speaking -> if (speaking) 0L else 1000L } // Emit immediately if speaking, debounce silence by 1s
                        .filter { !it && speechStartTime >= 0 } // Interested in silence *after* speech started
                        .take(1) // Only need the first occurrence of end-of-speech
                        .collect { _ -> // isSpeaking will be false here
                            Log.d(TAG, "VAD detected end of speech (silence > 1s after speech)")
                            val finalText = _currentUtteranceText.value.trim()
                            if (finalText.isNotBlank()) {
                                Log.i(TAG, "Sending final UserInput: '$finalText'")
                                trySend(AdvancedModeAction.UserInput(text = finalText))
                                // Clear accumulator immediately after sending UserInput action
                                // The reducer will handle clearing it from the state later
                                _currentUtteranceText.value = ""
                            } else {
                                Log.i(TAG, "End of speech detected, but no text accumulated.")
                            }
                            // Stop recording *after* processing the end of speech
                            Log.d(TAG, "Stopping recording due to VAD end-of-speech")
                            s2t.stopStreaming() // This should trigger awaitClose eventually

                            // Cancel this VAD job as its purpose is fulfilled
                            this.cancel()
                            speechStartTime = -1 // Reset speech start time
                        }
                }
            } ?: run { Log.w(TAG, "isSpeechDetectedFlow is null") }

            // Collect Transcription Results
            transcriptionResultJob = viewModelScope.launch {
                s2t.transcriptionResultFlow
                    .catch { e -> Log.e(TAG, "Error in transcriptionResultFlow", e) }
                    .collect { result ->
                            Log.d(TAG, "Collected transcription result: '$result'")
                            // Append result (reducer updates _currentUtteranceText)
                            trySend(AdvancedModeAction.AppendUserText(textSegment = result + " "))
                    }
            }

            // Start streaming (No listener needed anymore)
            s2t.startStreaming() // Remove the listener argument

            // Add idle state reset when stopping recording
            awaitClose {
                Log.d(TAG, "recordMessage Flow closing (awaitClose)")
                listenerAudioJob?.cancel()
                vadStateJob?.cancel()
                transcriptionResultJob?.cancel()
                
                // Set user idle when recording stops
                trySend(AdvancedModeAction.SetIdleState(true, USER_ID_SELF))
            }
        }.buffer(Channel.BUFFERED) // Use a buffered channel
            .catch { e -> Log.e(TAG, "Error in recordMessage flow", e) } // Catch errors in the flow
    }

    fun speakMessage(message: String): Flow<AdvancedModeAction> {
        return callbackFlow {
            var ttsJob: Job? = null
            val currentState = s2t.recorderStateFlow?.value
            Log.d(TAG, "Attempting to speak. Recorder state: $currentState")

            // Allow speaking unless actively recording.
            if (currentState != RecorderState.Recording) {
                Log.d(TAG, "Proceeding with TTS...")
                trySend(AdvancedModeAction.TtsStarted) // Signal TTS start

                ttsJob = viewModelScope.launch {
                    try {
                        // Generate audio for each sentence/phrase
                        for (sentence in message.split(".", ",", "!", "?").filter { it.isNotBlank() }) {
                            Log.d(TAG, "Generating TTS for: $sentence")
                            tts.generate(sentence.trim()) // Assuming this queues generation
                        }

                        // Collect all generated audio after queuing
                        tts.generatedAudio
                            .catch { e -> Log.e(TAG, "Error in generatedAudio flow", e) }
                            .collect { audioChunk ->
                                Log.d(TAG, "Sending TTS audio chunk for visualization")
                                trySend(AdvancedModeAction.UpdateVisualisation(audioChunk, USER_ID_AI))
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during TTS generation/collection", e)
                    } finally {
                        Log.d(TAG, "TTS generation/collection Job finished.")
                        // Signal completion here regardless of errors in generation itself
                        trySend(AdvancedModeAction.TtsFinished)
                    }
                }
            } else {
                Log.w(TAG, "Recorder is active ($currentState), skipping TTS generation for: $message")
                // Send finished immediately if we skip? Or let the caller handle it?
                // Sending finished ensures the state machine progresses if the trigger condition was brief.
                trySend(AdvancedModeAction.TtsFinished)
                close() // Close the flow immediately if skipped
            }

            awaitClose {
                Log.d(TAG, "speakMessage Flow closing (awaitClose)")
                ttsJob?.cancel()
                tts.stop()
                // Ensure TTS state is reset if flow is cancelled externally
                if (_isTtsSpeaking.value) {
                    // This might race with the finally block's trySend, but ensures cleanup
                    _isTtsSpeaking.value = false
                    Log.d(TAG, "Resetting TTS speaking state in awaitClose")
                }
            }
        }.buffer(Channel.BUFFERED) // Use buffered channel
            .catch { e -> Log.e(TAG, "Error in speakMessage flow", e) } // Catch errors
    }

    override fun onCleared() {
        Log.d(TAG, "ViewModel cleared, releasing resources.")
        s2t.release()
        tts.stop()
        super.onCleared()
    }
}