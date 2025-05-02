package ai.liv.s2tlibrary;


/**
 * Created by saumyesrivastava on 06/09/17.
 */

public class OpusEncoder {/*
 implements AudioEncoder {

    private static final String TAG = "OpusEncoder";

    private static final int STATE_NONE = 0;
    private static final int STATE_STARTED = 1;
    private volatile int state = STATE_NONE;

    private static final String FILE_EXTENSION = ".opus";
    private Context mContext;

    private final int AUDIO_FRAME_TIME_IN_MILLIS = 60;

    private static long encoderObjectAddress;

    @NonNull
    private OpusTool opusTool = new OpusTool();

    private String filePath = null;
    private ByteBuffer fileBuffer;
    long profilingTime = 0;
    int count = 0;

    /*@Override
    public byte[] encodeBytes(@NonNull ByteBuffer buffer) {

        //TODO for Future v2 of encoder
        return new byte[0];
    }

    OpusEncoder(Context context){
        mContext = context;
    }

    private void writeAudioDataToOpus(ByteBuffer buffer, int size, String filePath) {

        ByteBuffer finalBuffer = ByteBuffer.allocateDirect(size);
        finalBuffer.put(buffer);
        finalBuffer.rewind();

        //write data to Opus file
        while (state == STATE_STARTED && finalBuffer.hasRemaining()) {
            int oldLimit = -1;
            if (finalBuffer.remaining() > fileBuffer.remaining()) {
                oldLimit = finalBuffer.limit();
                finalBuffer.limit(fileBuffer.remaining() + finalBuffer.position());
            }
            fileBuffer.put(finalBuffer);

            if (fileBuffer.position() == fileBuffer.limit()) {
                int length = fileBuffer.limit();

                int rst = opusTool.writeFrame(fileBuffer, length, filePath);

                if (rst != 0) {
                    fileBuffer.rewind();
                }
            }
            if (oldLimit != -1) {
                finalBuffer.limit(oldLimit);
            }
        }
    }


    private int writeAudioDataToFile(@NonNull ByteBuffer buffer, String filePath) {
        if (state != STATE_STARTED)
            return -1;

        buffer.rewind();
        int len = buffer.array().length;
        Log.d(TAG, "\n lengh of buffersize is " + len);

        if (len <= 0) {
            return -1;
        }

        if (len != AudioRecord.ERROR_INVALID_OPERATION) {
            try {
                long start = System.currentTimeMillis();
                writeAudioDataToOpus(buffer, len, filePath);
                long diff = System.currentTimeMillis()-start;
                Log.d("Profiling","writeFrame took:"+(diff));
                count++;
                profilingTime += diff;
            } catch (Exception e) {
                Log.e(LOG_TAG, "writeAudioDataToFile() threw error " + e.getMessage());
                return -1;
            }
            return 1;
        }
        return -1;
    }

    @Override
    public File encodeBytes(@NonNull ByteBuffer buffer, int recordingIndex, boolean addHeader, long sampleRate) {

        fileBuffer = ByteBuffer.allocateDirect(AUDIO_FRAME_TIME_IN_MILLIS*((int)sampleRate/1000)*2);// Should be 1920 for 16khz, 960 for 8khz, double size of LISTENER_TIMER_INTERVAL ms frame ,to accord with function writeFrame()

        //use opus format
        File file = S2TUtils.makeOutputFile(mContext,recordingIndex, FILE_EXTENSION);
        if(file == null){
            Log.d(TAG,"makeOutputFile is null");
            return null;
        }

        if (addHeader) {
            encoderObjectAddress = opusTool.startRecording(file.getAbsolutePath(), sampleRate, AUDIO_FRAME_TIME_IN_MILLIS);
            if (encoderObjectAddress == 0) {
                Log.d(TAG,"startRecording is null");
                return null;
            }
        }

        state = STATE_STARTED;
        writeAudioDataToFile(buffer, file.getAbsolutePath());
        return file;
    }

    public int stopEncoding() {
        if (state != STATE_STARTED)
            return -1;

        Log.d("Profiling","average time taken by writeFrame:"+profilingTime/count);
        state = STATE_NONE;
        opusTool.stopRecording();
        return 0;
    }

    @Override
    public int getFrameSizeInMillis() {
        return AUDIO_FRAME_TIME_IN_MILLIS;
    }

    public boolean isWorking() {
        return state != STATE_NONE;
    }

    public void release() {
        if (state != STATE_NONE) {
            stopEncoding();
        }
    } */

}
