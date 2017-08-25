package eu.pretix.pretixdroid.check;

import android.content.Context;
import android.net.wifi.WifiConfiguration;

import com.joshdholtz.sentry.Sentry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.net.api.ApiException;
import eu.pretix.pretixdroid.net.api.PretixApi;

public class OnlineCheckProvider implements TicketCheckProvider {
    private Context ctx;
    private PretixApi api;
    private AppConfig config;

    public OnlineCheckProvider(Context ctx) {
        this.ctx = ctx;

        this.config = new AppConfig(ctx);
        this.api = PretixApi.fromConfig(config);
    }

    @Override
    public CheckResult check(String ticketid) {
        Sentry.addBreadcrumb("provider.check", "started");
        try {
            CheckResult res = new CheckResult(CheckResult.Type.ERROR);
            JSONObject response = api.redeem(ticketid);
            String status = response.getString("status");
            if ("ok".equals(status)) {
                res.setType(CheckResult.Type.VALID);
            } else {
                String reason = response.optString("reason");
                if ("already_redeemed".equals(reason)) {
                    res.setType(CheckResult.Type.USED);
                } else if ("unknown_ticket".equals(reason)) {
                    res.setType(CheckResult.Type.INVALID);
                } else if ("unpaid".equals(reason)) {
                    res.setType(CheckResult.Type.UNPAID);
                }
            }

            if (response.has("data")) {
                res.setTicket(response.getJSONObject("data").getString("item"));
                res.setVariation(response.getJSONObject("data").getString("variation"));
                res.setAttendee_name(response.getJSONObject("data").getString("attendee_name"));
                res.setOrderCode(response.getJSONObject("data").getString("order"));
                res.setRequireAttention(response.getJSONObject("data").optBoolean("attention", false));
            }
            return res;
        } catch (JSONException e) {
            Sentry.captureException(e);
            CheckResult cr = new CheckResult(CheckResult.Type.ERROR, "Invalid server response");
            if (e.getCause() != null)
                cr.setTicket(e.getCause().getMessage());
            return cr;
        } catch (ApiException e) {
            Sentry.addBreadcrumb("provider.check", "API Error: " + e.getMessage());
            CheckResult cr = new CheckResult(CheckResult.Type.ERROR, e.getMessage());
            if (e.getCause() != null)
                cr.setTicket(e.getCause().getMessage());
            return cr;
        }
    }

    @Override
    public List<SearchResult> search(String query) throws CheckException {
        Sentry.addBreadcrumb("provider.search", "started");
        try {
            JSONObject response = api.search(query);

            List<SearchResult> results = new ArrayList<>();
            for (int i = 0; i < response.getJSONArray("results").length(); i++) {
                JSONObject res = response.getJSONArray("results").getJSONObject(i);
                SearchResult sr = new SearchResult();
                sr.setAttendee_name(res.getString("attendee_name"));
                sr.setTicket(res.getString("item"));
                sr.setVariation(res.getString("variation"));
                sr.setOrderCode(res.getString("order"));
                sr.setSecret(res.getString("secret"));
                sr.setRedeemed(res.getBoolean("redeemed"));
                sr.setPaid(res.getBoolean("paid"));
                sr.setRequireAttention(res.optBoolean("attention", false));
                results.add(sr);
            }
            return results;
        } catch (JSONException e) {
            Sentry.captureException(e);
            throw new CheckException("Unknown server response");
        } catch (ApiException e) {
            Sentry.addBreadcrumb("provider.search", "API Error: " + e.getMessage());
            throw new CheckException(e.getMessage());
        }
    }

    public static StatusResult parseStatusResponse(JSONObject response) throws JSONException {
        List<StatusResultItem> items = new ArrayList<>();

        int itemcount = response.getJSONArray("items").length();
        for (int i = 0; i < itemcount; i++) {
            JSONObject item = response.getJSONArray("items").getJSONObject(i);
            List<StatusResultItemVariation> variations = new ArrayList<>();

            int varcount = item.getJSONArray("variations").length();
            for (int j = 0; j < varcount; j++) {
                JSONObject var = item.getJSONArray("variations").getJSONObject(j);
                variations.add(new StatusResultItemVariation(
                        var.getLong("id"),
                        var.getString("name"),
                        var.getInt("total"),
                        var.getInt("checkins")
                ));
            }

            items.add(new StatusResultItem(
                    item.getLong("id"),
                    item.getString("name"),
                    item.getInt("total"),
                    item.getInt("checkins"),
                    variations,
                    item.getBoolean("admission")
            ));
        }

        return new StatusResult(
                response.getJSONObject("event").getString("name"),
                response.getInt("total"),
                response.getInt("checkins"),
                items
        );
    }

    @Override
    public StatusResult status() throws CheckException {
        Sentry.addBreadcrumb("provider.status", "started");
        try {
            JSONObject response = api.status();
            return parseStatusResponse(response);
        } catch (JSONException e) {
            Sentry.captureException(e);
            throw new CheckException("Unknown server response");
        } catch (ApiException e) {
            Sentry.addBreadcrumb("provider.search", "API Error: " + e.getMessage());
            throw new CheckException(e.getMessage());
        }
    }
}
