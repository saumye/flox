package ai.flox.advanced.ui

import ai.flox.advanced.AudioPlaybackVisualizer
import ai.flox.advanced.model.AdvancedModeState
import ai.flox.arch.Store
import ai.flox.state.Action
import ai.flox.state.State
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

val dispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()

@Composable
fun AdvancedModeScreen(
    stateFlow: StateFlow<AdvancedModeState>,
    store: Store<State, Action>,
    conversation: String
) {
    val state: AdvancedModeState by stateFlow.collectAsStateWithLifecycle()
    var speechWavesViewRef by remember { mutableStateOf<SpeechWavesView?>(null) }
    var visualizer by remember { mutableStateOf<AudioPlaybackVisualizer?>(null) }

    // Store the AudioTrack reference if needed
    val audioTrackRef = remember { mutableStateOf<AudioTrack?>(null) }

    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (voiceWaves, contentArea, playButton) = createRefs()

        // SpeechWavesView for visualization
        AndroidView(
            factory = { context ->
                SpeechWavesView(context).apply {
                    density = 0.2f
                    pathCount = 4
                    speed = AnimationSpeed.NORMAL
                }.also {
                    speechWavesViewRef = it
                }
            },
            modifier = Modifier
                .constrainAs(voiceWaves) {
                    centerHorizontallyTo(parent)
                    centerVerticallyTo(parent)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.value(140.dp)
                }
        )

        state.assistantOutputState.byteArray?.let { audioData ->
            // This effect handles cleanup
            LaunchedEffect (audioData) {
                    CoroutineScope(dispatcher).launch {
                        playAudioWithVisualization(audioData, speechWavesViewRef)
                    }
            }
        }

        // Main content area
        Box(
            modifier = Modifier
                .constrainAs(contentArea) {
                    top.linkTo(voiceWaves.bottom)
                    bottom.linkTo(playButton.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
        ) {
            // Rest of your UI
        }
    }

    // This effect handles cleanup
    DisposableEffect(Unit) {
        onDispose {
            visualizer?.stop()
            audioTrackRef.value?.release()
        }
    }
}

private fun playAudioWithVisualization(audioData: FloatArray, speechWavesView: SpeechWavesView?) {
    if (speechWavesView == null) return

    Log.d("AdvancedModeScreen","playAudioWithVisualization start")
    // Create and configure AudioTrack
    val sampleRate = 24000 // Use the actual sample rate of your audio
    val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
    val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(audioFormat)
                .setChannelMask(channelConfig)
                .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

    // Create the visualizer
    val visualizer = AudioPlaybackVisualizer(
        audioData = audioData,
        sampleRate = sampleRate,
        speechWavesView = speechWavesView
    )
    Log.d("AdvancedModeScreen","playAudioWithVisualization start")
    // Start playback and visualization
    audioTrack.play()
    visualizer.start(audioTrack)

    // Write data to AudioTrack
    audioTrack.write(audioData, 0, audioData.size, AudioTrack.WRITE_BLOCKING)

//    // Set a marker at the end of the playback
    audioTrack.setNotificationMarkerPosition(audioData.size)
}