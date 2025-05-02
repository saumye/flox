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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.Arrays
import java.util.concurrent.Executors
import kotlin.math.abs

val inputAnimDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
val outputAnimDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()

@Composable
fun AdvancedModeScreen(
    stateFlow: StateFlow<AdvancedModeState>,
    store: Store<State, Action>,
    conversation: String
) {
    val state: AdvancedModeState by stateFlow.collectAsStateWithLifecycle()
    var speechWavesViewRef by remember { mutableStateOf<SpeechWavesView?>(null) }
    val visualizer by remember { mutableStateOf<AudioPlaybackVisualizer?>(null) }

    // Store the AudioTrack reference if needed
    val audioTrackRef = remember { mutableStateOf<AudioTrack?>(null) }

    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (voiceWaves, inputContent, outputContent) = createRefs()

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

        LaunchedEffect(state.forceUpdate) {
            state.assistantOutputState.voiceSamples?.let { audioData ->
                withContext(outputAnimDispatcher) {
                    startOutputVisualisation(audioData, speechWavesViewRef)
                }
            }
        }

        // Use LaunchedEffect with the content hash as key
        LaunchedEffect(state.forceUpdate) {
            state.voiceInputState.voiceSamples?.let { audioData ->
                withContext(inputAnimDispatcher) {
                    startInputVisualisation(audioData, speechWavesViewRef)
                }
            }
        }

        Text(
            text = state.voiceInputState.text,
            color = Color.Black,
            textAlign = TextAlign.Start,
            fontSize = 32.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(outputContent) {
                    top.linkTo(voiceWaves.bottom)
                    start.linkTo(voiceWaves.start)
                    end.linkTo(voiceWaves.end)
                    width = Dimension.fillToConstraints
                }
                .padding(16.dp)
        )
        Text(
            text = state.assistantOutputState.text,
            color = Color.Black,
            textAlign = TextAlign.Start,
            fontSize = 32.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(inputContent) {
                    bottom.linkTo(voiceWaves.top)
                    start.linkTo(voiceWaves.start)
                    end.linkTo(voiceWaves.end)
                    width = Dimension.fillToConstraints
                }
                .padding(16.dp)
        )

        // This effect handles cleanup
        DisposableEffect(Unit) {
            onDispose {
                visualizer?.stop()
                audioTrackRef.value?.release()
            }
        }
    }
}

private fun startOutputVisualisation(audioData: FloatArray, speechWavesView: SpeechWavesView?) {
    if (speechWavesView == null) return

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
    // Start playback and visualization
    audioTrack.play()
    visualizer.start(audioTrack)

    // Write data to AudioTrack
    audioTrack.write(audioData, 0, audioData.size, AudioTrack.WRITE_BLOCKING)

    // Set a marker at the end of the playback
    audioTrack.setNotificationMarkerPosition(audioData.size)
}

private fun startInputVisualisation(audioData: FloatArray, speechWavesView: SpeechWavesView?) {


    // Create a buffer for visualization
    val buffer = ByteArray(audioData.size)

    // Find the maximum amplitude in the current window for normalization
    var maxAmplitude = 0.0f
    for (element in audioData) {
        val amplitude = abs(element)
        if (amplitude > maxAmplitude) {
            maxAmplitude = amplitude
        }
    }

    // Ensure we get visualization even for quiet audio
    val amplificationFactor = if (maxAmplitude < 1.0e-5f) {
        1.0e7f
    } else {
        1.0f / maxAmplitude
    }

    // Fill buffer with amplified audio data
    for (i in audioData.indices) {
        val amplifiedSample = audioData[i] * amplificationFactor
        buffer[i] = (amplifiedSample * 127).toInt().coerceIn(-128, 127).toByte()
    }


    // Update the visualization
    speechWavesView?.update(buffer)
}

