package eu.pretix.pretixdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import eu.pretix.pretixdroid.net.api.PretixApi;

public class AppConfig {
    public static final String PREFS_NAME = "pretixdroid";
    public static final String PREFS_KEY_API_URL = "pretix_api_url";
    public static final String PREFS_KEY_API_KEY = "pretix_api_key";
    public static final String PREFS_KEY_API_VERSION = "pretix_api_version";
    public static final String PREFS_KEY_FLASHLIGHT = "flashlight";
    public static final String PREFS_KEY_AUTOFOCUS = "autofocus";
    public static final String PREFS_KEY_CAMERA = "camera";
    public static final String PREFS_KEY_ASYNC_MODE = "async";
    public static final String PREFS_PLAY_AUDIO = "playaudio";
    public static final String PREFS_KEY_LAST_SYNC = "last_sync";
    public static final String PREFS_KEY_LAST_FAILED_SYNC = "last_failed_sync";
    public static final String PREFS_KEY_LAST_FAILED_SYNC_MSG = "last_failed_sync_msg";
    public static final String PREFS_KEY_LAST_DOWNLOAD = "last_download";
    private SharedPreferences prefs;
    private SharedPreferences default_prefs;

    public AppConfig(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        default_prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public boolean isConfigured() {
        return prefs.contains(PREFS_KEY_API_URL);
    }

    public void setEventConfig(String url, String key, int version) {
        prefs.edit()
                .putString(PREFS_KEY_API_URL, url)
                .putString(PREFS_KEY_API_KEY, key)
                .putInt(PREFS_KEY_API_VERSION, version)
                .remove(PREFS_KEY_LAST_DOWNLOAD)
                .remove(PREFS_KEY_LAST_SYNC)
                .remove(PREFS_KEY_LAST_FAILED_SYNC)
                .apply();
    }

    public void resetEventConfig() {
        prefs.edit()
                .remove(PREFS_KEY_API_URL)
                .remove(PREFS_KEY_API_KEY)
                .remove(PREFS_KEY_API_VERSION)
                .remove(PREFS_KEY_LAST_DOWNLOAD)
                .remove(PREFS_KEY_LAST_SYNC)
                .remove(PREFS_KEY_LAST_FAILED_SYNC)
                .apply();
    }

    public int getApiVersion() {
        return prefs.getInt(PREFS_KEY_API_VERSION, PretixApi.SUPPORTED_API_VERSION);
    }

    public String getApiUrl() {
        return prefs.getString(PREFS_KEY_API_URL, "");
    }

    public String getApiKey() {
        return prefs.getString(PREFS_KEY_API_KEY, "");
    }

    public boolean getFlashlight() {
        return prefs.getBoolean(PREFS_KEY_FLASHLIGHT, false);
    }

    public boolean getAutofocus() {
        return default_prefs.getBoolean(PREFS_KEY_AUTOFOCUS, true);
    }

    public boolean getCamera() {
        return default_prefs.getBoolean(PREFS_KEY_CAMERA, true);
    }

    public boolean getSoundEnabled() {
        return default_prefs.getBoolean(PREFS_PLAY_AUDIO, true);
    }

    public void setFlashlight(boolean val) {
        prefs.edit().putBoolean(PREFS_KEY_FLASHLIGHT, val).apply();
    }

    public void setAutofocus(boolean val) {
        prefs.edit().putBoolean(PREFS_KEY_AUTOFOCUS, val).apply();
    }

    public void setSoundEnabled(boolean val) {
        default_prefs.edit().putBoolean(PREFS_PLAY_AUDIO, val).apply();
    }

    public void setCamera(boolean val) {
        default_prefs.edit().putBoolean(PREFS_KEY_CAMERA, val).apply();
    }

    public boolean getAsyncModeEnabled() {
        return default_prefs.getBoolean(PREFS_KEY_ASYNC_MODE, false);
    }

    public void setAsyncModeEnabled(boolean val) {
        default_prefs.edit().putBoolean(PREFS_KEY_ASYNC_MODE, val).apply();
    }

    public long getLastDownload() {
        return prefs.getLong(PREFS_KEY_LAST_DOWNLOAD, 0);
    }

    public void setLastDownload(long val) {
        prefs.edit().putLong(PREFS_KEY_LAST_DOWNLOAD, val).apply();
    }

    public long getLastSync() {
        return prefs.getLong(PREFS_KEY_LAST_SYNC, 0);
    }

    public void setLastSync(long val) {
        prefs.edit().putLong(PREFS_KEY_LAST_SYNC, val).apply();
    }

    public long getLastFailedSync() {
        return prefs.getLong(PREFS_KEY_LAST_FAILED_SYNC, 0);
    }

    public void setLastFailedSync(long val) {
        prefs.edit().putLong(PREFS_KEY_LAST_FAILED_SYNC, val).apply();
    }

    public String getLastFailedSyncMsg() {
        return prefs.getString(PREFS_KEY_LAST_FAILED_SYNC_MSG, "");
    }

    public void setLastFailedSyncMsg(String val) {
        prefs.edit().putString(PREFS_KEY_LAST_FAILED_SYNC_MSG, val).apply();
    }
}
