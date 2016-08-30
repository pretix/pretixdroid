package eu.pretix.pretixdroid;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfig {
    public static final String PREFS_NAME = "pretixdroid";
    public static final String PREFS_KEY_API_URL = "pretix_api_url";
    public static final String PREFS_KEY_API_KEY = "pretix_api_key";
    private SharedPreferences prefs;

    public AppConfig(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isConfigured() {
        return prefs.contains(PREFS_KEY_API_URL);
    }

    public void setEventConfig(String url, String key) {
        prefs.edit()
                .putString(PREFS_KEY_API_URL, url)
                .putString(PREFS_KEY_API_KEY, key)
                .apply();
    }

    public void resetEventConfig() {
        prefs.edit()
                .remove(PREFS_KEY_API_URL)
                .remove(PREFS_KEY_API_KEY)
                .apply();
    }
}
