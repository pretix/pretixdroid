package eu.pretix.pretixdroid;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import eu.pretix.pretixdroid.check.SingleDeviceCheckProvider;
import eu.pretix.pretixdroid.check.TicketCheckProvider;

public class PretixDroid extends Application {
    public static final String PREFS_NAME = "pretixdroid";
    public static final String SERVICE_NAME = "pretixdroid";
    /*
     * It is not a security problem that the keystore password is hardcoded in plain text.
     * It would be only relevant in a case in which the attack would have either root access on the
     * phone or can execute arbitrary code witht his application's user. In both cases, we're
     * screwed either way.
     */
    public static final String KEYSTORE_PASSWORD = "ZnDNUkQ01PVZyD7oNP3a8DVXrvltxD";

    public TicketCheckProvider getCheckProvider(Context ctx) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (settings.getBoolean("multidevice", false)) {
            return null;
        } else {
            return new SingleDeviceCheckProvider(ctx);
        }
    }
}
