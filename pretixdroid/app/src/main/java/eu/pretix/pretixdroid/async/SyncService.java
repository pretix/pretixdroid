package eu.pretix.pretixdroid.async;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.joshdholtz.sentry.Sentry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.db.QueuedCheckIn;
import eu.pretix.pretixdroid.db.Ticket;
import eu.pretix.pretixdroid.net.api.ApiException;
import eu.pretix.pretixdroid.net.api.PretixApi;
import io.requery.BlockingEntityStore;
import io.requery.Persistable;
import io.requery.util.CloseableIterator;

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
        api = PretixApi.fromConfig(config);

        if (!config.isConfigured()) {
            return;
        }

        long upload_interval = 1000;
        long download_interval = 30000;
        if (!config.getAsyncModeEnabled()) {
            download_interval = 120000;
        }

        if ((System.currentTimeMillis() - config.getLastSync()) < upload_interval) {
            return;
        }
        if ((System.currentTimeMillis() - config.getLastFailedSync()) < 30000) {
            return;
        }

        try {
            uploadTicketData();

            if ((System.currentTimeMillis() - config.getLastDownload()) > download_interval) {
                downloadTicketData();
                config.setLastDownload(System.currentTimeMillis());
            }

            config.setLastSync(System.currentTimeMillis());
            config.setLastFailedSync(0);
        } catch (SyncException e) {
            config.setLastFailedSync(System.currentTimeMillis());
            config.setLastFailedSyncMsg(e.getMessage());
        }
    }

    private void uploadTicketData() throws SyncException {
        Sentry.addBreadcrumb("sync.queue", "Start upload");

        List<QueuedCheckIn> queued = dataStore.select(QueuedCheckIn.class).get().toList();

        try {
            for (QueuedCheckIn qci : queued) {
                JSONObject response = api.redeem(qci.getSecret(), qci.getDatetime(), true, qci.getNonce());
                String status = response.getString("status");
                if ("ok".equals(status)) {
                    dataStore.delete(qci);
                } else {
                    String reason = response.optString("reason");
                    if ("already_redeemed".equals(reason)) {
                        // Well, we can't really do something about this.
                        dataStore.delete(qci);
                    } // Else: Retry later
                }
            }
        } catch (JSONException e) {
            Sentry.captureException(e);
            throw new SyncException("Unknown server response");
        } catch (ApiException e) {
            Sentry.addBreadcrumb("sync.tickets", "API Error: " + e.getMessage());
            throw new SyncException(e.getMessage());
        }
        Sentry.addBreadcrumb("sync.queue", "Upload complete");
    }

    private static boolean long_changed(Long newint, Long oldint) {
        return (newint != null && oldint == null)
                || (newint == null && oldint != null)
                || (newint != null && oldint != null && !newint.equals(oldint));
    }

    private static boolean string_changed(String newstring, String oldstring) {
        return (newstring != null && oldstring == null)
                || (newstring == null && oldstring != null)
                || (newstring != null && oldstring != null && !newstring.equals(oldstring));
    }

    private void downloadTicketData() throws SyncException {
        Sentry.addBreadcrumb("sync.tickets", "Start download");

        // Download objects from server
        JSONObject response;
        try {
            response = api.download();
        } catch (ApiException e) {
            Sentry.addBreadcrumb("sync.tickets", "API Error: " + e.getMessage());
            throw new SyncException(e.getMessage());
        }

        // Index all known objects
        Map<String, Ticket> known = new HashMap<>();
        CloseableIterator<Ticket> tickets = dataStore.select(Ticket.class).get().iterator();
        try {
            while (tickets.hasNext()) {
                Ticket t = tickets.next();
                known.put(t.getSecret(), t);
            }
        } finally {
            tickets.close();
        }

        try {
            List<Ticket> inserts = new ArrayList<>();
            // Insert or update
            for (int i = 0; i < response.getJSONArray("results").length(); i++) {
                JSONObject res = response.getJSONArray("results").getJSONObject(i);

                Ticket ticket;
                boolean created = false;
                if (!known.containsKey(res.getString("secret"))) {
                    ticket = new Ticket();
                    created = true;
                } else {
                    ticket = known.get(res.getString("secret"));
                }

                if (string_changed(res.getString("attendee_name"), ticket.getAttendee_name())) {
                    ticket.setAttendee_name(res.getString("attendee_name"));
                }
                if (string_changed(res.getString("item"), ticket.getItem())) {
                    ticket.setItem(res.getString("item"));
                }
                if (long_changed(res.optLong("item_id"), ticket.getItem_id())) {
                    ticket.setItem_id(res.optLong("item_id"));
                }
                if (string_changed(res.getString("variation"), ticket.getVariation())) {
                    ticket.setVariation(res.getString("variation"));
                }
                if (long_changed(res.optLong("variation_id"), ticket.getVariation_id())) {
                    ticket.setVariation_id((long) res.optLong("variation_id"));
                }
                if (string_changed(res.getString("order"), ticket.getOrder())) {
                    ticket.setOrder(res.getString("order"));
                }
                if (string_changed(res.getString("secret"), ticket.getSecret())) {
                    ticket.setSecret(res.getString("secret"));
                }
                if (res.getBoolean("redeemed") != ticket.isRedeemed()) {
                    ticket.setRedeemed(res.getBoolean("redeemed"));
                }
                if (res.getBoolean("paid") != ticket.isPaid()) {
                    ticket.setPaid(res.getBoolean("paid"));
                }

                if (created) {
                    inserts.add(ticket);
                } else {
                    dataStore.update(ticket);
                }
                known.remove(res.getString("secret"));
            }

            dataStore.insert(inserts);
        } catch (JSONException e) {
            Sentry.captureException(e);
            throw new SyncException("Unknown server response");
        }

        // Those have been deleted online, delete them here as well
        for (String key : known.keySet()) {
            dataStore.delete(known.get(key));
        }
        Sentry.addBreadcrumb("sync.tickets", "Download complete");
    }

}
