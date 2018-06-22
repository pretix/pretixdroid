package eu.pretix.pretixdroid;

import android.app.Application;

import com.facebook.stetho.Stetho;

import net.sqlcipher.database.SQLiteException;

import eu.pretix.libpretixsync.check.AsyncCheckProvider;
import eu.pretix.libpretixsync.check.OnlineCheckProvider;
import eu.pretix.libpretixsync.check.TicketCheckProvider;
import eu.pretix.libpretixsync.db.Models;
import io.requery.BlockingEntityStore;
import io.requery.Persistable;
import io.requery.android.sqlcipher.SqlCipherDatabaseSource;
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
            String dbPass = KeystoreHelper.secureValue(KEYSTORE_PASSWORD, true);

            SqlCipherDatabaseSource source = new SqlCipherDatabaseSource(this,
                    Models.DEFAULT, Models.DEFAULT.getName(), dbPass, 5);

            try {
                // check if database has been decrypted
                source.getReadableDatabase().execSQL("select count(*) from sqlite_master;"); //source.getReadableDatabase().getSyncedTables() ???
            } catch(SQLiteException e) {
                // if not, delete it
                this.deleteDatabase(Models.DEFAULT.getName());
                // and create a new one
                source = new SqlCipherDatabaseSource(this,
                        Models.DEFAULT, Models.DEFAULT.getName(), dbPass, 5);
            }

            Configuration configuration = source.getConfiguration();
            dataStore = new EntityDataStore<>(configuration);
        }
        return dataStore;
    }

    public TicketCheckProvider getNewCheckProvider() {
        AppConfig config = new AppConfig(this);
        TicketCheckProvider p;
        if (config.getAsyncModeEnabled()) {
            p = new AsyncCheckProvider(config, getData());
        } else {
            p = new OnlineCheckProvider(config, new AndroidHttpClientFactory());
        }
        p.setSentry(new AndroidSentryImplementation());
        return p;
    }
}
