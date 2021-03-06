package org.fruct.oss.getssupplement;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Andrey on 19.07.2015.
 */
public class Settings {
    public static void saveString(Context context, String settingsName, String value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(Const.PREFS_NAME, 0).edit();
        editor.putString(settingsName, value);
        editor.apply();
    }

    public static void saveBoolean(Context context, String settingsName, Boolean value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(Const.PREFS_NAME, 0).edit();
        editor.putBoolean(settingsName, value);
        editor.apply();
    }

    public static void saveCheckedStatus(Context context, int categoryId, boolean value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(Const.PREFS_NAME_CATEGORIES_CHECKED, 0).edit();
        editor.putBoolean(Integer.toString(categoryId), value);
        editor.apply();
    }

    public static void saveMapHash(Context context, String hash) {
        SharedPreferences.Editor editor = context.getSharedPreferences(Const.PREFS_MAP_HASH, 0).edit();
        editor.putString(Const.PREFS_MAP_HASH, hash);
        editor.apply();
    }

    public static String getToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Const.PREFS_NAME, 0);
        return sharedPreferences.getString(Const.PREFS_AUTH_TOKEN, null);
    }

    public static String getStorageDir(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Const.PREFS_NAME, 0);
        return sharedPreferences.getString(Const.PREF_STORAGE_PATH, null);
    }

    public static boolean getIsTrusted(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Const.PREFS_NAME, 0);
        return sharedPreferences.getBoolean(Const.PREFS_IS_TRUSTED_USER, false);
    }

    public static boolean getIsChecked(Context context, int categoryId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Const.PREFS_NAME_CATEGORIES_CHECKED, 0);
        return sharedPreferences.getBoolean(Integer.toString(categoryId), true);
    }

    public static String getMapHash(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Const.PREFS_MAP_HASH, 0);
        return sharedPreferences.getString(Const.PREFS_MAP_HASH, null);
    }
}
