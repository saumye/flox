package ai.liv.s2tlibrary;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


import static ai.liv.s2tlibrary.S2TConstants.LOG_TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by garima on 18/12/15.
 */
final public class S2TUtils {

    private static String TAG = S2TUtils.class.getName();

    static protected String convertDateToString(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String dateString = df.format(date);
        return dateString;
    }

    static protected Date convertStringToDate(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Date convertedDate = new Date();
        try {
            convertedDate = dateFormat.parse(dateString);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return convertedDate;
    }

    static protected boolean saveToSharedPref(@NonNull final Context c, final String key, final String value) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    static protected boolean saveToSharedPref(@NonNull final Context c, final String key, final Boolean value) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    static protected boolean saveToSharedPref(@NonNull final Context c, final String key, final int value) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(key, value);
        return editor.commit();
    }

    static protected boolean removeFromSharedPref(@NonNull final Context c, final String key) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(key);
        return editor.commit();
    }

    static protected Boolean getFromSharedPref(@NonNull final Context c, String key, Boolean defaultValue) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
        return pref.getBoolean(key, defaultValue);
    }

    @Nullable
    static protected String getFromSharedPref(@NonNull final Context c, String key, String defaultValue) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
        return pref.getString(key, defaultValue);
    }

    static protected int getFromSharedPref(@NonNull final Context c, String key, int defaultValue) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
        return pref.getInt(key, defaultValue);
    }

    static protected int saveToSetInSharedPref(@NonNull final Context c, final String setKey, final String appSessionId, final int appSessionStatus, final String timestamp) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
        Set<String> set = pref.getStringSet(setKey, new HashSet<String>());
        if(set.size() <= S2TConstants.MAX_EVENT_SIZE) {
            String data = appSessionId + "," + appSessionStatus + "," + timestamp;
            set.add(data);
            Log.d(TAG,"putting:"+data+",size:"+set.size());
            SharedPreferences.Editor editor = pref.edit();
            editor.putStringSet(setKey, set);
            editor.commit();
        }
        return set.size();
    }

    static protected int saveSetInSharedPref(@NonNull final Context c, Set<String> set, final String setKey) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.putStringSet(setKey, set);
        editor.commit();
        return set.size();
    }


    static protected Set<String> getSetFromSharedPref(@NonNull final Context c, final String setKey) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
        return pref.getStringSet(setKey, new HashSet<String>());
    }

    static protected boolean removeFromSetInSharedPref(@NonNull final Context c, final String setKey, final String key, final int value) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
        Set<String> set = pref.getStringSet(setKey, new HashSet<String>());

        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String appSessionAndStatus = iterator.next();
            if(appSessionAndStatus.equalsIgnoreCase(key+","+value)){
                iterator.remove();
            }
        }

        SharedPreferences.Editor editor = pref.edit();
        editor.putStringSet(key, set);
        return editor.commit();
    }

    static protected int getUserID(@NonNull final Context c) {
        return S2TUtils.getFromSharedPref(c, S2TConstants.PREF_APP_USER_ID, -1);
    }

    @NonNull
    static protected String decodeData(String s) {
        String finalString = "";
        try {
            byte[] text_bytes = Base64.decode(s, Base64.URL_SAFE);
            finalString = new String(text_bytes, "UTF-8");
        } catch (IllegalArgumentException e) {
            Log.e("Decode data Error: ", "IllegalArgumentException");
            return "";
        } catch (UnsupportedEncodingException e) {
            Log.e("Decode data Error: ", "UnsupportedEncodingException");
            return "";
        }

        return finalString;
    }


    static protected double standardDeviation(@NonNull byte[] b) {
        double average = 0, sd = 0;
        double average_square = 0;
        ByteBuffer bb = ByteBuffer.wrap(b);

        int len = b.length/2;
        for (int i=0; i<len; i++) {
            short num = bb.getShort();
            double dnum = (double)num;
            average += dnum;
            average_square += (dnum * dnum);
        }
        average = average/len;
        sd = average_square/len - average*average;

        sd = Math.sqrt(sd);
        //Log.v("ApplicationUtils", "Length " + len + " Mean: " + average + " SD: " + sd + " Avg Sq: " + average_square);
        return sd;
    }

    @NonNull
    static protected String getCurrentMethodName() {
        return Thread.currentThread().getStackTrace()[3].getClassName() + "." + Thread.currentThread().getStackTrace()[3].getMethodName();
    }

    static protected String getDeviceInfo() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model;
        } else {
            return manufacturer + " " + model;
        }
    }


    static protected JSONArray getJSONArrayFromSet(Set<String> set){
        JSONArray arr = new JSONArray();
        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()){
            String appSessionAndStatus = iterator.next();
            String[] list = appSessionAndStatus.split(",");
            if(list.length == 3) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("app_session_id", list[0]);
                    obj.put("app_session_status", Integer.valueOf(list[1]));
                    obj.put("timestamp", Long.valueOf(list[2]));
                    Log.d(TAG,obj+"");
                    arr.put(obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return arr;
    }

    static protected void addAppSessionIdStatusToQueue(@NonNull final Context context, final String appSessionId, final int appSessionStatus, final long timestamp) {
        if(appSessionId == null || appSessionId.isEmpty()){
            return;
        }

        S2TUtils.saveToSetInSharedPref(context, S2TConstants.PREF_PENDING_STATUSES_SET, appSessionId,appSessionStatus, timestamp+"");
    }

    static protected float convertDpToPixel(float dp, @NonNull Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    @NonNull
    static protected TextView setTextProperlyForEachLanguage(@NonNull TextView textView, @NonNull String languageNick, @NonNull Context context){

        float scaledDensity = textView.getContext().getResources().getDisplayMetrics().scaledDensity;
        int textSizeInSP = (int)(textView.getTextSize()/scaledDensity);

        /*if (languageNick.equals("PB")) {
            textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "DroidSansFallback.ttf"));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,textSizeInSP+2);
            textView.setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.0f,  textView.getResources().getDisplayMetrics()), 0.5f);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        }
        else{*/
            textView.setTypeface(Typeface.DEFAULT);
            textView.setTypeface(textView.getTypeface(), Typeface.NORMAL);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,textSizeInSP);
        //}
        return textView;
    }


    static protected void promptUserToEnableWindowOverlay(@NonNull Context context){
        try {
            Toast.makeText(context, "Please scroll down to give us permission to draw overlay over other apps inside your Permission Manager", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.getApplicationInfo().packageName, null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }catch (Exception ex){
            return;
        }
    }

    static protected boolean isMiuiFloatWindowOpAllowed(@NonNull Context context) {
        final int version = Build.VERSION.SDK_INT;

        if (version >= Build.VERSION_CODES.KITKAT) {
            return checkOp(context, 24, context.getPackageName(), context.getApplicationInfo().uid); //See AppOpsManager.OP_SYSTEM_ALERT_WINDOW=24 /*@hide/
        } else {
            return (context.getApplicationInfo().flags & 1<<27) == 1;
        }
    }

    static protected boolean checkOp(Context context, int op, String packageName, int uid) {
        final int version = Build.VERSION.SDK_INT;

        if (version >= 19) {
            AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                int val = (Integer) invokeMethod(manager, "checkOp", op, uid, packageName);
                return (AppOpsManager.MODE_ALLOWED == val);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG,""+e.getMessage());
            }
        } else {
            Log.d(TAG,"Below API 19 cannot invoke!");
        }
        return false;
    }

    static protected Object invokeMethod(@NonNull Object receiver, String methodName, Object... methodArgs) throws Exception {
        Class<?>[] argsClass = new Class[] { int.class, int.class, String.class };
        Method method = receiver.getClass().getMethod(methodName, argsClass);
        return method.invoke(receiver, methodArgs);
    }


}
