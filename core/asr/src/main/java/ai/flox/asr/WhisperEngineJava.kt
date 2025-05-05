package ai.flox.asr

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Arrays

// Implement the Kotlin interface
class WhisperEngineJava(
    private val context: Context // Use context if needed for delegates later
) : WhisperEngine {

    private val TAG = "WhisperEngineJava"
    // Inject or initialize WhisperUtil. Assuming object for now.
    private val whisperUtil = WhisperUtil()

    @Volatile // Ensure visibility across threads
    private var isInitializedInternal = false
    private lateinit var interpreter: Interpreter
    // private var gpuDelegate: GpuDelegate? = null // Keep for future delegate impl

    init {
        // Potentially load TF Lite dynamically or check availability
    }

    override fun isInitialized(): Boolean {
        return isInitializedInternal
    }

    // Make suspend as model loading is I/O bound
    override suspend fun initialize(modelPath: String, vocabPath: String, multilingual: Boolean): Boolean {
        return withContext(Dispatchers.IO) { // Perform I/O and init on IO thread
            try {
                // Load model first
                loadModel(modelPath)
                Log.d(TAG, "Model loaded: $modelPath")

                // Load filters and vocab
                val vocabLoaded = whisperUtil.loadFiltersAndVocab(multilingual, vocabPath)
                if (vocabLoaded) {
                    isInitializedInternal = true
                    Log.d(TAG, "Filters and Vocab loaded: $vocabPath")
                } else {
                    isInitializedInternal = false
                    Log.e(TAG, "Failed to load Filters and Vocab.")
                    // Clean up loaded model if vocab fails?
                    deinitialize()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error initializing model/vocab", e)
                isInitializedInternal = false
                deinitialize() // Ensure interpreter is closed on error
            }
            isInitializedInternal
        }
    }

    // No need for suspend, simple cleanup
    override fun deinitialize() {
        interpreter.close()
        // gpuDelegate?.close() // Close delegate if used
        // gpuDelegate = null
        isInitializedInternal = false
        Log.d(TAG, "WhisperEngine deinitialized.")
    }

    // Make suspend as spectrogram and inference can be slow
    override suspend fun transcribeBuffer(samples: FloatArray): String? {
         // Switch to Default dispatcher for CPU-bound tasks
         return transcribeBufferInternal(samples)
    }

    // Internal function to run on background dispatcher
    private suspend fun transcribeBufferInternal(samples: FloatArray): String? {
         return withContext(Dispatchers.Unconfined) {
             if (!isInitializedInternal || interpreter == null) {
                Log.e(TAG, "Engine not initialized, cannot transcribe.")
                return@withContext null
            }
            Log.d(TAG, "Calculating Mel spectrogram for ${samples.size} samples...")
            // Use suspend version of getMelSpectrogram
             Log.d(TAG, "Calculating Mel spectrogram for: "+Arrays.toString(samples.copyOfRange(samples.size - 500, samples.size)))
            val melSpectrogram = getMelSpectrogram(samples)
            if (melSpectrogram == null) {
                Log.e(TAG, "Failed to calculate Mel spectrogram.")
                return@withContext null
            }
             Log.d(TAG, "Mel spectrogram calculated. Running inference...")
            val x = runInference(melSpectrogram)
             Log.d(TAG,"Returned result from runInference: $x")
             x
         }
    }

    // Keep internal, runs on IO dispatcher from initialize
    @Throws(IOException::class)
    private fun loadModel(modelPath: String) {
        val fileInputStream = FileInputStream(modelPath)
        val fileChannel = fileInputStream.channel
        val startOffset = 0L
        val declaredLength = fileChannel.size()
        val tfliteModel = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        fileChannel.close()
        fileInputStream.close()

        val options = Interpreter.Options().apply {
            numThreads = Runtime.getRuntime().availableProcessors() // Use available cores
             // TODO: Add delegate logic here (GPU/NNAPI) based on config/availability checks
             // Example (needs refinement and async checks):
             // val nnApiDelegate = NnApiDelegate()
             // addDelegate(nnApiDelegate)
             // setUseNNAPI(true)
        }

        interpreter = Interpreter(tfliteModel, options)
        Log.i(TAG, "TFLite Interpreter created with ${options.numThreads} threads.")
    }

     // Keep internal, make suspend as util function is suspend
     private suspend fun getMelSpectrogram(samples: FloatArray): FloatArray? {
        // REMOVE Padding/truncation of input samples here
         val fixedInputSize = WhisperUtil.WHISPER_SAMPLE_RATE * WhisperUtil.WHISPER_CHUNK_SIZE
         val inputSamples = FloatArray(fixedInputSize)
         val copyLength = minOf(samples.size, fixedInputSize)
         System.arraycopy(samples, 0, inputSamples, 0, copyLength)

        // Call the suspend function from WhisperUtil with the ORIGINAL samples and length
        return whisperUtil.getMelSpectrogram(inputSamples, inputSamples.size, 8) // Use actual samples.size
    }

    // Keep internal, runs on Default dispatcher via transcribeBufferInternal
    // Keep internal, runs on Default dispatcher via transcribeBufferInternal
    private fun runInference(inputMelSpectrogram: FloatArray): String? {
        if (!isInitializedInternal) {
            Log.e(TAG, "Engine not initialized, cannot transcribe.")
            return null
        }

        // Check for empty input
        if (inputMelSpectrogram.isEmpty()) {
            Log.w(TAG, "Input spectrogram is empty.")
            return ""
        }

        try {
            // --- Create Input Tensor ---
            val inputTensor = interpreter.getInputTensor(0)
            val inputBuffer = TensorBuffer.createFixedSize(inputTensor.shape(), inputTensor.dataType())

            // --- Create Output Tensor ---
            val outputTensor = interpreter.getOutputTensor(0)
            val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), DataType.FLOAT32)

            // Create a ByteBuffer for input data
            val inputSize = inputTensor.shape()[0] * inputTensor.shape()[1] * inputTensor.shape()[2] * Float.SIZE_BYTES
            val inputBuf = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())

            for (i in inputMelSpectrogram) {
                inputBuf.putFloat(i)
            }

            // Load the buffer into the TensorBuffer
            inputBuffer.loadBuffer(inputBuf)

            // --- Run Inference ---
            Log.d(TAG, "Running inference...")
            interpreter.run(inputBuffer.buffer, outputBuffer.buffer)
            Log.d(TAG, "Inference complete")

            // --- Process Output ---
            // Get the output length
            val outputLen = outputBuffer.intArray.size
            Log.d(TAG, "Output length: $outputLen tokens")

            // Build the result string by sequentially reading tokens
            val result = StringBuilder()
            var hasContent = false

            for (i in 0 until outputLen) {
                // CRITICAL: Use getInt() which sequentially reads and advances position
                // This is identical to how WhisperEngineLegacy processes tokens
                val token = outputBuffer.buffer.getInt()
                Log.v(TAG, "Token[$i]: $token")

                // End of transcript detection
                if (token == whisperUtil.tokenEOT) {
                    Log.d(TAG, "End-of-Transcript token found.")
                    break
                }

                // Process regular words
                if (token < whisperUtil.tokenEOT) {
                    val word = whisperUtil.getWordFromToken(token)
                    if (word != null) {
                        Log.v(TAG, "Word: '$word'")
                        result.append(word)
                        hasContent = true
                    } else {
                        Log.w(TAG, "Unknown token ID: $token")
                        result.append("[UNK:$token]")
                    }
                } else {
                    // Handle special tokens (similar to legacy implementation)
                    if (token == whisperUtil.tokenTranscribe)
                        Log.d(TAG, "It is Transcription...")

                    if (token == whisperUtil.tokenTranslate)
                        Log.d(TAG, "It is Translation...")

                    val word = whisperUtil.getWordFromToken(token)
                    Log.d(TAG, "Skipping special token: $token, word: $word")
                }
            }

            val finalResult = result.toString().trim()
            Log.i(TAG, "Transcription result: '$finalResult'")

            // Return null only if result is completely empty
            return if (finalResult.isBlank() && !hasContent) null else finalResult
        } catch (e: Exception) {
            Log.e(TAG, "Error during inference: ${e.message}", e)
            return null
        }
    }

    // Keep for debugging if needed
    private fun printTensorDump(message: String, tensor: Tensor) {
        // ... (Implementation unchanged) ...
         Log.d(TAG,"Output Tensor Dump ===>")
        Log.d(TAG, "  shape.length: " + tensor.shape().size);
        for (i in 0 until tensor.shape().size)
            Log.d(TAG, "    shape[" + i + "]: " + tensor.shape()[i]);
        Log.d(TAG, "  dataType: " + tensor.dataType());
        Log.d(TAG, "  name: " + tensor.name());
        Log.d(TAG, "  numBytes: " + tensor.numBytes());
        Log.d(TAG, "  index: " + tensor.index());
        Log.d(TAG, "  numDimensions: " + tensor.numDimensions());
        Log.d(TAG, "  numElements: " + tensor.numElements());
        Log.d(TAG, "  shapeSignature.length: " + tensor.shapeSignature().size);
        Log.d(TAG, "  quantizationParams.getScale: " + tensor.quantizationParams().scale);
        Log.d(TAG, "  quantizationParams.getZeroPoint: " + tensor.quantizationParams().zeroPoint);
        Log.d(TAG, "==================================================================");
    }
}
