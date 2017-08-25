package eu.pretix.pretixdroid;

import android.app.Application;

import com.facebook.stetho.Stetho;


import eu.pretix.pretixdroid.check.AsyncCheckProvider;
import eu.pretix.pretixdroid.check.OnlineCheckProvider;
import eu.pretix.pretixdroid.check.TicketCheckProvider;
import eu.pretix.pretixdroid.db.Models;
import io.requery.BlockingEntityStore;
import io.requery.Persistable;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.sql.Configuration;
import io.requery.sql.EntityDataStore;

public class PretixDroid extends Application {
    /*
     * It is not a security problem that the keystore password is hardcoded in plain text.
     * It would be only relevant in a case in which the attack would have either root access on the
     * phone or can execute arbitrary code with this application's user. In both cases, we're
     * screwed either way.
     */
    public static final String KEYSTORE_PASSWORD = "ZnDNUkQ01PVZyD7oNP3a8DVXrvltxD";
    private BlockingEntityStore<Persistable> dataStore;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this);
        }
    }

    public BlockingEntityStore<Persistable> getData() {
        if (dataStore == null) {
            // override onUpgrade to handle migrating to a new version
            DatabaseSource source = new DatabaseSource(this, Models.DEFAULT, 2);
            Configuration configuration = source.getConfiguration();
            dataStore = new EntityDataStore<Persistable>(configuration);
        }
        return dataStore;
    }

    public TicketCheckProvider getNewCheckProvider() {
        AppConfig config = new AppConfig(this);
        if (config.getAsyncModeEnabled()) {
            return new AsyncCheckProvider(this);
        } else {
            return new OnlineCheckProvider(this);
        }
    }
}
