package eu.pretix.pretixdroid.async;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import eu.pretix.libpretixsync.api.PretixApi;
import eu.pretix.libpretixsync.sync.SyncManager;
import eu.pretix.pretixdroid.AndroidHttpClientFactory;
import eu.pretix.pretixdroid.AndroidSentryImplementation;
import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.PretixDroid;
import io.requery.BlockingEntityStore;
import io.requery.Persistable;

public class SyncService extends IntentService {

    private AppConfig config;
    private PretixApi api;
    private BlockingEntityStore<Persistable> dataStore;

    public SyncService() {
        super("SyncService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        config = new AppConfig(this);
        dataStore = ((PretixDroid) getApplicationContext()).getData();
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.i("SyncService", "Sync triggered");

        // Rebuild in case config has changed
        api = PretixApi.fromConfig(config, new AndroidHttpClientFactory());

        long upload_interval = 1000;
        long download_interval = 30000;
        if (!config.getAsyncModeEnabled()) {
            download_interval = 120000;
        }

        SyncManager sm = new SyncManager(
                config,
                api,
                new AndroidSentryImplementation(),
                dataStore,
                upload_interval,
                download_interval
        );
        sm.sync();
    }

}
