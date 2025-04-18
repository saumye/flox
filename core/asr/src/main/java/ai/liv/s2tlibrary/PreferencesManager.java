package ai.liv.s2tlibrary;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by saumyesrivastava on 06/09/17.
 */

public class PreferencesManager {

    private static PreferencesManager sInstance;
    private final SharedPreferences mPref;
    private static final String PREF_NAME = "ai.liv.s2tlibrary.ALL_PREFS";

    private PreferencesManager(Context context) {
        mPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized void initializeInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PreferencesManager(context);
        }
    }

    public static synchronized PreferencesManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException(PreferencesManager.class.getSimpleName() +
                    " is not initialized, call initializeInstance(..) method first.");
        }
        return sInstance;
    }

    protected boolean saveToSharedPref(final String key, final String value) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    protected boolean saveToSharedPref(final String key, final Boolean value) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    protected boolean saveToSharedPref(final String key, final int value) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putInt(key, value);
        return editor.commit();
    }

    protected boolean removeFromSharedPref(final String key) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.remove(key);
        return editor.commit();
    }

    protected Boolean getFromSharedPref(String key, Boolean defaultValue) {
        return mPref.getBoolean(key, defaultValue);
    }

    protected String getFromSharedPref(String key, String defaultValue) {
        return mPref.getString(key, defaultValue);
    }

    protected int getFromSharedPref(String key, int defaultValue) {
        return mPref.getInt(key, defaultValue);
    }

    protected int saveToSetInSharedPref(final String setKey, final String appSessionId, final int appSessionStatus, final String timestamp) {
        Set<String> set = mPref.getStringSet(setKey, new HashSet<String>());
        if(set.size() <= S2TConstants.MAX_EVENT_SIZE) {
            String data = appSessionId + "," + appSessionStatus + "," + timestamp;
            set.add(data);
            SharedPreferences.Editor editor = mPref.edit();
            editor.putStringSet(setKey, set);
            editor.commit();
        }
        return set.size();
    }

    protected int saveSetInSharedPref(Set<String> set, final String setKey) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putStringSet(setKey, set);
        editor.commit();
        return set.size();
    }


    protected Set<String> getSetFromSharedPref(final String setKey) {
        return mPref.getStringSet(setKey, new HashSet<String>());
    }

    protected boolean removeFromSetInSharedPref(final String setKey, final String key, final int value) {
        Set<String> set = mPref.getStringSet(setKey, new HashSet<String>());

        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String appSessionAndStatus = iterator.next();
            if(appSessionAndStatus.equalsIgnoreCase(key+","+value));{
                iterator.remove();
            }
        }

        SharedPreferences.Editor editor = mPref.edit();
        editor.putStringSet(key, set);
        return editor.commit();
    }

}
