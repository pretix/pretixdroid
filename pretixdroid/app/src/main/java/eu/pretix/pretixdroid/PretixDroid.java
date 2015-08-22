package eu.pretix.pretixdroid;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import eu.pretix.pretixdroid.check.SingleDeviceCheckProvider;
import eu.pretix.pretixdroid.check.TicketCheckProvider;

public class PretixDroid extends Application {
    public static final String PREFS_NAME = "pretixdroid";

    public TicketCheckProvider getCheckProvider(Context ctx) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (settings.getBoolean("multidevice", false)) {
            return null;
        } else {
            return new SingleDeviceCheckProvider(ctx);
        }
    }
}
