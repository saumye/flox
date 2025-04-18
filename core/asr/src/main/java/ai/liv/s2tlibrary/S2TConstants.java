package ai.liv.s2tlibrary;

/**
 * Created by garima on 18/12/15.
 */
public class S2TConstants {

    /**
     * Preference Keys
     */

    protected static final String PREF_APP_USER_ID = "s2t_pref_app_user_ID";
    protected static final String PREF_LANG = "s2t_pref_language";
    protected static final String PREF_FORMAT = "s2t_pref_format";
    protected static final String PREF_TIMER_INTERVAL = "s2t_pref_timer_interval";
    protected static final String PREF_BASE_URL = "s2t_pref_base_url";
    protected static final String PREF_INTENT_FLAG = "s2t_pref_intent";
    protected static final String PREF_CONTINUOUS_FLAG = "continuousFlag";
    protected static final String PREF_SPLIT_FLAG = "splitFlag";
    protected static final String PREF_APP_VERSION = "s2t_pref_app_version";
    protected static final String PREF_PENDING_STATUSES_SET = "s2t_pref_pending_statuses_set";
    protected static final String PREF_OVERRIDE_LANGUAGES = "s2t_pref_override_languages";
    protected static final String PREF_EXTRA_LANGUAGE = "s2t_pref_extra_language";

    /**
     * Preference Default values
     */
    protected static String PREF_BASE_URL_VAL = "https://dev.liv.ai/liv_transcription_api/";
    protected static final boolean PREF_DEFAULT_CONTINUOUS_FLAG = false;
    protected static final boolean PREF_DEFAULT_SPLIT_FLAG = true;
    protected static final boolean PREF_DEFAULT_INTENT_FLAG = false;
    protected static final int TIMEOUT_IN_MILLIS = 14880;//Multiple of 480 to omit padding in the end.

    /**
     * Timers
     */
    protected static final int PREF_TIMER_INTERVAL_VAL = 15;
    protected static final int BREAK_INTERVAL = 5000;

    /**
     *  Endpoints
     */
    protected static final String RECORDING_PATH = "recordings/";
    protected static final String APPSESSION_PATH = "sessions/";
    protected static final String USER_PATH = "appusers/";
    protected static final String EVENT_PATH = "events/";
    protected static final String EVENT_PATH_V2 = "events_v2/";

    /**
     *  Misc
     */
    public static final String LOG_TAG = "S2TLOG";
    protected static String INTENT_LAYOUT_EXTRA = "ai.liv.s2tlibrary.S2TService.INTENT_LAYOUT_EXTRA";
    protected static String INTENT_HEIGHT_EXTRA = "ai.liv.s2tlibrary.S2TService.INTENT_HEIGHT_EXTRA";


    /**
     * INITIALIZING : recorder is initializing;
     * READY : recorder has been initialized, recorder not yet started
     * RECORDING : recording
     * ERROR : reconstruction needed
     * STOPPED: reset needed
     */
    protected enum State {
        INITIALIZING,
        READY,
        RECORDING,
        ERROR,
        STOPPED
    }

    protected static final int DEFAULT_KEYBOARD_HEIGHT_IN_DP = 260;
    protected static final int MIN_KEYBOARD_HEIGHT_IN_DP = 220;
    protected static final int EVENT_BATCH_SIZE = 3;
    protected static final int MAX_EVENT_SIZE = 50;


    /**
    *   HockeyApp Events
     */
    protected static final String EVENT_TRANSCRIPTION_COMPLETE = "Transcription Complete";
    protected static final String EVENT_TRANSCRIPTION_COMPLETE_TAT_KEY = "TAT";
    protected static final String FILE_NAME_PREFIX_KEY = "file_name_prefix_key";

    protected static final String EVENT_TRANSCRIPTION_COMPLETE_RECORDING_LENGTH_KEY = "Rec Length";
    protected static final String EVENT_CLOSE_CLICKED_BEFORE_STOP = "Close Clicked before Stop";
    protected static final String EVENT_CLOSE_CLICKED_AFTER_STOP = "Close Clicked after Stop";

    protected static final String EVENT_ERROR_THROWN = "Error Thrown";
    protected static final String EVENT_ERROR_THROWN_TYPE_KEY = "Error type";

    /**
        Events to be tracked for each App Session
    **/
    public static final int EVENT_CLOSE_CLICKED_BEFORE_TRANSACTION = 200;
    public static final int EVENT_CLOSE_CLICKED_AFTER_TRANSACTION = 201;
    public static final int EVENT_DONE_CLICKED = 202;
    public static final int EVENT_FULL_RECORDING_FINISHED = 203;
    public static final int EVENT_TRANSCRIPTION_RECEIVED = 204;

}
