package ai.liv.s2tlibrary;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import android.util.Log;

import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static ai.liv.s2tlibrary.S2TConstants.LOG_TAG;
import static ai.liv.s2tlibrary.S2TConstants.PREF_TIMER_INTERVAL_VAL;
import static ai.liv.s2tlibrary.S2TConstants.State;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import ai.flox.asr.Whisper;

public class S2TAudioRecorder {

    private Boolean should_stop;

    private boolean sendTranscription = true;

    private boolean oneRun;

    private static final String TAG = "S2TAudioRecorder";
    private static final String ISSUE_TAG = "IssueTag";

    private final static int[] sampleRates = {16000,8000};


    // The interval (in millisec) in which the recorded samples are output to the file
    private int listenerTimerInterval = 1000;

    private int numFramesInASecond;

    // The interval (in millisec) in which recording must be split and sent to server
    private int splitAudioDurationInMillis ;

    private int timerInterval;

    // Audio Recorder instance
    private AudioRecord audioRecorder;

    //VAD instance
    private S2TVAD vad = null;

    private boolean vadOn = false;

    private boolean splitFlag = true;

    private State state;

    // Current recording index
    private int rec_idx;

    // Number of channels, sample rate, sample size(size in bits), buffer size, audio source, sample size(see AudioFormat)
    private short                    nChannels;
    private int                      sRate;
    private short                    bSamples;
    private int                      bufferSize;
    private int                      aSource;
    private int                      aFormat;

    // Number of frames written to file on each output(only in uncompressed mode)
    private int                      framePeriod;

    // Buffer for output(only in uncompressed mode)
    private float[]                   buffer;

    // ByteBuffer storing recorded bytes for TIMER_INTERVAL+EXTRA_TIMER_INTERVAL
    private FloatBuffer fileBuffer;

    // Number of times bytes have been transferred from buffer to ByteBuffer
    private int readDataCount;

    private Context mContext;
    double bufferLengthSum = 0;

    private Whisper mWhisper;

    private int initializationRetryCount = 0;

    private static final int MAX_INITIALIZATION_RETRY = 3;

    //==============================================================================================
    // Audio Recorder instantiation
    //==============================================================================================

    @Nullable
    public static S2TAudioRecorder getInstance(Context c, Whisper whisper)
    {

        S2TAudioRecorder recorder;
        int i=0;
        do
        {
            recorder = new S2TAudioRecorder(c,
                    MediaRecorder.AudioSource.MIC,
                    sampleRates[i],
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    whisper
                    );

        } while((++i<sampleRates.length) & !(recorder.state == State.INITIALIZING));

        return recorder;
    }

    /**
     *
     *
     * Default constructor
     *
     * Instantiates a new recorder, in case of compressed recording the parameters can be left as 0.
     * In case of errors, no exception is thrown, but the state is set to ERROR
     *
     */
    S2TAudioRecorder(Context c, int audioSource, int sampleRate, int channelConfig, int audioFormat, Whisper whisper)
    {
        mContext = c;
        mWhisper = whisper;

        timerInterval = S2TUtils.getFromSharedPref(mContext, S2TConstants.PREF_TIMER_INTERVAL, PREF_TIMER_INTERVAL_VAL);
        try
        {
            bSamples = 32;
            nChannels = 1;
            aSource = audioSource;
            sRate   = sampleRate;
            aFormat = audioFormat;
            numFramesInASecond = 1000/listenerTimerInterval;

            framePeriod = sampleRate * listenerTimerInterval/1000;

            bufferSize = framePeriod * bSamples * nChannels / 8;

            if(splitFlag){
                splitAudioDurationInMillis =S2TConstants.BREAK_INTERVAL;
            }
            else{
                splitAudioDurationInMillis = timerInterval*1000;
            }

            int fullBufferSize = bufferSize * ((timerInterval+2) * 1000 / listenerTimerInterval + 1);
            fileBuffer = FloatBuffer.allocate(fullBufferSize);

            Log.d(TAG,"bufferSize"+bufferSize);
            if (bufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat))
            {
                // Check to make sure buffer size is not smaller than the smallest allowed one
                bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

                // Set frame period and timer interval accordingly
                framePeriod = bufferSize / (bSamples * nChannels / 8 );

            }

            Log.d(TAG,"bufferSize"+bufferSize+","+framePeriod);
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            audioRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize*numFramesInASecond*timerInterval);

            if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED)
                throw new Exception("AudioRecord initialization failed");


            state = State.INITIALIZING;
            if(vadOn) {
                vad = new S2TVAD(sRate);
            }
        } catch (Exception e)
        {
            if (e.getMessage() != null)
            {
                Log.e(S2TConstants.LOG_TAG, e.getMessage());
            }
            else
            {
                Log.e(S2TConstants.LOG_TAG, "Unknown error occured while initializing recording");
            }
            state = State.ERROR;
        }
    }

    /**
     * Callback triggered after regular intervals to read from audioRecorder to local buffer
     */
    @NonNull
    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener()
    {
        public void onPeriodicNotification(AudioRecord recorder)
        {
            if(sendTranscription) {
                readDataFromBuffer();
                //new ReadTask().execute();
            }
        }

        public void onMarkerReached(AudioRecord recorder)
        {
            // NOT USED
        }
    };

    long getAnimationRefreshTime(){
        return listenerTimerInterval;       //Currently refresh everytime the buffer copies, can be edited later
    }

    private synchronized void init() {
        
        oneRun = false;
        rec_idx = 0;
        readDataCount = 0;
        
        audioRecorder.setRecordPositionUpdateListener(updateListener);
        audioRecorder.setPositionNotificationPeriod(framePeriod);
        should_stop = false;
        sendTranscription = true;

    }
    /**
     *
     * Prepares the recorder for recording, in case the recorder is not in the INITIALIZING state and the file path was not set
     * the recorder is set to the ERROR state, which makes a reconstruction necessary.
     * In case uncompressed recording is toggled, the header of the wave file is written.
     * In case of an exception, the state is changed to ERROR
     *
     */
    public void prepare()
    {
        try
        {
            ++initializationRetryCount;

                if (audioRecorder.getState() == AudioRecord.STATE_INITIALIZED)
                {
                    buffer = new float[framePeriod*bSamples/8*nChannels];

                    timerInterval = S2TUtils.getFromSharedPref(mContext, S2TConstants.PREF_TIMER_INTERVAL, PREF_TIMER_INTERVAL_VAL);
                    if(splitFlag){
                        splitAudioDurationInMillis = S2TConstants.BREAK_INTERVAL;
                    }
                    else{
                        splitAudioDurationInMillis = timerInterval*1000;
                    }
                    int fullBufferSize = bufferSize * ((timerInterval+2) * 1000 / listenerTimerInterval + 1);
                    fileBuffer = FloatBuffer.allocate(fullBufferSize);
                    state = State.READY;
                } else {
                    if ((audioRecorder.getState() == AudioRecord.STATE_UNINITIALIZED) && (initializationRetryCount < MAX_INITIALIZATION_RETRY)) {
                        audioRecorder.release();
                        reset(true);
                        prepare();
                    } else {
                        Log.e(LOG_TAG, "prepare() method called on uninitialized recorder");
                        state = State.ERROR;
                    }
                }
        }
        catch(Exception e)
        {
            if (e.getMessage() != null)
            {
                Log.e(LOG_TAG, e.getMessage());
            }
            else
            {
                Log.e(LOG_TAG, "Unknown error occured in prepare()");
            }
            state = State.ERROR;
        }
    }

    /**
     *
     *
     * Starts the recording, and sets the state to RECORDING.
     * Call after prepare().
     *
     */
    public void start() {
        if (state == State.READY) {
            init();

            //Alternating files prefixes to check race condition
            int fileNamePrefix = S2TUtils.getFromSharedPref(mContext, S2TConstants.FILE_NAME_PREFIX_KEY, 1);
            if (fileNamePrefix == 1) {
                S2TUtils.saveToSharedPref(mContext, S2TConstants.FILE_NAME_PREFIX_KEY, 2);
            } else {
                S2TUtils.saveToSharedPref(mContext, S2TConstants.FILE_NAME_PREFIX_KEY, 1);
            }

            audioRecorder.startRecording();
            readDataFromBuffer();
            state = State.RECORDING;
        } else {
            Log.e(LOG_TAG, "start() called on illegal state");
            state = State.ERROR;
        }
    }

    /**
     *
     *
     *  Stops the recording, and sets the state to STOPPED.
     * In case of further usage, a reset is needed.
     * Also finalizes the wave file in case of uncompressed recording.
     *
     */
    public void stop()
    {
        if (state == State.RECORDING) {
            try{
                audioRecorder.stop();
                audioRecorder.release();
            }catch (Exception e){
                Log.e(LOG_TAG, "Exception while releasing audioRecorder");
            }
            
            audioRecorder.setRecordPositionUpdateListener(null);
            should_stop = true;
            readDataFromBuffer();
            state = State.STOPPED;
        }
        else {
            Log.e(LOG_TAG, "stop() called on illegal state "+state);
        }
    }

    /**
     *
     *
     *  Releases the resources associated with this class, and removes the unnecessary files, when necessary
     *
     */
    public void release()
    {
        //Log.v(TAG, "Stopping rec");
        if (state == State.RECORDING) {
            stop();
        }

        if (audioRecorder != null) {
            audioRecorder.release();
        }
    }

    /**
     *
     *
     * Resets the recorder to the INITIALIZING state, as if it was just created.
     * In case the class was in RECORDING state, the recording is stopped.
     * In case of exceptions the class is set to the ERROR state.
     *
     */
    public void reset(boolean sendTranscription)
    {
        try
        {
            if (state != State.ERROR)
            {
                this.sendTranscription = sendTranscription;
                release();
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                audioRecorder = new AudioRecord(aSource, sRate, nChannels+1, aFormat, bufferSize*numFramesInASecond);
                state = State.INITIALIZING;
            }
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, e.getMessage());
            state = State.ERROR;
        }
    }

    


    //==============================================================================================
    // Buffer Read and write methods
    //==============================================================================================
    private synchronized void readDataFromBuffer() {

        if (oneRun) {
            return;
        }
        int bytesRead = audioRecorder.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING); // Fill buffer
        int sum = 0;

        for(int i = 0; i < buffer.length; i++) {
            sum += buffer[i] * buffer[i];
        }
        // Log.d(TAG,"copy+sum end:"+System.currentTimeMillis());
        bufferLengthSum += buffer.length;

        double amplitude = sum/bufferLengthSum;
        bufferLengthSum = 0;
        try{
            // Log.d(TAG, "readDataFromBuffer"+Arrays.toString(buffer));
            fileBuffer.put(buffer);
        }catch (@NonNull BufferOverflowException | ReadOnlyBufferException ex){
            // ErrorHelper.getInstance().sendError(S2TError.ERROR_AUDIO_BUFFER, ex.getMessage());
            return;
        }

        readDataCount++;

        if(vadOn) {
            if (vad.non_speech_detected) {
                should_stop = true;
            } else {
                new FFTTask(FloatBuffer.wrap(buffer)).execute(null, null, null);
            }
        }
        // Log.d(TAG,"fileBuffer.put end:"+System.currentTimeMillis());
        if (should_stop || readDataCount >= splitAudioDurationInMillis/listenerTimerInterval) {//

            rec_idx++;

            if (timerInterval*1000 - splitAudioDurationInMillis*rec_idx <= 0) {
                should_stop = true;
            }

            if(sendTranscription) {

                int start = 0, end = fileBuffer.position();

                //Just to get 4 frame data
                int sizeOfBuffer = bufferSize*(splitAudioDurationInMillis/listenerTimerInterval);

                Log.d(TAG,"fileBuffer.position():"+fileBuffer.position()+", sizeofBuffer:"+sizeOfBuffer);
                if(fileBuffer.position() >=  sizeOfBuffer) {
                    if (splitFlag) {
                        start = (rec_idx - 1) * (sizeOfBuffer);
                        if (!should_stop) {
                            end = ((rec_idx) * (sizeOfBuffer));
                        }
                    }
                    ReadBufferTask rb = new ReadBufferTask(rec_idx);
                    rb.execute(0,end);
                }else {
                    // ErrorHelper.getInstance().sendError(S2TError.ERROR_IN_AUDIO);
                }
            }
            readDataCount = 0;
            if (should_stop) {
                oneRun = true;
                if(vadOn) {
                    vad.reset_vad_params();
                }
            }
        }
    }


    //==============================================================================================
    // Local Utilities
    //==============================================================================================

    // Interface exposed to RecordFragment for triggering onStop()
    interface S2TAudioRecorderCallbacks{
        void onStopRecording();
        void onResponseFromServer(String[] text_list,JSONObject intent, double[] confidence_list);
        void onAmplitudeChanged(double amplitude);
    }


    private class FFTTask extends AsyncTask<String, String, String> {
        FloatBuffer bb;
        FFTTask(FloatBuffer bb){
            this.bb = bb;
        }
        @Nullable
        @Override
        protected String doInBackground(String... params) {
            // vad.calcAndStop(this.bb);
            return null;
        }
    }

    private class ReadBufferTask extends AsyncTask<Integer, Void, Void> {
        int r_idx;
        public ReadBufferTask(int id) {
            super();
            r_idx = id;
        }

        public float[] byteToFloat (byte[] byteArray){
            float[] floatOut = new float[byteArray.length / 4];
            ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(floatOut);
            return floatOut;
        }

        @Nullable
        @Override
        protected Void doInBackground(Integer... params) {

            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
            int start = params[0];
            int end = params[1];
            //int start = 0;
            //int end = 15361;
            /*final byte[] array = fileBuffer.array();
            final int arrayOffset = fileBuffer.arrayOffset();
            byte[] temp = Arrays.copyOfRange(array, arrayOffset,
                    arrayOffset + fileBuffer.position());
            */

            final float[] array = fileBuffer.array();

            // Log.v(TAG,"start:"+start+",end:"+end +"fileBuffer.position():"+fileBuffer.position());

            //Method1
            final int arrayOffset = fileBuffer.arrayOffset();
            float[] temp = Arrays.copyOfRange(array, arrayOffset+start, arrayOffset+end);
            Log.v(TAG,"temp:"+Arrays.toString(temp));
            mWhisper.transcribeBuffer(temp);

            //fileBuffer.clear();       TODO

            if(should_stop) {
                try{
                    publishProgress();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    private class ResponseTimeout implements Runnable{

        private boolean killMe = false;
        private String appSessionId;

        ResponseTimeout(String appSessionId){
            this.appSessionId = appSessionId;
        }

        @Override
        public void run(){
            if(!killMe) {
                if(appSessionId.equalsIgnoreCase(appSessionId)) {
                    // ErrorHelper.getInstance().sendError(S2TError.ERROR_NO_INTERNET_TIMEOUT);
                    killMe = true;
                }
            }
        }

        void killTimeout(){
            killMe = true;
        }
    }

    private class ReadTask extends AsyncTask<String, String, String> {
        @Nullable
        @Override
        protected String doInBackground(String... params) {
            readDataFromBuffer();
            return null;
        }
    }
}


